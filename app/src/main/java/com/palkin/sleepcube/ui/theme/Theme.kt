package com.palkin.sleepcube.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF7B7BFF),
    secondary = Color(0xFF5555CC),
    background = Color(0xFF0D0D2B),
    surface = Color(0xFF141430),
    onPrimary = Color.White,
    onBackground = Color(0xFFE0E0FF),
    onSurface = Color(0xFFCCCCEE),
)

@Composable
fun SleepCubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
