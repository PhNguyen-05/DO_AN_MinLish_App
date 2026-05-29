package com.minlish.app.data.model

data class RegisterRequest(
    val email: String,
    val passwordHash: String,
    val fullName: String,

    val targetGoal: String,
    val avatarBase64: String? = null,
    val avatarMimeType: String? = null
    val targetGoal: String
)

data class AuthResponse(
    val token: String,
    val user: UserInfo
)

data class ApiMessageResponse(
    val message: String? = null,
    val error: String? = null
)

data class UserInfo(
    val id: Long,
    val full_name: String,
    val email: String,
    val avatar_url: String? = null
)
data class UserInfo(
    val id: Long,
    val full_name: String,
    val email: String
)

