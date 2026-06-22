package com.palkin.sleepcube.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palkin.sleepcube.audio.SleepMode
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    isPlaying: Boolean,
    selectedMode: SleepMode,
    elapsedSeconds: Long,
    durationMinutes: Int,
    noiseEnabled: Boolean,
    mainVolume: Float,
    noiseVolume: Float,
    phaseName: String,
    phaseStage: String,
    phaseCycle: Int,
    currentFreq: Float,
    onModeSelected: (SleepMode) -> Unit,
    onStartStop: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onNoiseToggle: (Boolean) -> Unit,
    onMainVolumeChange: (Float) -> Unit,
    onNoiseVolumeChange: (Float) -> Unit,
) {
    val bg          = Color(0xFF0D0D2B)
    val cardBg      = Color(0xFF141430)
    val textPrimary = Color(0xFFE0E0FF)
    val textSec     = Color(0xFF8888AA)
    val accent      = Color(0xFF7B7BFF)

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
        Text("Бинауральные ритмы", fontSize = 14.sp, color = textSec)

        Spacer(Modifier.height(24.dp))

        // ── Карточка текущей фазы (Умный сон) ─────────────────────────────
        if (isPlaying && selectedMode == SleepMode.SMART_SLEEP && phaseName.isNotEmpty()) {
            PhaseCard(
                phaseName  = phaseName,
                phaseStage = phaseStage,
                cycle      = phaseCycle,
                freq       = currentFreq,
                accent     = accent,
                cardBg     = cardBg,
                textPrimary = textPrimary,
                textSec    = textSec,
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Режим ─────────────────────────────────────────────────────────
        SectionLabel("Режим", textSec)
        Spacer(Modifier.height(8.dp))
        SleepMode.entries.forEach { m ->
            ModeCard(
                mode       = m,
                isSelected = selectedMode == m,
                enabled    = !isPlaying,
                cardBg     = cardBg, accent = accent,
                textPrimary = textPrimary, textSec = textSec,
                onClick    = { onModeSelected(m) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        // ── Длительность ──────────────────────────────────────────────────
        SectionLabel("Длительность", textSec)
        Spacer(Modifier.height(8.dp))
        DurationPicker(
            minutes    = durationMinutes,
            enabled    = !isPlaying,
            cardBg     = cardBg, accent = accent,
            textPrimary = textPrimary, textSec = textSec,
            onValueChange = onDurationChange,
        )

        Spacer(Modifier.height(12.dp))

        // ── Розовый шум ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Розовый шум", color = textPrimary, modifier = Modifier.weight(1f))
                Switch(checked = noiseEnabled, onCheckedChange = onNoiseToggle, enabled = !isPlaying)
            }
        }

        Spacer(Modifier.height(12.dp))

        VolumeSlider("Громкость ритмов", mainVolume,  textSec, accent, onMainVolumeChange)
        if (noiseEnabled) {
            Spacer(Modifier.height(8.dp))
            VolumeSlider("Громкость шума", noiseVolume, textSec, accent, onNoiseVolumeChange)
        }

        Spacer(Modifier.height(20.dp))

        // ── Таймер ────────────────────────────────────────────────────────
        if (isPlaying) {
            val remaining = if (durationMinutes > 0) {
                val r = durationMinutes * 60L - elapsedSeconds
                if (r > 0) r else 0L
            } else null

            Text(
                if (remaining != null) formatTime(remaining) else formatTime(elapsedSeconds),
                fontSize = 52.sp, fontWeight = FontWeight.Thin, color = accent,
            )
            Text(
                if (remaining != null) "осталось" else "прошло",
                fontSize = 13.sp, color = textSec,
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Кнопка ────────────────────────────────────────────────────────
        Button(
            onClick = onStartStop,
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFF3D1A2E) else Color(0xFF1A1A5E),
            ),
            elevation = ButtonDefaults.buttonElevation(6.dp),
        ) {
            Text(if (isPlaying) "⏹" else "▶", fontSize = 36.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(6.dp))
        Text(if (isPlaying) "Остановить" else "Начать", color = textSec, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))
        Text(
            "Для бинауральных ритмов нужны стереонаушники",
            fontSize = 11.sp, color = Color(0xFF555577), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ── Карточка текущей фазы ─────────────────────────────────────────────────

@Composable
private fun PhaseCard(
    phaseName: String, phaseStage: String, cycle: Int, freq: Float,
    accent: Color, cardBg: Color, textPrimary: Color, textSec: Color,
) {
    val stageColor = when (phaseStage) {
        "N3"  -> Color(0xFF4488FF)
        "REM" -> Color(0xFFAA66FF)
        "N1"  -> Color(0xFF66AAFF)
        else  -> accent
    }
    val stageEmoji = when (phaseStage) {
        "N3"  -> "💤"
        "REM" -> "✨"
        "N1"  -> "😴"
        else  -> "🌙"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0E30)),
        border = BorderStroke(1.dp, stageColor.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stageEmoji, fontSize = 32.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(phaseName, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "Стадия $phaseStage · Цикл $cycle",
                    color = textSec, fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.1f Гц".format(freq),
                    color = stageColor, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                )
                Text("бинаур.", color = textSec, fontSize = 10.sp)
            }
        }
    }
}

// ── Карточка режима ──────────────────────────────────────────────────────

@Composable
private fun ModeCard(
    mode: SleepMode, isSelected: Boolean, enabled: Boolean,
    cardBg: Color, accent: Color, textPrimary: Color, textSec: Color,
    onClick: () -> Unit,
) {
    val (emoji, desc, rangeLabel) = when (mode) {
        SleepMode.SMART_SLEEP  -> Triple("🧠", "5 циклов · авто N1→N2→N3→REM", "авто")
        SleepMode.DEEP_SLEEP   -> Triple("💤", "Дельта-волны, глубокий сон",    "2 Гц")
        SleepMode.NAP          -> Triple("😴", "Альфа-ритмы, короткий отдых",   "10 Гц")
        SleepMode.VIVID_DREAMS -> Triple("✨", "Тета-ритмы, яркие сновидения",  "6 Гц")
        SleepMode.WAKE_UP      -> Triple("☀️", "Гамма-ритмы, мягкое пробуждение","40 Гц")
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
                Text(desc, color = textSec, fontSize = 12.sp)
            }
            Text(rangeLabel, color = accent, fontSize = 12.sp)
        }
    }
}

