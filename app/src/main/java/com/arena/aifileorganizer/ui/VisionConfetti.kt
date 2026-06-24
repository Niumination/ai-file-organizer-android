package com.arena.aifileorganizer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * visionOS Liquid Glass confetti
 * Lottie file: assets/confetti_vision.json
 * Source: custom – inspired by:
 * - Liquid Effect Animation – thanh – 21st.dev – 264★
 * - Animated AI Chat – jatin-yadav05
 */
@Composable
fun VisionConfetti(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {}
) {
    if (!visible) return
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("confetti_vision.json")
    )
    var isPlaying by remember { mutableStateOf(true) }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        speed = 1.15f,
        iterations = 1
    )
    LaunchedEffect(progress) {
        if (progress >= 0.99f) {
            isPlaying = false
            onFinish()
        }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}
