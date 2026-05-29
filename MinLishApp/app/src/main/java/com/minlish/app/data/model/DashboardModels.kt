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
