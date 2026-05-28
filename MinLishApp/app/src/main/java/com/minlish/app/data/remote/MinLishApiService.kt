package com.minlish.app.data.remote

import com.minlish.app.data.model.AuthResponse
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.model.RegisterRequest
import com.minlish.app.data.model.ProfileResponse
import com.minlish.app.data.model.ProfileUpdateRequest
import com.minlish.app.data.model.ApiMessageResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface MinLishApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("api/dashboard")
    suspend fun getDashboard(@Header("Authorization") token: String): DashboardResponse

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
}

object RetrofitClient {
    // Default for Android emulator (AVD): use host machine via 10.0.2.2
    // If you run the app on a physical device, set this to your PC LAN IP (e.g. "http://192.168.0.114:3000/")
    // For Genymotion emulator use 10.0.3.2
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val instance: MinLishApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinLishApiService::class.java)
    }
}
