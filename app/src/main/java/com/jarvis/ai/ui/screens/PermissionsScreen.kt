package com.jarvis.ai.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.Manifest
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun PermissionsScreen(state: UiState, vm: JarvisViewModel, requestMicPermission: () -> Unit) {
    val context = LocalContext.current
    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        IconButton({ vm.navigate(Screen.Settings) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
        Text("الأذونات والصوت", style = MaterialTheme.typography.headlineSmall)
        Text(if (mic) "الميكروفون: مسموح" else "الميكروفون: غير مسموح")
        Text(if (state.ttsReady) "صوت JARVIS الإنجليزي: متاح" else "صوت JARVIS الإنجليزي: غير مؤكد أو غير متاح")
        Text("رفض الميكروفون لا يمنع استخدام المحادثة النصية.")
        Button(requestMicPermission) { Text("منح إذن الميكروفون") }
    }
}
