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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun MemoryScreen(state: UiState, vm: JarvisViewModel) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.navigate(Screen.Home) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("الذاكرة", style = MaterialTheme.typography.headlineSmall)
            TextButton({ confirm = true }) { Text("حذف الكل") }
        }
        Text("حقائقك المحفوظة محليًا", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(state.facts, key = { it.id }) { fact ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(fact.category); Text(fact.content); Text("المصدر: ${fact.source}", style = MaterialTheme.typography.labelSmall) }
                    TextButton({ vm.deleteFact(fact.id) }) { Text("حذف") }
                }
            }
        }
        if (state.facts.isEmpty()) Text("لا توجد ذاكرة محفوظة بعد.")
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("حذف الذاكرة") }, text = { Text("سيُحذف سجل الذاكرة المحلي والرسائل المحفوظة.") }, confirmButton = { Button({ vm.clearMemory(); confirm = false }) { Text("حذف") } }, dismissButton = { TextButton({ confirm = false }) { Text("إلغاء") } })
}
