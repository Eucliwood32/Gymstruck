package com.example.gymstruck.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

interface AlarmScheduler {
    fun scheduleRestEnd(triggerAtElapsed: Long)
    fun cancel()
}

class DefaultAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, RestEndReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun scheduleRestEnd(triggerAtElapsed: Long) {
        val pi = buildPendingIntent()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pi
            )
        } else {
            // 정확 알람 권한 없을 때 근사 알람으로 fallback
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pi
            )
        }
    }

    override fun cancel() {
        alarmManager.cancel(buildPendingIntent())
    }
}
