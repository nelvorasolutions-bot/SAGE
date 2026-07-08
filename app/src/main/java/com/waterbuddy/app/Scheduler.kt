package com.waterbuddy.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Schedules the next reminder via AlarmManager so it fires even when the
 *  app is closed. Uses exact-and-allow-while-idle, which also grants a short
 *  window to start the overlay foreground service when it fires. */
object Scheduler {
    private const val REQ = 7001

    private fun pending(c: Context): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            c, REQ, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleNext(c: Context, minutesFromNow: Int = Store.interval(c)) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + minutesFromNow * 60_000L
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(c))
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(c))
        }
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java)?.cancel(pending(c))
    }
}
