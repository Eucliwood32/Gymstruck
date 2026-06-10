package com.example.gymstruck.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

interface AudioCueController {
    fun startDucking()  // 오디오 포커스 요청 → 배경 음악 자동 더킹 (지속)
    fun stopDucking()   // 오디오 포커스 해제 → 배경 음악 볼륨 복귀
    fun release()
}

class DefaultAudioCueController(context: Context) : AudioCueController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // TRANSIENT_MAY_DUCK 포커스 요청 → 시스템이 배경 음악 볼륨을 자동으로 낮춘다 (API 26+)
    private val focusRequest = AudioFocusRequest
        .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { }
        .build()

    override fun startDucking() {
        audioManager.requestAudioFocus(focusRequest)
    }

    override fun stopDucking() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    override fun release() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}
