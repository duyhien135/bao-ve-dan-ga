package com.example.baovedanga

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Quản lý âm thanh hiệu ứng bằng SoundPool (độ trễ thấp, phát chồng được).
// File âm thanh trong res/raw/, tổng hợp bằng synth_sounds.py.
class SoundManager(context: Context) {
    var muted by mutableStateOf(false)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mapOf(
        "hit" to soundPool.load(context, R.raw.snd_hit, 1),
        "thud" to soundPool.load(context, R.raw.snd_thud, 1),
        "slash" to soundPool.load(context, R.raw.snd_slash, 1),
        "water" to soundPool.load(context, R.raw.snd_water, 1),
        "boom" to soundPool.load(context, R.raw.snd_boom, 1),
        "zap" to soundPool.load(context, R.raw.snd_zap, 1),
        "arrow" to soundPool.load(context, R.raw.snd_arrow, 1),
        "fire" to soundPool.load(context, R.raw.snd_fire, 1),
        "magic" to soundPool.load(context, R.raw.snd_magic, 1),
        "die" to soundPool.load(context, R.raw.snd_die, 1),
        "growl" to soundPool.load(context, R.raw.snd_growl, 1),
        "chicken" to soundPool.load(context, R.raw.snd_chicken, 1),
        "coin" to soundPool.load(context, R.raw.snd_coin, 1),
        "win" to soundPool.load(context, R.raw.snd_win, 1),
        "lose" to soundPool.load(context, R.raw.snd_lose, 1),
        "click" to soundPool.load(context, R.raw.snd_click, 1),
        "buff" to soundPool.load(context, R.raw.snd_buff, 1),
        "crit" to soundPool.load(context, R.raw.snd_crit, 1),
        "shield" to soundPool.load(context, R.raw.snd_shield, 1),
    )

    fun play(name: String, volume: Float = 1f) {
        if (muted) return
        val id = ids[name] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
