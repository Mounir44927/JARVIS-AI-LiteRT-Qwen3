package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun ProfileScreen(state: UiState, vm: JarvisViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.navigate(Screen.Home) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("الملف الشخصي", style = MaterialTheme.typography.headlineSmall)
        }
        val profile = state.profile
        if (profile == null) {
            Text("لم يتم حفظ اسمك بعد", style = MaterialTheme.typography.titleLarge)
            Text("قل لـ JARVIS: اسمي أحمد", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(profile.name, style = MaterialTheme.typography.displaySmall)
            Text("طريقة المخاطبة: ${profile.title}")
        }
        Text("كلمة الإيقاظ: ${com.jarvis.ai.voice.OpenWakeWordController.SUPPORTED_WAKE_PHRASE}")
    }
}
