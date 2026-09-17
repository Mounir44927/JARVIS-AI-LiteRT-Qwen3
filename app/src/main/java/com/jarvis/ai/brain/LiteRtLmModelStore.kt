package com.jarvis.ai.brain

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Lazily provisions the official LiteRT Community Qwen3-1.7B INT4 artifact.
 *
 * We intentionally keep the ~977 MB model out of the APK. It is downloaded once into the app's
 * internal files directory and verified by SHA-256 before it is exposed to LiteRT-LM.
 */
class LiteRtLmModelStore(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun getModelFile(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, MODEL_DIR_NAME).apply { mkdirs() }
        val model = File(dir, MODEL_FILE_NAME)
        if (isValid(model)) return@withContext model

        if (model.exists()) model.delete()
        val partial = File(dir, "$MODEL_FILE_NAME.part")
        download(partial)
        if (!isValid(partial)) {
            partial.delete()
            throw IOException("Downloaded LiteRT-LM model failed SHA-256 verification")
        }
        if (!partial.renameTo(model)) {
            model.delete()
            if (!partial.renameTo(model)) {
                throw IOException("Could not finalize LiteRT-LM model file")
            }
        }
        model
    }

    private fun isValid(file: File): Boolean {
        if (!file.isFile || file.length() != MODEL_SIZE_BYTES) return false
        return sha256(file) == MODEL_SHA256
    }

    private fun download(partial: File) {
        val existing = partial.length().coerceAtLeast(0L)
        val available = StatFs(context.filesDir.path).availableBytes
        val required = MODEL_SIZE_BYTES + 256L * 1024L * 1024L
        if (available < required) {
            throw IOException("Not enough free storage for the local model")
        }

        val requestBuilder = Request.Builder().url(MODEL_URL)
        if (existing > 0L && existing < MODEL_SIZE_BYTES) {
            requestBuilder.header("Range", "bytes=$existing-")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("LiteRT-LM model download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("LiteRT-LM model download returned no body")
            val append = existing > 0L && response.code == 206
            body.byteStream().use { input ->
                FileOutputStream(partial, append).use { target ->
                    input.copyTo(target)
                }
            }
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

    companion object {
        const val MODEL_FILE_NAME = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm"
        const val MODEL_SHA256 = "2eeffef7b51bc3e1225ea69fe7aa5f417397934b56a5b6c20cc068d6fd2c918b"
        const val MODEL_SIZE_BYTES = 977_184_032L
        const val MODEL_URL =
            "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm?download=true"
        private const val MODEL_DIR_NAME = "litertlm"
    }
}
