package com.palkin.sleepcube.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

enum class SleepMode(val beatFreq: Float, val label: String) {
    DEEP_SLEEP(2f, "Глубокий сон"),
    NAP(10f, "Вздремнуть"),
    VIVID_DREAMS(6f, "Яркие сны"),
    WAKE_UP(40f, "Пробуждение"),
}

/** Длительность сессии. null = бесконечно. */
enum class SessionDuration(val minutes: Int?, val label: String) {
    MIN_20(20, "20 мин"),
    MIN_30(30, "30 мин"),
    MIN_45(45, "45 мин"),
    HOUR_1(60, "1 ч"),
    HOUR_2(120, "2 ч"),
    HOUR_4(240, "4 ч"),
    HOUR_8(480, "8 ч"),
    INFINITE(null, "∞"),
}

class AudioEngine {
    private val sampleRate = 44100
    private val baseFreq = 200f
    private val framesPerBuffer = 4096

    private var binauralTrack: AudioTrack? = null
    private var noiseTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile var beatFreq: Float = 2f
    @Volatile var mainVolume: Float = 0.5f
    @Volatile var noiseVolume: Float = 0.25f

    fun start(mode: SleepMode, noiseEnabled: Boolean) {
        beatFreq = mode.beatFreq
        startBinaural()
        if (noiseEnabled) startPinkNoise()
    }

    private fun startBinaural() {
        // Stereo: left=baseFreq, right=baseFreq+beatFreq
        val bufBytes = framesPerBuffer * 2 * 2 // frames * channels * bytes
        binauralTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        binauralTrack?.play()

        scope.launch {
            val buf = ShortArray(framesPerBuffer * 2)
            var frame = 0L
            while (isActive) {
                val bf = beatFreq
                val vol = (mainVolume * Short.MAX_VALUE).toInt()
                for (i in 0 until framesPerBuffer) {
                    val t = frame.toDouble() / sampleRate
                    buf[i * 2] = (sin(2.0 * PI * baseFreq * t) * vol)
                        .toInt().coerceIn(-32768, 32767).toShort()
                    buf[i * 2 + 1] = (sin(2.0 * PI * (baseFreq + bf) * t) * vol)
                        .toInt().coerceIn(-32768, 32767).toShort()
                    frame++
                }
                binauralTrack?.write(buf, 0, buf.size)
            }
        }
    }

    private fun startPinkNoise() {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        noiseTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        noiseTrack?.play()

        scope.launch {
            val buf = ShortArray(2048)
            val rng = java.util.Random()
            // Paul Kellet pink noise filter state
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
            var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0

            while (isActive) {
                val vol = (noiseVolume * Short.MAX_VALUE * 0.25).toInt()
                for (i in buf.indices) {
                    val white = rng.nextGaussian()
                    b0 = 0.99886 * b0 + white * 0.0555179
                    b1 = 0.99332 * b1 + white * 0.0750759
                    b2 = 0.96900 * b2 + white * 0.1538520
                    b3 = 0.86650 * b3 + white * 0.3104856
                    b4 = 0.55000 * b4 + white * 0.5329522
                    b5 = -0.7616 * b5 - white * 0.0168980
                    val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362).coerceIn(-4.0, 4.0) / 4.0
                    b6 = white * 0.115926
                    buf[i] = (pink * vol).toInt().coerceIn(-32768, 32767).toShort()
                }
                noiseTrack?.write(buf, 0, buf.size)
            }
        }
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
        binauralTrack?.stop(); binauralTrack?.release(); binauralTrack = null
        noiseTrack?.stop(); noiseTrack?.release(); noiseTrack = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
