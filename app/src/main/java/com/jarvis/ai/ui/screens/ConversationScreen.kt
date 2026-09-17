package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun ConversationScreen(state: UiState, vm: JarvisViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.navigate(Screen.Home) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("المحادثة", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(Modifier.weight(1f), reverseLayout = false) {
            items(state.messages, key = { it.id }) { message ->
                Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), tonalElevation = 1.dp) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (message.role == "user") "أنت" else "JARVIS", style = MaterialTheme.typography.labelMedium)
                        Text(message.content)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(state.input, vm::onInputChange, Modifier.weight(1f), placeholder = { Text("اكتب سؤالك…") }, singleLine = true)
            IconButton(vm::startListening) { Icon(Icons.Default.Mic, "استماع") }
            IconButton(vm::submitText) { Text("إرسال") }
        }
    }
}
