package com.minlish.app.data.model

data class ProfileResponse(
    val id: Long? = null,
    val email: String? = null,
    val full_name: String,
    val target_goal: String,
    val current_level: String,
    val avatar_url: String? = null,
    val avatarBase64: String? = null,
    val avatarMimeType: String? = null
)

data class ProfileUpdateRequest(
    val fullName: String,
    val targetGoal: String,
    val currentLevel: String,
    val avatarBase64: String? = null,
    val avatarMimeType: String? = null,
    val removeAvatar: Boolean = false
)
