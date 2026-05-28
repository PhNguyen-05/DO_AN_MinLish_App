package com.minlish.app.data.model

data class ProfileResponse(
    val id: Long? = null,
    val email: String? = null,
    val full_name: String,
    val target_goal: String,
    val current_level: String
)

data class ProfileUpdateRequest(
    val fullName: String,
    val targetGoal: String,
    val currentLevel: String
)
