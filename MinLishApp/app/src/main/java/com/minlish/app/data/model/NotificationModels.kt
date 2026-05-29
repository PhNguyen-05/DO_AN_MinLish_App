package com.minlish.app.data.model

data class NotificationSummaryResponse(
    val email: String,
    val full_name: String? = null,
    val daily_new_words_goal: Int,
    val daily_review_goal: Int,
    val new_words_available: Int,
    val due_review_count: Int,
    val words_learned_today: Int,
    val words_reviewed_today: Int,
    val push_title: String,
    val push_body: String
)
