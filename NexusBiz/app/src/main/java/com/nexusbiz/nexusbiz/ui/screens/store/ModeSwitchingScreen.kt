package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModeSwitchingScreen(
    targetMode: ModeSwitchTarget,
    onFinish: () -> Unit
) {
    LaunchedEffect(targetMode) {
        delay(1000)
        onFinish()
    }

    val iconScale = remember { Animatable(0.8f) }
    LaunchedEffect(targetMode) {
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    val iconAlpha = remember { Animatable(0f) }
    LaunchedEffect(targetMode) {
        launch {
            iconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, delayMillis = 200, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(targetMode.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        BubbleField()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        ambientColor = targetMode.iconShadowColor,
                        spotColor = targetMode.iconShadowColor
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.98f))
                    .scale(iconScale.value)
                    .graphicsLayer(alpha = iconAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = targetMode.icon,
                    contentDescription = null,
                    tint = targetMode.iconColor,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = targetMode.title,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            targetMode.subtitle?.let {
                val textAlpha by targetModeTextAlpha()
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.graphicsLayer(alpha = textAlpha)
                )
            }
        }
    }
}

@Composable
private fun targetModeTextAlpha() = rememberInfiniteTransition().animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(delayMillis = 200, durationMillis = 800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)

@Composable
private fun BubbleField() {
    val specs = remember {
        listOf(
            BubbleSpec(6.dp, (-60).dp, (-70).dp, 0, 6f, 0.4f..0.6f),
            BubbleSpec(8.dp, 40.dp, (-80).dp, 220, 8f, 0.35f..0.55f),
            BubbleSpec(10.dp, (-30).dp, 60.dp, 120, 10f, 0.45f..0.65f),
            BubbleSpec(6.dp, 55.dp, 50.dp, 180, 7f, 0.4f..0.6f),
            BubbleSpec(8.dp, 10.dp, (-110).dp, 60, 9f, 0.3f..0.5f)
        )
    }
    specs.forEach { Bubble(it) }
}

@Composable
private fun Bubble(spec: BubbleSpec) {
    val transition = rememberInfiniteTransition()
    val vertical by transition.animateFloat(
        initialValue = 0f,
        targetValue = spec.yAmplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, delayMillis = spec.delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by transition.animateFloat(
        initialValue = spec.alphaRange.start,
        targetValue = spec.alphaRange.endInclusive,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, delayMillis = spec.delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(spec.size)
            .offset(x = spec.offsetX, y = spec.offsetY + vertical.dp)
            .graphicsLayer(alpha = alpha)
            .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
    )
}

private data class BubbleSpec(
    val size: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val delayMs: Int,
    val yAmplitude: Float,
    val alphaRange: ClosedFloatingPointRange<Float>
)

