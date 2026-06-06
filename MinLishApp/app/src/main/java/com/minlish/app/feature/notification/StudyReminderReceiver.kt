package com.minlish.app.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ctx = context ?: return

        if (!ReminderPreferences.isDailyPushEnabled(ctx)) return

        NotificationScheduler.scheduleDailyReminder(
            context = ctx,
            hour = ReminderPreferences.getReminderHour(ctx),
            minute = ReminderPreferences.getReminderMinute(ctx)
        )

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastUpdateDate = ReminderPreferences.getLastUpdateDate(ctx)
        val studiedToday = if (lastUpdateDate == todayStr) {
            val learned = ReminderPreferences.getWordsLearnedToday(ctx)
            val reviewed = ReminderPreferences.getWordsReviewedToday(ctx)
            learned > 0 || reviewed > 0
        } else {
            false
        }
        val dueReviewCount = ReminderPreferences.getDueReviewCount(ctx)

        if (studiedToday) return

        val bodyText = when {
            dueReviewCount > 0 -> {
                "Bạn đang có $dueReviewCount thẻ cần ôn tập. Hãy dành ít phút học để duy trì streak nhé!"
            }
            else -> {
                "Hôm nay bạn chưa học từ mới nào. Hãy mở MinLish ngay để tiếp tục giữ chuỗi học tập nhé!"
            }
        }

        NotificationScheduler.showStudyReminder(
            context = ctx,
            title = "MinLish nhắc học",
            body = bodyText
        )
    }
}
