package com.jarvis.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFFFC94D),
    tertiary = Color(0xFF66F6FF),
    background = Color(0xFF05070F),
    surface = Color(0xFF0A0E1B),
    onBackground = Color(0xFFE8EAF0),
    onSurface = Color(0xFFE8EAF0)
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JarvisScheme, content = content)
}
