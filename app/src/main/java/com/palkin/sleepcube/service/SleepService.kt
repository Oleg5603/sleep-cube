package com.palkin.sleepcube.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.palkin.sleepcube.MainActivity
import com.palkin.sleepcube.R
import com.palkin.sleepcube.audio.AudioEngine
import com.palkin.sleepcube.audio.SleepMode
import com.palkin.sleepcube.audio.SleepProgram60
import kotlinx.coroutines.*

class SleepService : Service() {

    inner class SleepBinder : Binder() {
        fun getService(): SleepService = this@SleepService
    }

    val audioEngine = AudioEngine()
    private val binder = SleepBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var programJob: Job? = null

    private val stopRunnable = Runnable {
        audioEngine.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val CHANNEL_ID = "sleep_cube_ch"
        const val NOTIF_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_MODE = "MODE"
        const val EXTRA_NOISE = "NOISE"
        const val EXTRA_DURATION_MIN = "DURATION_MIN"
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID, "Звуковой кубик сна", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Управление сессией сна" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val mode = SleepMode.valueOf(
                    intent.getStringExtra(EXTRA_MODE) ?: SleepMode.DEEP_SLEEP.name
                )
                val noise = intent.getBooleanExtra(EXTRA_NOISE, false)
                val durationMin = intent.getIntExtra(EXTRA_DURATION_MIN, -1)
                val durationLabel = if (durationMin > 0) " · ${fmtDuration(durationMin)}" else ""
                startForeground(NOTIF_ID, buildNotification(mode.label + durationLabel))
                audioEngine.start(mode, noise)

                // Таймер авто-остановки
                handler.removeCallbacks(stopRunnable)
                if (durationMin > 0) {
                    handler.postDelayed(stopRunnable, durationMin * 60_000L)
                }

                // Программная смена фаз для Умного сна
                if (mode == SleepMode.SMART_SLEEP) {
                    startSmartProgram()
                }
            }
            ACTION_STOP -> {
                handler.removeCallbacks(stopRunnable)
                programJob?.cancel()
                audioEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Каждую секунду проверяет фазу и плавно обновляет целевую частоту. */
    private fun startSmartProgram() {
        programJob?.cancel()
        programJob = serviceScope.launch {
            var elapsedSec = 0L
            var lastStage = ""
            while (isActive) {
                delay(1000)
                elapsedSec++
                val elapsedMin = elapsedSec / 60f
                val phase = SleepProgram60.getPhaseAt(elapsedMin)

                audioEngine.targetBeatFreq = phase.targetFreq
                audioEngine.currentPhaseName = phase.label
                audioEngine.currentStage = phase.stage
                audioEngine.currentCycle = phase.cycle

                // Обновляем уведомление при смене стадии
                if (phase.stage != lastStage) {
                    lastStage = phase.stage
                    val notif = buildNotification(
                        "Цикл ${phase.cycle} · ${phase.label} · ${phase.targetFreq} Гц"
                    )
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIF_ID, notif)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        programJob?.cancel()
        serviceScope.cancel()
        audioEngine.release()
        super.onDestroy()
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SleepService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Кубик Сна")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Стоп", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun fmtDuration(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        return when { h > 0 && m > 0 -> "${h}ч ${m}м"; h > 0 -> "${h}ч"; else -> "${m}м" }
    }
}
