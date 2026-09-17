package com.jarvis.ai.voice

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Provisions a multilingual whisper.cpp model into private app storage.
 *
 * Default: ggml-small-q5_1.bin (~190 MiB), suitable for Arabic and other multilingual input.
 * The model is never bundled in the APK. Downloads are resumable and verified atomically.
 */
class WhisperModelStore(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val provisioningMutex = Mutex()

    suspend fun getSmallModelFile(): File =
        provisioningMutex.withLock {
            withContext(Dispatchers.IO) { getModelFile(SMALL) }
        }

    suspend fun getMediumModelFile(): File =
        provisioningMutex.withLock {
            withContext(Dispatchers.IO) { getModelFile(MEDIUM) }
        }

    private fun getModelFile(spec: ModelSpec): File {
        val dir = File(context.filesDir, MODEL_DIR_NAME).apply { mkdirs() }
        val model = File(dir, spec.fileName)
        if (isValid(model, spec)) return model

        if (model.exists()) model.delete()

        val partial = File(dir, "${spec.fileName}.part")
        downloadResumable(partial, spec)

        if (!isValid(partial, spec)) {
            partial.delete()
            throw IOException("Whisper model failed SHA-256 verification: ${spec.fileName}")
        }

        if (!partial.renameTo(model)) {
            model.delete()
            if (!partial.renameTo(model)) {
                throw IOException("Could not atomically finalize Whisper model: ${spec.fileName}")
            }
        }
        return model
    }

    private fun isValid(file: File, spec: ModelSpec): Boolean {
        if (!file.isFile || file.length() != spec.sizeBytes) return false
        return sha256(file) == spec.sha256
    }

    private fun downloadResumable(partial: File, spec: ModelSpec) {
        val existing = partial.takeIf { it.isFile }?.length()?.coerceAtLeast(0L) ?: 0L

        val available = StatFs(context.filesDir.path).availableBytes
        val required = spec.sizeBytes + 64L * 1024L * 1024L
        if (available < required) {
            throw IOException("Not enough free storage for Whisper model: ${spec.fileName}")
        }

        val requestBuilder = Request.Builder().url(spec.url)
        if (existing in 1 until spec.sizeBytes) {
            requestBuilder.header("Range", "bytes=$existing-")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Whisper model download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Whisper model download returned no body")

            // If the server ignores Range and returns 200, restart from zero instead of appending
            // the full payload to a partial file.
            val append = existing in 1 until spec.sizeBytes && response.code == 206

            body.byteStream().use { input ->
                FileOutputStream(partial, append).use { output ->
                    input.copyTo(output)
                }
            }
        }

        if (partial.length() != spec.sizeBytes) {
            throw IOException(
                "Whisper model size mismatch: expected=${spec.sizeBytes}, actual=${partial.length()}"
            )
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    data class ModelSpec(
        val fileName: String,
        val url: String,
        val sizeBytes: Long,
        val sha256: String
    )

    companion object {
        private const val MODEL_DIR_NAME = "whisper"

        val SMALL = ModelSpec(
            fileName = "ggml-small-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin?download=true",
            sizeBytes = 190_085_487L,
            sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb"
        )

        val MEDIUM = ModelSpec(
            fileName = "ggml-medium-q5_0.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium-q5_0.bin?download=true",
            sizeBytes = 539_212_467L,
            sha256 = "19fea4b380c3a618ec4723c3eef2eb785ffba0d0538cf43f8f235e7b3b34220f"
        )
    }
}
