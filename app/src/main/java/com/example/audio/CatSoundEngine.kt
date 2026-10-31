package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class CatSoundType(
    val titleFa: String,
    val titleEn: String,
    val emoji: String,
    val bubbleText: String
) {
    LOUD_MEOW("میو بلند و کش‌دار", "Loud Meow", "📢", "میوووووووو!"),
    SCREAM_CAT("جیغ خنده‌دار", "Screaming Cat", "🙀", "وای میااااااووو!"),
    ANGRY_HISS("فیشش گربه عصبانی", "Angry Hiss", "😾", "فیسسسسسسس!"),
    KITTEN_SQUEAK("بچه گربه ناز", "Cute Kitten", "🐱", "میو کوچولو!"),
    DRAMATIC_HUH("گربه متعجب (چی؟!)", "Dramatic Huh", "👀", "هووووه؟!"),
    CAT_PURR("خرخر آرامش‌بخش", "Cat Purr", "💤", "خرررر خررررر..."),
    OPERA_CAT("آواز اپرای گربه", "Opera Cat", "🎭", "لا لا میوووووو!"),
    TRILL_CHIRP("ترپ ترپ شیطون", "Playful Trill", "✨", "پرررر میو!")
}

class CatSoundEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val soundCache = ConcurrentHashMap<CatSoundType, ByteArray>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        // Pre-render core sounds in background
        scope.launch {
            for (type in CatSoundType.values()) {
                if (isActive) {
                    getOrGenerateSound(type)
                }
            }
        }
    }

    fun playSound(type: CatSoundType, onStarted: () -> Unit = {}, onFinished: () -> Unit = {}) {
        scope.launch {
            val pcmData = getOrGenerateSound(type)
            withContext(Dispatchers.Main) {
                triggerHaptic(type)
                onStarted()
            }
            playPcm(pcmData)
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }

    fun playSoundSync(type: CatSoundType) {
        val pcmData = getOrGenerateSound(type)
        triggerHaptic(type)
        playPcm(pcmData)
    }

    private fun triggerHaptic(type: CatSoundType) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = when (type) {
                        CatSoundType.SCREAM_CAT -> longArrayOf(0, 150, 80, 250, 100, 300)
                        CatSoundType.LOUD_MEOW -> longArrayOf(0, 120, 60, 200)
                        CatSoundType.ANGRY_HISS -> longArrayOf(0, 80, 50, 80, 50, 120)
                        CatSoundType.CAT_PURR -> longArrayOf(0, 40, 60, 40, 60, 40, 60)
                        else -> longArrayOf(0, 100, 60, 100)
                    }
                    val amplitudes = when (type) {
                        CatSoundType.SCREAM_CAT -> intArrayOf(0, 255, 0, 255, 0, 255)
                        CatSoundType.CAT_PURR -> intArrayOf(0, 90, 0, 90, 0, 90, 0)
                        else -> intArrayOf(0, 220, 0, 180)
                    }
                    val effect = VibrationEffect.createWaveform(
                        timings.take(amplitudes.size).toLongArray(),
                        amplitudes,
                        -1
                    )
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(180)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(pcmData: ByteArray) {
        try {
            val sampleRate = 44100
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()

            val durationMs = (pcmData.size * 1000L) / (sampleRate * 2)
            Thread.sleep(durationMs + 50)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun getOrGenerateSound(type: CatSoundType): ByteArray {
        return soundCache.getOrPut(type) {
            when (type) {
                CatSoundType.LOUD_MEOW -> generateLoudMeow()
                CatSoundType.SCREAM_CAT -> generateScreamCat()
                CatSoundType.ANGRY_HISS -> generateAngryHiss()
                CatSoundType.KITTEN_SQUEAK -> generateKittenSqueak()
                CatSoundType.DRAMATIC_HUH -> generateDramaticHuh()
                CatSoundType.CAT_PURR -> generateCatPurr()
                CatSoundType.OPERA_CAT -> generateOperaCat()
                CatSoundType.TRILL_CHIRP -> generateTrillChirp()
            }
        }
    }

    fun getMaxVolumeLevel(): Int {
        return audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }

    fun getCurrentVolumeLevel(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
    }

    fun setMediaVolume(level: Int) {
        try {
            val max = getMaxVolumeLevel()
            val safeLevel = level.coerceIn(0, max)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, safeLevel, AudioManager.FLAG_SHOW_UI)
        } catch (_: Exception) {}
    }

    fun setMaxVolume() {
        setMediaVolume(getMaxVolumeLevel())
    }

    fun release() {
        scope.cancel()
        soundCache.clear()
    }

    // --- Synthetic Waveform Generators ---

    private fun generateLoudMeow(): ByteArray {
        val sampleRate = 44100
        val durationSec = 1.3
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Pitch envelope: starts 420 Hz, rises to 780 Hz at 35%, settles to 380 Hz
            val baseFreq = when {
                progress < 0.35 -> 420.0 + (780.0 - 420.0) * (progress / 0.35)
                else -> 780.0 - (780.0 - 380.0) * ((progress - 0.35) / 0.65)
            }
            // Vocal cat vibrato
            val vibrato = sin(2.0 * PI * 6.5 * t) * 16.0
            val freq = (baseFreq + vibrato).coerceAtLeast(100.0)

            // Amplitude envelope: quick attack, sustained meow, slow release
            val amp = when {
                progress < 0.1 -> progress / 0.1
                progress < 0.7 -> 1.0 - 0.15 * ((progress - 0.1) / 0.6)
                else -> 0.85 * (1.0 - (progress - 0.7) / 0.3)
            }

            // Cat formant harmonics (fundamental + 2nd + 3rd + 4th)
            val wave = sin(phase) * 0.60 +
                    sin(phase * 2.0) * 0.28 +
                    sin(phase * 3.0) * 0.16 +
                    sin(phase * 4.0) * 0.08

            val sampleVal = (wave * amp * 31000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateScreamCat(): ByteArray {
        val sampleRate = 44100
        val durationSec = 1.5
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Piercing high pitch scream: 650 Hz up to 1300 Hz then down to 500 Hz
            val baseFreq = when {
                progress < 0.3 -> 650.0 + (1300.0 - 650.0) * (progress / 0.3)
                progress < 0.75 -> 1300.0 - (1300.0 - 900.0) * ((progress - 0.3) / 0.45)
                else -> 900.0 - (900.0 - 500.0) * ((progress - 0.75) / 0.25)
            }
            val tremolo = sin(2.0 * PI * 8.5 * t) * 35.0
            val freq = (baseFreq + tremolo).coerceAtLeast(150.0)

            val amp = when {
                progress < 0.08 -> progress / 0.08
                progress < 0.8 -> 1.0
                else -> (1.0 - (progress - 0.8) / 0.2)
            }

            // Comedic vocal distortion
            var rawWave = sin(phase) * 0.7 + sin(phase * 2.1) * 0.4 + sin(phase * 3.0) * 0.25
            if (rawWave > 0.85) rawWave = 0.85 + (rawWave - 0.85) * 0.3
            if (rawWave < -0.85) rawWave = -0.85 + (rawWave + 0.85) * 0.3

            val sampleVal = (rawWave * amp * 32000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateAngryHiss(): ByteArray {
        val sampleRate = 44100
        val durationSec = 0.9
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random(42)

        var lastSample = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Noise burst with sharp hiss attack
            val whiteNoise = (random.nextDouble() * 2.0 - 1.0)
            // High-pass filter for sharp snake-like cat hiss
            val filteredNoise = whiteNoise - 0.75 * lastSample
            lastSample = whiteNoise

            val amp = when {
                progress < 0.05 -> progress / 0.05
                progress < 0.4 -> 1.0
                else -> exp(-4.0 * (progress - 0.4))
            }

            val sampleVal = (filteredNoise * amp * 28000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateKittenSqueak(): ByteArray {
        val sampleRate = 44100
        val durationSec = 0.75
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            val freq = when {
                progress < 0.4 -> 950.0 + (1450.0 - 950.0) * (progress / 0.4)
                else -> 1450.0 - (1450.0 - 850.0) * ((progress - 0.4) / 0.6)
            }
            val amp = when {
                progress < 0.08 -> progress / 0.08
                progress < 0.6 -> 0.9
                else -> 0.9 * (1.0 - (progress - 0.6) / 0.4)
            }

            val wave = sin(phase) * 0.8 + sin(phase * 2.0) * 0.3
            val sampleVal = (wave * amp * 26000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateDramaticHuh(): ByteArray {
        val sampleRate = 44100
        val durationSec = 0.45
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Rapid rising intonation ("Huh?!")
            val freq = 340.0 + (820.0 - 340.0) * (progress * progress)
            val amp = when {
                progress < 0.1 -> progress / 0.1
                progress < 0.7 -> 1.0
                else -> (1.0 - (progress - 0.7) / 0.3)
            }

            val wave = sin(phase) * 0.7 + sin(phase * 2.0) * 0.25 + sin(phase * 3.0) * 0.15
            val sampleVal = (wave * amp * 29000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateCatPurr(): ByteArray {
        val sampleRate = 44100
        val durationSec = 1.8
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        var throatPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Low frequency rumble 32 Hz + 64 Hz harmonic
            val baseRumble = sin(phase) * 0.65 + sin(phase * 2.0) * 0.35
            // Rhythmic throat oscillation at 26 Hz
            val throatMod = 0.6 + 0.4 * sin(throatPhase)

            val amp = when {
                progress < 0.1 -> progress / 0.1
                progress > 0.9 -> (1.0 - (progress - 0.9) / 0.1)
                else -> 1.0
            }

            val sampleVal = (baseRumble * throatMod * amp * 24000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * 36.0 / sampleRate
            throatPhase += 2.0 * PI * 26.0 / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
            if (throatPhase > 2.0 * PI) throatPhase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateOperaCat(): ByteArray {
        val sampleRate = 44100
        val durationSec = 1.7
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Sweeping vibrato opera meow
            val sweep = 500.0 + 350.0 * sin(PI * progress)
            val vibrato = sin(2.0 * PI * 7.0 * t) * 30.0
            val freq = sweep + vibrato

            val amp = when {
                progress < 0.15 -> progress / 0.15
                progress < 0.8 -> 1.0
                else -> (1.0 - (progress - 0.8) / 0.2)
            }

            val wave = sin(phase) * 0.6 + sin(phase * 2.0) * 0.3 + sin(phase * 3.0) * 0.15
            val sampleVal = (wave * amp * 29000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun generateTrillChirp(): ByteArray {
        val sampleRate = 44100
        val durationSec = 0.8
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSec

            // Rapid 18 Hz trill
            val trill = sin(2.0 * PI * 18.0 * t) * 90.0
            val base = 580.0 + (750.0 - 580.0) * progress
            val freq = (base + trill).coerceAtLeast(200.0)

            val amp = when {
                progress < 0.1 -> progress / 0.1
                progress > 0.8 -> (1.0 - (progress - 0.8) / 0.2)
                else -> 1.0
            }

            val wave = sin(phase) * 0.75 + sin(phase * 2.0) * 0.25
            val sampleVal = (wave * amp * 27000.0).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return shortArrayToByteArray(buffer)
    }

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            val s = shorts[i].toInt()
            bytes[i * 2] = (s and 0x00FF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0x00FF).toByte()
        }
        return bytes
    }
}
