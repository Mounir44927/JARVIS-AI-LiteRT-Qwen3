package com.jarvis.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SourceChip(title: String, modifier: Modifier = Modifier) {
    AssistChip(onClick = {}, label = { Text(title.ifBlank { "مصدر" }) }, modifier = modifier)
}

@Composable
fun MemoryBadge(active: Boolean) {
    FilterChip(selected = active, onClick = {}, label = { Text(if (active) "من الذاكرة" else "ذاكرة") })
}

@Composable
fun ConnectionIndicator(online: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(if (online) "متصل" else "دون اتصال")
    }
}
