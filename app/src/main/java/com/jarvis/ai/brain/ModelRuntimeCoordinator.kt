package com.jarvis.ai.brain

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

/**
 * Pure memory policy; kept separate so it can be unit-tested without Android framework classes.
 */
object ModelRuntimePolicy {
    fun shouldKeepHeavyModelsResident(
        totalMemBytes: Long,
        availMemBytes: Long,
        lowMemory: Boolean
    ): Boolean =
        totalMemBytes >= ModelRuntimeCoordinator.HIGH_RAM_DEVICE_BYTES &&
            availMemBytes >= ModelRuntimeCoordinator.RESIDENT_AVAIL_MEMORY_BYTES &&
            !lowMemory
}

/**
 * Coordinates memory-heavy local models without forcing unload/reload on healthy devices.
 *
 * Policy:
 * - A device with >= 6 GiB total RAM, enough currently available RAM, and no Android
 *   low-memory signal keeps LiteRT-LM and Whisper resident together.
 * - On constrained devices, Whisper may temporarily release LiteRT-LM before inference.
 * - All model inference is serialized through one Mutex; resident models never run concurrently.
 *
 * The coordinator deliberately does not decide which AI backend should answer a request.
 * Routing remains LocalFirstRoutingGateway's responsibility.
 */
class ModelRuntimeCoordinator(context: Context) {

    private val appContext = context.applicationContext
    private val activityManager =
        requireNotNull(appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)

    private val inferenceMutex = Mutex()
    private var lastSnapshot = snapshot()

    @Volatile
    private var lastReloadMillis: Long = 0L

    @Volatile
    private var lastLocalResponseMillis: Long = 0L

    /**
     * Serializes LiteRT inference with any future Whisper inference.
     * No unload/reload is performed here because this path is already running under the
     * local inference lock.
     */
    suspend fun <T> withLiteRt(block: suspend () -> T): T =
        inferenceMutex.withLock {
            val start = System.nanoTime()
            try {
                block()
            } finally {
                lastLocalResponseMillis = elapsedMillis(start)
                lastSnapshot = snapshot()
            }
        }

    /**
     * Serializes Whisper inference. On constrained devices only, it asks the LiteRT owner to
     * release itself before Whisper starts, then measures and records the reload after Whisper.
     *
     * On healthy devices no release/reload occurs: both models may stay resident and only the
     * Mutex prevents simultaneous native inference.
     */
    suspend fun <T> withWhisper(
        releaseLiteRtForPressure: suspend () -> Unit,
        reloadLiteRtAfterPressure: suspend () -> Unit,
        block: suspend () -> T
    ): T =
        inferenceMutex.withLock {
            lastSnapshot = snapshot()
            val shouldRelease = shouldReleaseLiteRtForWhisper(lastSnapshot)

            if (!shouldRelease) {
                return@withLock try {
                    block()
                } finally {
                    lastSnapshot = snapshot()
                }
            }

            releaseLiteRtForPressure()
            lastSnapshot = snapshot()

            try {
                block()
            } finally {
                val reloadStart = System.nanoTime()
                runCatching { reloadLiteRtAfterPressure() }
                    .onFailure {
                        Log.w(TAG, "LiteRT-LM reload after Whisper failed", it)
                    }
                lastReloadMillis = elapsedMillis(reloadStart)
                lastSnapshot = snapshot()
            }
        }

    /**
     * Returns the current memory picture directly from ActivityManager.MemoryInfo.
     * This is intentionally evaluated immediately before resource arbitration.
     */
    fun snapshot(): MemorySnapshot {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val runtimeUsed = runCatching { Debug.getNativeHeapAllocatedSize() }.getOrDefault(0L)
        return MemorySnapshot(
            totalMemBytes = info.totalMem,
            availMemBytes = info.availMem,
            lowMemory = info.lowMemory,
            thresholdBytes = info.threshold,
            nativeHeapAllocatedBytes = runtimeUsed
        )
    }

    fun shouldKeepHeavyModelsResident(memory: MemorySnapshot = snapshot()): Boolean =
        ModelRuntimePolicy.shouldKeepHeavyModelsResident(
            totalMemBytes = memory.totalMemBytes,
            availMemBytes = memory.availMemBytes,
            lowMemory = memory.lowMemory
        )

    fun shouldReleaseLiteRtForWhisper(memory: MemorySnapshot = snapshot()): Boolean =
        !shouldKeepHeavyModelsResident(memory)

    fun metrics(): ModelRuntimeMetrics =
        ModelRuntimeMetrics(
            lastReloadMillis = lastReloadMillis,
            lastLocalResponseMillis = lastLocalResponseMillis,
            memory = lastSnapshot
        )

    private fun elapsedMillis(startNanos: Long): Long =
        max(0L, (System.nanoTime() - startNanos) / 1_000_000L)

    data class MemorySnapshot(
        val totalMemBytes: Long,
        val availMemBytes: Long,
        val lowMemory: Boolean,
        val thresholdBytes: Long,
        val nativeHeapAllocatedBytes: Long
    ) {
        val totalMemGiB: Double get() = totalMemBytes.toDouble() / BYTES_PER_GIB
        val availMemGiB: Double get() = availMemBytes.toDouble() / BYTES_PER_GIB
    }

    data class ModelRuntimeMetrics(
        val lastReloadMillis: Long,
        val lastLocalResponseMillis: Long,
        val memory: MemorySnapshot
    )

    companion object {
        private const val TAG = "JarvisRuntime"
        private const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0

        // User-approved starting policy: devices above ~6 GiB may keep both heavy models resident.
        const val HIGH_RAM_DEVICE_BYTES = 6L * 1024L * 1024L * 1024L

        // Require real headroom, not merely a large total RAM figure.
        const val RESIDENT_AVAIL_MEMORY_BYTES = 1536L * 1024L * 1024L
    }
}
