package com.waterbuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arm reminders after the phone reboots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Store.remindersOn(context)) {
            Scheduler.scheduleNext(context)
        }
    }
}
