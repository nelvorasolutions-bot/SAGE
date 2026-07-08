package com.waterbuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Fires on each scheduled reminder. If it's within active hours and the
 *  goal isn't met yet, it sends the buddy walking onto the screen, then
 *  schedules the next reminder. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Store.remindersOn(context)) return

        val shouldShow = Store.inActiveHours(context) && !Store.goalReached(context)
        if (shouldShow) {
            Store.setLastRemindTs(context, System.currentTimeMillis())
            val svc = Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        }
        // keep the chain going regardless, so it checks again next interval
        Scheduler.scheduleNext(context)
    }
}
