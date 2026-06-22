package com.palkin.sleepcube.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palkin.sleepcube.audio.SleepMode

@Composable
fun MainScreen(
    isPlaying: Boolean,
    selectedMode: SleepMode,
    elapsedSeconds: Long,
    noiseEnabled: Boolean,
    mainVolume: Float,
    noiseVolume: Float,
    onModeSelected: (SleepMode) -> Unit,
    onStartStop: () -> Unit,
    onNoiseToggle: (Boolean) -> Unit,
    onMainVolumeChange: (Float) -> Unit,
    onNoiseVolumeChange: (Float) -> Unit,
) {
    val bg = Color(0xFF0D0D2B)
    val cardBg = Color(0xFF141430)
    val textPrimary = Color(0xFFE0E0FF)
    val textSecondary = Color(0xFF8888AA)
    val accent = Color(0xFF7B7BFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        Text("Кубик Сна", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Бинауральные ритмы", fontSize = 14.sp, color = textSecondary)

        Spacer(Modifier.height(32.dp))

        // Mode cards
        Text("Режим", fontSize = 13.sp, color = textSecondary, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))

        SleepMode.entries.forEach { mode ->
            ModeCard(
                mode = mode,
                isSelected = selectedMode == mode,
                enabled = !isPlaying,
                cardBg = cardBg,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onClick = { onModeSelected(mode) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Pink noise toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Розовый шум", color = textPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = noiseEnabled,
                    onCheckedChange = onNoiseToggle,
                    enabled = !isPlaying,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Volume sliders
        VolumeSlider("Громкость ритмов", mainVolume, textSecondary, accent, onMainVolumeChange)
        Spacer(Modifier.height(12.dp))
        if (noiseEnabled) {
            VolumeSlider("Громкость шума", noiseVolume, textSecondary, accent, onNoiseVolumeChange)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Timer
        if (isPlaying) {
            Text(
                formatTime(elapsedSeconds),
                fontSize = 52.sp,
                fontWeight = FontWeight.Thin,
                color = accent,
            )
            Text("${selectedMode.beatFreq} Гц", fontSize = 13.sp, color = textSecondary)
            Spacer(Modifier.height(24.dp))
        }

        // Start/Stop button
        Button(
            onClick = onStartStop,
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFF3D1A2E) else Color(0xFF1A1A5E),
            ),
            elevation = ButtonDefaults.buttonElevation(6.dp),
        ) {
            Text(
                if (isPlaying) "⏹" else "▶",
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (isPlaying) "Остановить" else "Начать",
            color = textSecondary,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Для бинауральных ритмов нужны стереонаушники",
            fontSize = 11.sp,
            color = Color(0xFF555577),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ModeCard(
    mode: SleepMode,
    isSelected: Boolean,
    enabled: Boolean,
    cardBg: Color,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    val (emoji, desc, range) = when (mode) {
        SleepMode.DEEP_SLEEP -> Triple("💤", "Дельта-волны, глубокий сон", "2 Гц")
        SleepMode.VIVID_DREAMS -> Triple("✨", "Тета-ритмы, яркие сновидения", "6 Гц")
        SleepMode.WAKE_UP -> Triple("☀️", "Гамма-ритмы, мягкое пробуждение", "40 Гц")
    }
    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E1E4E) else cardBg,
        ),
        border = if (isSelected) BorderStroke(1.dp, accent) else null,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.label, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(desc, color = textSecondary, fontSize = 12.sp)
            }
            Text(range, color = accent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    value: Float,
    textColor: Color,
    accent: Color,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = textColor, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
