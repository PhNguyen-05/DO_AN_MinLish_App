package com.minlish.app.data.model

data class DashboardResponse(
    val full_name: String,
    val target_goal: String,
    val current_level: String,
    val current_streak: Int,
    val total_words_learned: Int,
    val accuracy_rate: Float,
    val daily_new_words_goal: Int,
    val avatar_url: String? = null
)

data class ProgressResponse(
    val daily_activity: List<DailyActivityItem> = emptyList(),
    val retention_rate: Float,
    val estimated_level: String,
    val level_reason: String
)

data class DailyActivityItem(
    val date: String,
    val words_learned: Int,
    val words_reviewed: Int
)
