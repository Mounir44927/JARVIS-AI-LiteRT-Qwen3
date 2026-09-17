package com.jarvis.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.jarvis.ai.core.AssistantState

/**
 * JARVIS Rive orb.
 *
 * The project carries a custom .riv generator under tools/rive. The runtime
 * uses only the local jarvis_orb.riv resource so offline operation never depends
 * on a remote CDN or a network animation fallback.
 */
@Composable
fun JarvisRiveOrb(
    state: AssistantState,
    voiceLevel: Float,
    modifier: Modifier = Modifier,
) {
    val level = voiceLevel.coerceIn(0f, 1f)
    val stateScale by animateFloatAsState(
        targetValue = when (state) {
            AssistantState.Idle -> 1f
            AssistantState.ListeningForWakeWord -> 1.03f
            AssistantState.ListeningForCommand -> 1.06f + level * 0.10f
            AssistantState.Thinking -> 1.025f
            AssistantState.Speaking -> 1.05f
            is AssistantState.Error -> 0.98f
        },
        label = "jarvis-orb-scale",
    )

    val ringAlpha = when (state) {
        AssistantState.Idle -> 0.22f
        AssistantState.ListeningForWakeWord -> 0.34f
        AssistantState.ListeningForCommand -> 0.46f
        AssistantState.Thinking -> 0.32f
        AssistantState.Speaking -> 0.40f
        is AssistantState.Error -> 0.50f
    }

    val animationName = when (state) {
        AssistantState.Idle -> "Idle"
        AssistantState.ListeningForWakeWord,
        AssistantState.ListeningForCommand -> "Listening"
        AssistantState.Thinking -> "Thinking"
        AssistantState.Speaking -> "Speaking"
        is AssistantState.Error -> "Error"
    }

    Box(
        modifier = modifier
            .size(220.dp)
            .graphicsLayer {
                scaleX = stateScale
                scaleY = stateScale
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val baseRadius = size.minDimension * 0.31f
            val reactiveRadius = baseRadius * (1f + level * 0.18f)
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.08f + ringAlpha * 0.28f),
                radius = reactiveRadius * 1.42f,
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = ringAlpha),
                radius = reactiveRadius,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFFFFC94D).copy(alpha = 0.28f + level * 0.22f),
                radius = baseRadius * 0.50f,
            )
        }

        AndroidView(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color.Transparent),
            factory = { context ->
                RiveAnimationView(context).also { view ->
                    val localResId = context.resources.getIdentifier(
                        "jarvis_orb",
                        "raw",
                        context.packageName,
                    )

                    runCatching {
                        if (localResId != 0) {
                            view.setRiveResource(
                                resId = localResId,
                                artboardName = "JARVIS_ORB",
                                animationName = animationName,
                                autoplay = true,
                            )
                            view.tag = animationName
                        }
                    }
                }
            },
            update = { view ->
                val localResId = view.context.resources.getIdentifier(
                    "jarvis_orb",
                    "raw",
                    view.context.packageName,
                )
                if (localResId != 0 && view.tag != animationName) {
                    runCatching {
                        view.play(animationName = animationName)
                        view.tag = animationName
                    }
                }
            },
        )
    }
}
