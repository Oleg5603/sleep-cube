package com.palkin.sleepcube.audio

data class SleepPhase(
    val startMin: Int,
    val endMin: Int,
    val stage: String,       // N1, N2, N3, REM
    val targetFreq: Float,   // Гц для бинауральных ритмов
    val label: String,
    val cycle: Int,
)

/**
 * Программа сна для 60+: 5 циклов, ~450 минут.
 * Частоты подобраны по таблице фаз сна:
 *   N1  8–13 → 10 Гц (альфа → тета, засыпание)
 *   N2   4–8 →  6 Гц (тета, лёгкий сон)
 *   N3 0.5–4 →  1.5 Гц (дельта, глубокий сон)
 *   REM смешанная → 7 Гц (тета, сновидения)
 */
object SleepProgram60 {
    val phases = listOf(
        // ── Цикл 1 ─────────────────────────────────────
        SleepPhase(0,   10,  "N1",  10.0f, "Засыпание",         1),
        SleepPhase(10,  35,  "N2",   6.0f, "Лёгкий сон",        1),
        SleepPhase(35,  70,  "N3",   1.5f, "Глубокий сон",      1),
        SleepPhase(70,  95,  "REM",  7.0f, "Быстрый сон",       1),
        // ── Цикл 2 ─────────────────────────────────────
        SleepPhase(95,  120, "N2",   6.0f, "Лёгкий сон",        2),
        SleepPhase(120, 145, "N3",   1.5f, "Глубокий сон",      2),
        SleepPhase(145, 175, "REM",  7.0f, "Быстрый сон",       2),
        // ── Цикл 3 ─────────────────────────────────────
        SleepPhase(175, 205, "N2",   6.0f, "Лёгкий сон",        3),
        SleepPhase(205, 210, "N3",   1.5f, "Глубокий сон",      3),
        SleepPhase(210, 250, "REM",  7.0f, "Быстрый сон",       3),
        // ── Цикл 4 ─────────────────────────────────────
        SleepPhase(250, 280, "N2",   6.0f, "Лёгкий сон",        4),
        SleepPhase(280, 285, "N3",   1.5f, "Глубокий сон",      4),
        SleepPhase(285, 330, "REM",  7.0f, "Быстрый сон",       4),
        // ── Цикл 5 ─────────────────────────────────────
        SleepPhase(330, 370, "N2",   8.0f, "Поверхностный сон", 5),
        SleepPhase(370, 420, "REM",  7.0f, "Быстрый сон",       5),
        // ── Пробуждение ─────────────────────────────────
        SleepPhase(420, 450, "N1",  12.0f, "Пробуждение",       5),
    )

    fun getPhaseAt(elapsedMin: Float): SleepPhase =
        phases.firstOrNull { elapsedMin >= it.startMin && elapsedMin < it.endMin }
            ?: phases.last()
}
