package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState
import com.jarvis.ai.ui.components.JarvisRiveOrb
import com.jarvis.ai.ui.components.StateBadge

@Composable
fun HomeScreen(state: UiState, vm: JarvisViewModel) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    state.profile?.name?.let { "مرحبًا $it" } ?: "مرحبًا",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("JARVIS جاهز", style = MaterialTheme.typography.bodyMedium)
            }
            StateBadge(state.assistantState)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JarvisRiveOrb(state.assistantState, state.voiceLevel)
            Text("بماذا أساعدك؟", style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(state.input, vm::onInputChange, Modifier.weight(1f), placeholder = { Text("اكتب سؤالك…") }, singleLine = true)
            Button(onClick = { vm.submitText(); vm.navigate(Screen.Conversation) }) { Text("إرسال") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = vm::startListening) { Icon(Icons.Default.Mic, contentDescription = "استماع") }
            IconButton(onClick = { vm.navigate(Screen.Memory) }) { Icon(Icons.Default.Memory, contentDescription = "الذاكرة") }
            IconButton(onClick = { vm.navigate(Screen.Profile) }) { Icon(Icons.Default.Person, contentDescription = "الملف") }
            IconButton(onClick = { vm.navigate(Screen.Settings) }) { Icon(Icons.Filled.Settings, contentDescription = "الإعدادات") }
        }
    }
}
