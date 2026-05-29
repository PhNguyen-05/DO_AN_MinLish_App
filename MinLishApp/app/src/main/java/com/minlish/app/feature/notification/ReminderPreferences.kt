package com.minlish.app.feature.notification

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderPreferences {
    private const val PREFS_NAME = "minlish_reminder_preferences"
    private const val KEY_DAILY_PUSH_ENABLED = "daily_push_enabled"
    private const val KEY_EMAIL_NOTIFICATIONS_ENABLED = "email_notifications_enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_LAST_UPDATE_DATE = "last_update_date"
    private const val KEY_WORDS_LEARNED_TODAY = "words_learned_today"
    private const val KEY_WORDS_REVIEWED_TODAY = "words_reviewed_today"
    private const val KEY_DUE_REVIEW_COUNT = "due_review_count"

    fun isDailyPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DAILY_PUSH_ENABLED, false)

    fun setDailyPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DAILY_PUSH_ENABLED, enabled).apply()
    }

    fun isEmailNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, true)

    fun setEmailNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun saveReminderTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    fun getReminderHour(context: Context): Int = prefs(context).getInt(KEY_HOUR, 20)

    fun getReminderMinute(context: Context): Int = prefs(context).getInt(KEY_MINUTE, 0)

    fun getLastUpdateDate(context: Context): String =
        prefs(context).getString(KEY_LAST_UPDATE_DATE, "") ?: ""

    fun getWordsLearnedToday(context: Context): Int =
        prefs(context).getInt(KEY_WORDS_LEARNED_TODAY, 0)

    fun getWordsReviewedToday(context: Context): Int =
        prefs(context).getInt(KEY_WORDS_REVIEWED_TODAY, 0)

    fun getDueReviewCount(context: Context): Int =
        prefs(context).getInt(KEY_DUE_REVIEW_COUNT, 0)

    fun updateStudyStats(context: Context, wordsLearnedToday: Int, wordsReviewedToday: Int, dueReviewCount: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs(context).edit()
            .putString(KEY_LAST_UPDATE_DATE, todayStr)
            .putInt(KEY_WORDS_LEARNED_TODAY, wordsLearnedToday)
            .putInt(KEY_WORDS_REVIEWED_TODAY, wordsReviewedToday)
            .putInt(KEY_DUE_REVIEW_COUNT, dueReviewCount)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
