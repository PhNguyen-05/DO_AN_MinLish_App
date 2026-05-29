package com.minlish.app.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!ReminderPreferences.isDailyPushEnabled(context)) return

        NotificationScheduler.scheduleDailyReminder(
            context = context,
            hour = ReminderPreferences.getReminderHour(context),
            minute = ReminderPreferences.getReminderMinute(context)
        )
    }
}
