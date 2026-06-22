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
        override fun onServiceDisconnected(n: ComponentName) {
            bound = false; service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepCubeTheme {
                var isPlaying by remember { mutableStateOf(false) }
                var mode by remember { mutableStateOf(SleepMode.DEEP_SLEEP) }
                var noiseEnabled by remember { mutableStateOf(false) }
                var elapsed by remember { mutableLongStateOf(0L) }
                var mainVol by remember { mutableFloatStateOf(0.5f) }
                var noiseVol by remember { mutableFloatStateOf(0.25f) }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (isActive) { delay(1000); elapsed++ }
                    } else {
                        elapsed = 0L
                    }
                }

                LaunchedEffect(mainVol) { service?.audioEngine?.mainVolume = mainVol }
                LaunchedEffect(noiseVol) { service?.audioEngine?.noiseVolume = noiseVol }

                MainScreen(
                    isPlaying = isPlaying,
                    selectedMode = mode,
                    elapsedSeconds = elapsed,
                    noiseEnabled = noiseEnabled,
                    mainVolume = mainVol,
                    noiseVolume = noiseVol,
                    onModeSelected = { mode = it },
                    onStartStop = {
                        if (isPlaying) {
                            stopSession(); isPlaying = false
                        } else {
                            startSession(mode, noiseEnabled); isPlaying = true
                        }
                    },
                    onNoiseToggle = { noiseEnabled = it },
                    onMainVolumeChange = { mainVol = it },
                    onNoiseVolumeChange = { noiseVol = it },
                )
            }
        }
    }

    private fun startSession(mode: SleepMode, noise: Boolean) {
        val intent = Intent(this, SleepService::class.java).apply {
            action = SleepService.ACTION_START
            putExtra(SleepService.EXTRA_MODE, mode.name)
            putExtra(SleepService.EXTRA_NOISE, noise)
        }
        startForegroundService(intent)
        bindService(intent, conn, BIND_AUTO_CREATE)
    }

    private fun stopSession() {
        startService(Intent(this, SleepService::class.java).apply {
            action = SleepService.ACTION_STOP
        })
        if (bound) { unbindService(conn); bound = false }
    }

    override fun onDestroy() {
        if (bound) { unbindService(conn); bound = false }
        super.onDestroy()
    }
}
