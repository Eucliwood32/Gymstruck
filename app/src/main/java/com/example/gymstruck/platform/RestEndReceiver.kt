package com.example.gymstruck.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymstruck.di.AppContainer

class RestEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = AppContainer.getInstance(context)
        // 백그라운드에서 휴식 종료: 더킹 시작으로 알림
        container.audioController.startDucking()
    }
}
