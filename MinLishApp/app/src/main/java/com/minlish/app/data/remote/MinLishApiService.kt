package com.minlish.app.data.remote

import com.minlish.app.BuildConfig
import com.minlish.app.data.model.ApiMessageResponse
import com.minlish.app.data.model.AuthResponse
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.model.LearningDeckListResponse
import com.minlish.app.data.model.LearningPlanResponse
import com.minlish.app.data.model.LearningSessionResponse
import com.minlish.app.data.model.NotificationSummaryResponse
import com.minlish.app.data.model.ProfileResponse
import com.minlish.app.data.model.ProfileUpdateRequest
import com.minlish.app.data.model.ProgressResponse
import com.minlish.app.data.model.UserSettingsRequest
import com.minlish.app.data.model.UserSettingsResponse
import com.minlish.app.data.model.RegisterRequest
import com.minlish.app.data.model.ReviewCardRequest
import com.minlish.app.data.model.ReviewProgressResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MinLishApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("api/dashboard")
    suspend fun getDashboard(@Header("Authorization") token: String): DashboardResponse

    @GET("api/dashboard/progress")
    suspend fun getProgress(@Header("Authorization") token: String): ProgressResponse

    // Forgot / Reset password endpoints (placeholders - backend must implement)
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<ApiMessageResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Response<ApiMessageResponse>

    // User profile endpoints
    @GET("api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ProfileResponse

    @PUT("api/user/profile")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body request: ProfileUpdateRequest): Response<ProfileResponse>

    @GET("api/user/settings")
    suspend fun getSettings(@Header("Authorization") token: String): UserSettingsResponse

    @PUT("api/user/settings")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body request: UserSettingsRequest
    ): UserSettingsResponse

    @GET("api/learning/plan")
    suspend fun getLearningPlan(@Header("Authorization") token: String): LearningPlanResponse

    @GET("api/learning/decks")
    suspend fun getLearningDecks(@Header("Authorization") token: String): LearningDeckListResponse

    @GET("api/learning/session")
    suspend fun getLearningSession(
        @Header("Authorization") token: String,
        @Query("mode") mode: String,
        @Query("limit") limit: Int,
        @Query("deckId") deckId: Long? = null
    ): LearningSessionResponse

    @POST("api/learning/review")
    suspend fun reviewCard(
        @Header("Authorization") token: String,
        @Body request: ReviewCardRequest
    ): ReviewProgressResponse

    @GET("api/notifications/summary")
    suspend fun getNotificationSummary(@Header("Authorization") token: String): NotificationSummaryResponse

    @POST("api/notifications/email/reminder")
    suspend fun sendStudyReminderEmail(@Header("Authorization") token: String): ApiMessageResponse
}

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    val instance: MinLishApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinLishApiService::class.java)
    }

    fun resolveServerUrl(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        return BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
    }
}

