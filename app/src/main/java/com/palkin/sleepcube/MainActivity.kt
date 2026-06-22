package com.palkin.sleepcube

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.palkin.sleepcube.audio.SleepMode
import com.palkin.sleepcube.service.SleepService
import com.palkin.sleepcube.ui.MainScreen
import com.palkin.sleepcube.ui.theme.SleepCubeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    private var service: SleepService? = null
    private var bound = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            service = (b as SleepService.SleepBinder).getService()
            bound = true
        }
        override fun onServiceDisconnected(n: ComponentName) { bound = false; service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepCubeTheme {
                var isPlaying     by remember { mutableStateOf(false) }
                var mode          by remember { mutableStateOf(SleepMode.SMART_SLEEP) }
                var noiseEnabled  by remember { mutableStateOf(false) }
                var elapsed       by remember { mutableLongStateOf(0L) }
                var mainVol       by remember { mutableFloatStateOf(0.5f) }
                var noiseVol      by remember { mutableFloatStateOf(0.25f) }
                var durationMin   by remember { mutableIntStateOf(450) }  // 7.5ч по умолчанию
                var phaseName     by remember { mutableStateOf("") }
                var phaseStage    by remember { mutableStateOf("") }
                var phaseCycle    by remember { mutableIntStateOf(0) }
                var currentFreq   by remember { mutableFloatStateOf(0f) }

                // Таймер
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (isActive) { delay(1000); elapsed++ }
                    } else { elapsed = 0L }
                }

                // Polling фазы из сервиса (каждые 2 сек)
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (isActive) {
                            delay(2000)
                            service?.audioEngine?.let {
                                phaseName  = it.currentPhaseName
                                phaseStage = it.currentStage
                                phaseCycle = it.currentCycle
                                currentFreq = it.beatFreq
                            }
                        }
                    } else {
                        phaseName = ""; phaseStage = ""; phaseCycle = 0; currentFreq = 0f
                    }
                }

                // Авто-стоп в UI при достижении длительности
                LaunchedEffect(isPlaying, durationMin) {
                    if (isPlaying && durationMin > 0) {
                        val totalSec = durationMin * 60L
                        while (isActive) {
                            delay(500)
                            if (elapsed >= totalSec) { isPlaying = false; break }
                        }
                    }
                }

                LaunchedEffect(mainVol) { service?.audioEngine?.mainVolume = mainVol }
                LaunchedEffect(noiseVol) { service?.audioEngine?.noiseVolume = noiseVol }

                MainScreen(
                    isPlaying      = isPlaying,
                    selectedMode   = mode,
                    elapsedSeconds = elapsed,
                    durationMinutes = durationMin,
                    noiseEnabled   = noiseEnabled,
                    mainVolume     = mainVol,
                    noiseVolume    = noiseVol,
                    phaseName      = phaseName,
                    phaseStage     = phaseStage,
                    phaseCycle     = phaseCycle,
                    currentFreq    = currentFreq,
                    onModeSelected     = { mode = it },
                    onDurationChange   = { durationMin = it },
                    onStartStop = {
                        if (isPlaying) { stopSession(); isPlaying = false }
                        else { startSession(mode, noiseEnabled, durationMin); isPlaying = true }
                    },
                    onNoiseToggle      = { noiseEnabled = it },
                    onMainVolumeChange = { mainVol = it },
                    onNoiseVolumeChange = { noiseVol = it },
                )
            }
        }
    }

    private fun startSession(mode: SleepMode, noise: Boolean, durationMin: Int) {
        val intent = Intent(this, SleepService::class.java).apply {
            action = SleepService.ACTION_START
            putExtra(SleepService.EXTRA_MODE, mode.name)
            putExtra(SleepService.EXTRA_NOISE, noise)
            if (durationMin > 0) putExtra(SleepService.EXTRA_DURATION_MIN, durationMin)
        }
        startForegroundService(intent)
        bindService(intent, conn, BIND_AUTO_CREATE)
    }

    private fun stopSession() {
        startService(Intent(this, SleepService::class.java).apply { action = SleepService.ACTION_STOP })
        if (bound) { unbindService(conn); bound = false }
    }

    override fun onDestroy() {
        if (bound) { unbindService(conn); bound = false }
        super.onDestroy()
    }
}
