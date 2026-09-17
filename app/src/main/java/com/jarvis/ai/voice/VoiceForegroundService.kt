package com.jarvis.ai.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.ai.R
import com.jarvis.ai.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var settings: SettingsStore
    private lateinit var wakeEvents: WakeWordEventStore
    private var wakeWord: OpenWakeWordController? = null

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(applicationContext)
        wakeEvents = WakeWordEventStore(applicationContext)
        createChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_jarvis)
            .setContentTitle("JARVIS")
            .setContentText("استماع محلي لكلمة ${OpenWakeWordController.SUPPORTED_WAKE_PHRASE}")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        runCatching { startForeground(NOTIFICATION_ID, notification) }
            .onFailure { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SUSPEND -> wakeWord?.disable()
            ACTION_RESUME -> resumeIfAllowed()
            ACTION_STOP -> {
                stopWakeWord()
                stopSelf()
            }
            else -> resumeIfAllowed()
        }
        return START_STICKY
    }

    private fun resumeIfAllowed() {
        if (!hasRecordAudioPermission()) return
        serviceScope.launch {
            val enabled = settings.alwaysListeningEnabled.first()
            withContext(Dispatchers.Main.immediate) {
                if (enabled) {
                    ensureWakeWord()?.enable()
                } else {
                    // A process restart must never silently re-enable microphone listening.
                    stopWakeWord()
                }
            }
        }
    }

    private fun ensureWakeWord(): OpenWakeWordController? {
        if (wakeWord == null) {
            wakeWord = OpenWakeWordController(
                context = applicationContext,
                scope = serviceScope,
                onDetected = { score ->
                    wakeWord?.disable()
                    wakeEvents.markPending(score)
                    sendBroadcast(
                        Intent(ACTION_WAKE_WORD_DETECTED)
                            .setPackage(packageName)
                            .putExtra(EXTRA_SCORE, score)
                    )
                }
            )
        }
        return wakeWord
    }

    private fun stopWakeWord() {
        wakeWord?.disable()
    }

    override fun onDestroy() {
        wakeWord?.release()
        wakeWord = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasRecordAudioPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS الصوت",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "الاستماع المحلي لكلمة ${OpenWakeWordController.SUPPORTED_WAKE_PHRASE}"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_WAKE_WORD_DETECTED = "com.jarvis.ai.WAKE_WORD_DETECTED"
        const val ACTION_SUSPEND = "com.jarvis.ai.WAKE_WORD_SUSPEND"
        const val ACTION_RESUME = "com.jarvis.ai.WAKE_WORD_RESUME"
        const val ACTION_STOP = "com.jarvis.ai.WAKE_WORD_STOP"
        const val EXTRA_SCORE = "score"
        private const val CHANNEL_ID = "jarvis_voice"
        private const val NOTIFICATION_ID = 2001
    }
}
