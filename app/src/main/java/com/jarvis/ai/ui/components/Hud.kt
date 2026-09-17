package com.jarvis.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarvis.ai.core.AssistantState

@Composable
fun HudOrb(state: AssistantState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jarvis-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (state is AssistantState.ListeningForCommand) 1.18f else 1.05f,
        animationSpec = infiniteRepeatable(tween(if (state is AssistantState.Thinking) 450 else 1400), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.08f), radius = size.minDimension * 0.46f * pulse)
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.22f), radius = size.minDimension * 0.30f, style = Stroke(3.dp.toPx()))
            drawCircle(Color(0xFFFFC94D).copy(alpha = 0.55f), radius = size.minDimension * 0.16f)
            drawCircle(Color(0xFF00E5FF), radius = size.minDimension * 0.11f)
        }
    }
}

@Composable
fun StateBadge(state: AssistantState) {
    val text = when (state) {
        AssistantState.Idle -> "جاهز"
        AssistantState.ListeningForWakeWord -> "أستمع لكلمة الإيقاظ"
        AssistantState.ListeningForCommand -> "أستمع"
        AssistantState.Thinking -> "أحلل"
        AssistantState.Speaking -> "أتحدث"
        is AssistantState.Error -> "خطأ"
    }
    AssistChip(onClick = {}, label = { Text(text) })
}

@Composable
fun WaveIndicator(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "waves")
    val a by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "wave")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        StateBadge(if (active) AssistantState.ListeningForCommand else AssistantState.Idle)
        Spacer(Modifier.height(8.dp))
        Text(if (active) "تحدث الآن…" else "انقر للاستماع", style = MaterialTheme.typography.labelLarge)
        if (active) Text("▁▃▆█▆▃▁", color = Color(0xFF00E5FF).copy(alpha = a))
    }
}
