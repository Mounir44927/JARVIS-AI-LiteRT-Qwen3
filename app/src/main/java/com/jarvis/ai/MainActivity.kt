package com.jarvis.ai

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import com.jarvis.ai.ui.JarvisRoot
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.voice.VoiceForegroundService
import com.jarvis.ai.voice.WakeWordEventStore

class MainActivity : ComponentActivity() {
    private val vm by viewModels<JarvisViewModel> {
        JarvisViewModel.factory(application as JarvisApp)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.onMicrophonePermissionResult(granted)
        }

    private val wakeWordEvents by lazy { WakeWordEventStore(applicationContext) }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == VoiceForegroundService.ACTION_WAKE_WORD_DETECTED) {
                // Consume the durable event immediately when the Activity is already started.
                // If the Activity is stopped there is no receiver, so onStart() consumes it instead.
                if (wakeWordEvents.consumePending() != null) vm.startListening()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.uiState.collectAsState()
            JarvisRoot(
                state = state,
                viewModel = vm,
                requestMicPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            wakeWordReceiver,
            IntentFilter(VoiceForegroundService.ACTION_WAKE_WORD_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (wakeWordEvents.consumePending() != null) {
            vm.startListening()
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(wakeWordReceiver) }
        super.onStop()
    }

}
