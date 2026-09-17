package com.jarvis.ai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
fun AboutScreen(state: UiState, vm: JarvisViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        IconButton({ vm.navigate(Screen.Settings) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
        Text("JARVIS", style = MaterialTheme.typography.displaySmall)
        Text("الإصدار 2.0.0")
        Text("الذاكرة الشخصية محلية افتراضيًا. لا يُحفظ الصوت الخام افتراضيًا. عند استخدام مزود سحابي، يُرسل الحد الأدنى من السياق اللازم.")
        Text("الحقوق: MIT")
    }
}
