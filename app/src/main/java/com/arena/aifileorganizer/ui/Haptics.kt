package com.arena.aifileorganizer.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * visionOS-style haptics
 * Light tap / success / warning
 */
class VisionHaptics(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick() = vibrate(18, 120)
    fun light() = vibrate(25, 140)
    fun success() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 28, 45, 36), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(60)
        }
    }
    fun warning() = vibrate(45, 180)
    fun heavy() = vibrate(55, 210)

    private fun vibrate(ms: Long, amp: Int) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1,255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(ms)
        }
    }
}

@Composable
fun rememberVisionHaptics(): VisionHaptics {
    val ctx = LocalContext.current
    return remember { VisionHaptics(ctx) }
}
