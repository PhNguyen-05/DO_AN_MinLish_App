package com.minlish.app.data.model

data class UserSettingsResponse(
    val theme: String?,
    val daily_reminder_time: String?,
    val notifications_enabled: Int?,
    val email_notifications_enabled: Int?,
    val daily_new_words_goal: Int?,
    val daily_review_goal: Int?
)

data class UserSettingsRequest(
    val theme: String? = null,
    val dailyReminderTime: String? = null,
    val notificationsEnabled: Boolean? = null,
    val emailNotificationsEnabled: Boolean? = null,
    val dailyNewWordsGoal: Int? = null,
    val dailyReviewGoal: Int? = null
)
