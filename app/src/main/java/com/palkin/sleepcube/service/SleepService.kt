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

class SleepService : Service() {

    inner class SleepBinder : Binder() {
        fun getService(): SleepService = this@SleepService
    }

    val audioEngine = AudioEngine()
    private val binder = SleepBinder()
    private val handler = Handler(Looper.getMainLooper())
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
                val durationLabel = if (durationMin > 0) " · ${formatDuration(durationMin)}" else ""
                startForeground(NOTIF_ID, buildNotification(mode.label + durationLabel))
                audioEngine.start(mode, noise)
                handler.removeCallbacks(stopRunnable)
                if (durationMin > 0) {
                    handler.postDelayed(stopRunnable, durationMin * 60_000L)
                }
            }
            ACTION_STOP -> {
                handler.removeCallbacks(stopRunnable)
                audioEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        audioEngine.release()
        super.onDestroy()
    }

    private fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}ч ${m}м"
            h > 0 -> "${h}ч"
            else -> "${m}м"
        }
    }

    private fun buildNotification(modeLabel: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SleepService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Кубик Сна активен")
            .setContentText("Режим: $modeLabel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Стоп", stopIntent)
            .setOngoing(true)
            .build()
    }
}