// ── Выбор длительности ───────────────────────────────────────────────────

@Composable
private fun DurationPicker(
    minutes: Int, enabled: Boolean,
    cardBg: Color, accent: Color, textPrimary: Color, textSec: Color,
    onValueChange: (Int) -> Unit,
) {
    var textInput by remember(minutes) {
        mutableStateOf(if (minutes == 0) "" else minutes.toString())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (minutes == 0) "∞  без ограничения" else fmtDurationLabel(minutes),
                    color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { v ->
                        textInput = v.filter { it.isDigit() }.take(3)
                        onValueChange((textInput.toIntOrNull() ?: 0).coerceIn(0, 480))
                    },
                    enabled = enabled,
                    label = { Text("мин", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = textSec,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = minutes.toFloat(),
                onValueChange = { v ->
                    val s = (v / 5).roundToInt() * 5
                    onValueChange(s)
                    textInput = if (s == 0) "" else s.toString()
                },
                valueRange = 0f..480f,
                steps = 95,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("∞", color = textSec, fontSize = 11.sp)
                Text("30м", color = textSec, fontSize = 11.sp)
                Text("2ч", color = textSec, fontSize = 11.sp)
                Text("4ч", color = textSec, fontSize = 11.sp)
                Text("8ч", color = textSec, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text, fontSize = 13.sp, color = color, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun VolumeSlider(label: String, value: Float, textColor: Color, accent: Color, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = textColor, fontSize = 12.sp)
        Slider(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun fmtDurationLabel(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return when { h > 0 && m > 0 -> "${h} ч ${m} мин"; h > 0 -> "${h} ч"; else -> "${m} мин" }
}
