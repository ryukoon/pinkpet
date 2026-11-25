package com.ryukoon.pinkpet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** 파일 없이 코드로 만드는 8bit 효과음 + 진동 */
class Sfx(ctx: Context) {
    var enabled = true
    private val rate = 22050
    private val exec = Executors.newSingleThreadExecutor()
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun vibrate(vararg pattern: Long) {
        val v = vibrator ?: return
        try {
            if (pattern.size == 1) v.vibrate(VibrationEffect.createOneShot(pattern[0], VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(VibrationEffect.createWaveform(longArrayOf(0L) + pattern, -1))
        } catch (_: Exception) { }
    }

    private fun synth(type: String, f0: Float, f1: Float, f2: Float, dur: Float, vol: Float, vib: Float = 0f): ShortArray {
        val n = (rate * dur).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / rate
            val q = t / dur
            var f = if (q < 0.25f) f0 * (f1 / f0).pow(q / 0.25f) else f1 * (f2 / f1).pow((q - 0.25f) / 0.75f)
            if (vib > 0f) f += (f1 * 0.12f * sin(2 * PI * vib * t)).toFloat()
            phase += f / rate
            val ph = phase % 1.0
            val w = when (type) {
                "square" -> if (ph < 0.5) 1.0 else -1.0
                "saw" -> 2 * ph - 1
                "tri" -> 1 - 4 * abs(ph - 0.5)
                else -> sin(2 * PI * ph)
            }
            val env = if (t < 0.02f) t / 0.02f else (1f - (t - 0.02f) / (dur - 0.02f)).coerceAtLeast(0f).pow(2)
            out[i] = (w * env * vol * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun noise(dur: Float, vol: Float): ShortArray {
        val n = (rate * dur).toInt()
        val out = ShortArray(n)
        var prevIn = 0f; var prevOut = 0f
        for (i in 0 until n) {
            val x = (Random.nextFloat() * 2 - 1) * (1f - i.toFloat() / n).pow(3)
            val y = 0.6f * (prevOut + x - prevIn) // 고역 통과
            prevIn = x; prevOut = y
            out[i] = (y * vol * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun mix(base: ShortArray, add: ShortArray, offsetSec: Float) {
        val off = (offsetSec * rate).toInt()
        for (i in add.indices) {
            val j = i + off
            if (j >= base.size) break
            base[j] = (base[j] + add[i]).coerceIn(-32767, 32767).toShort()
        }
    }

    private fun play(make: () -> ShortArray) {
        if (!enabled) return
        exec.execute {
            try {
                val data = make()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(data, 0, data.size)
                track.play()
                Thread.sleep((data.size * 1000L / rate) + 60)
                track.release()
            } catch (_: Exception) { }
        }
    }

    fun tone(type: String, f0: Float, f1: Float, f2: Float, dur: Float, vol: Float, vib: Float = 0f) =
        play { synth(type, f0, f1, f2, dur, vol, vib) }

    fun boing(s: Float) = tone("square", 170f, 560f, 240f, 0.6f, 0.22f * s, 16f)
    fun jump() = tone("square", 300f, 700f, 900f, 0.18f, 0.15f)
    fun grab() = tone("square", 600f, 900f, 500f, 0.12f, 0.12f)
    fun pop() = tone("tri", 500f, 800f, 1000f, 0.15f, 0.2f)
    fun swish() = tone("saw", 900f, 380f, 160f, 0.16f, 0.12f)
    fun stab() = tone("square", 220f, 140f, 80f, 0.12f, 0.3f)
    fun slurp() = tone("sine", 260f, 620f, 380f, 0.16f, 0.15f)
    fun crash() = play {
        val base = noise(0.8f, 0.7f)
        floatArrayOf(2400f, 3300f, 4100f, 2900f, 3700f).forEachIndexed { i, f ->
            mix(base, synth("tri", f, f * 1.02f, f * 0.97f, 0.3f, 0.12f), 0.05f + i * 0.08f)
        }
        base
    }

    fun release() { exec.shutdownNow() }
}
