package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun SettingsScreen(state: UiState, vm: JarvisViewModel, requestMicPermission: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.navigate(Screen.Home) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("الإعدادات", style = MaterialTheme.typography.headlineSmall)
        }
        Button({ vm.navigate(Screen.Memory) }, Modifier.fillMaxWidth()) { Text("إدارة الذاكرة") }
        Button({ vm.navigate(Screen.Connection) }, Modifier.fillMaxWidth()) { Text("حالة الاتصال والنموذج") }
        Button({ vm.navigate(Screen.Permissions) }, Modifier.fillMaxWidth()) { Text("الأذونات والصوت") }
        Button({ vm.navigate(Screen.About) }, Modifier.fillMaxWidth()) { Text("حول وخصوصية") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("الاستماع المستمر")
                Text(
                    if (state.alwaysListeningEnabled) "نشط: كاشف ${com.jarvis.ai.voice.OpenWakeWordController.SUPPORTED_WAKE_PHRASE} يعمل محليًا"
                    else "متوقف: لا يبدأ الميكروفون تلقائيًا",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = state.alwaysListeningEnabled,
                onCheckedChange = { enabled -> vm.setAlwaysListeningEnabled(enabled, requestMicPermission) }
            )
        }
        TextButton(requestMicPermission, Modifier.fillMaxWidth()) { Text("طلب إذن الميكروفون") }
        Text("لا يبدأ الاستماع المستمر بمجرد منح الإذن؛ يجب تفعيله صراحة من هذا الخيار.", style = MaterialTheme.typography.bodySmall)
    }
}
