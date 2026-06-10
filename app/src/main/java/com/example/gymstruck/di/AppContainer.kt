package com.example.gymstruck.di

import android.content.Context
import android.os.SystemClock
import com.example.gymstruck.BuildConfig
import com.example.gymstruck.data.DataStorePresetRepository
import com.example.gymstruck.data.PresetRepository
import com.example.gymstruck.data.YouTubeDataApiSearchService
import com.example.gymstruck.data.YouTubeSearchService
import com.example.gymstruck.domain.TimerEngine
import com.example.gymstruck.platform.AlarmScheduler
import com.example.gymstruck.platform.AudioCueController
import com.example.gymstruck.platform.DefaultAlarmScheduler
import com.example.gymstruck.platform.DefaultAudioCueController

class AppContainer private constructor(context: Context) {

    val engine = TimerEngine(now = { SystemClock.elapsedRealtime() })
    val audioController: AudioCueController = DefaultAudioCueController(context)
    val alarmScheduler: AlarmScheduler = DefaultAlarmScheduler(context)
    val presetRepository: PresetRepository = DataStorePresetRepository(context)
    val youTubeSearchService: YouTubeSearchService =
        YouTubeDataApiSearchService(BuildConfig.YOUTUBE_API_KEY)

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
