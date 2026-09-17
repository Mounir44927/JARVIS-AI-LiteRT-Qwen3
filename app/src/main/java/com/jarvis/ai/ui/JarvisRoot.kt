package com.jarvis.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.jarvis.ai.ui.screens.*
import com.jarvis.ai.ui.theme.JarvisTheme

@Composable
fun JarvisRoot(state: UiState, viewModel: JarvisViewModel, requestMicPermission: () -> Unit) {
    JarvisTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                when (state.screen) {
                    Screen.Home -> HomeScreen(state, viewModel)
                    Screen.Conversation -> ConversationScreen(state, viewModel)
                    Screen.Memory -> MemoryScreen(state, viewModel)
                    Screen.Profile -> ProfileScreen(state, viewModel)
                    Screen.Settings -> SettingsScreen(state, viewModel, requestMicPermission)
                    Screen.Connection -> ConnectionScreen(state, viewModel)
                    Screen.Permissions -> PermissionsScreen(state, viewModel, requestMicPermission)
                    Screen.About -> AboutScreen(state, viewModel)
                }
            }
        }
    }
}
