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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jarvis.ai.BuildConfig
import com.jarvis.ai.brain.BackendEndpointValidator
import com.jarvis.ai.ui.JarvisViewModel
import com.jarvis.ai.ui.Screen
import com.jarvis.ai.ui.UiState

@Composable
fun ConnectionScreen(state: UiState, vm: JarvisViewModel) {
    var apiKey by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(state.geminiApiConfigured) {
        if (!state.geminiApiConfigured) apiKey = ""
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.navigate(Screen.Settings) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("Gemini والاتصال", style = MaterialTheme.typography.headlineSmall)
        }

        Text("النموذج: Gemini 2.5 Flash (${BuildConfig.DEFAULT_GEMINI_MODEL})")
        Text(
            if (state.geminiApiConfigured) "Gemini API: مفعّل على هذا الجهاز"
            else "Gemini API: غير مفعّل داخل التطبيق"
        )
        state.geminiStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gemini API Key") },
            placeholder = { Text(if (state.geminiApiConfigured) "أدخل مفتاحًا جديدًا لتغييره" else "ألصق مفتاح Gemini هنا") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = editing || !state.geminiApiConfigured
        )

        if (editing || !state.geminiApiConfigured) {
            Button(
                onClick = {
                    vm.saveGeminiApiKey(apiKey)
                    editing = false
                },
                enabled = apiKey.trim().isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.geminiApiConfigured) "تفعيل المفتاح الجديد" else "تفعيل Gemini") }
        } else {
            Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                Text("تغيير مفتاح Gemini")
            }
            TextButton(onClick = vm::clearGeminiApiKey, modifier = Modifier.fillMaxWidth()) {
                Text("إزالة المفتاح")
            }
        }

        Text(
            "المفتاح يُخزّن محليًا مشفّرًا باستخدام Android Keystore، ولا يظهر في النص بعد الحفظ. " +
                "لن تحتاج إلى إعادة إدخاله عند كل تشغيل. يمكنك تغييره لاحقًا من هذه الصفحة.",
            style = MaterialTheme.typography.bodySmall
        )

        val backendConfigured = BackendEndpointValidator.isValidBaseUrl(BuildConfig.BACKEND_BASE_URL)
        Text(
            if (backendConfigured) "Backend: متاح كمسار احتياطي عندما لا يكون Gemini API مفعّلًا."
            else "Backend: غير مُعد."
        )
    }
}
