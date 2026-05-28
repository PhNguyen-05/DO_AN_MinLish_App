package com.minlish.app.data.remote

import com.minlish.app.data.model.AuthResponse
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MinLishApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @GET("api/dashboard")
    suspend fun getDashboard(@Header("Authorization") token: String): DashboardResponse
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.114:3000/"
    //private const val BASE_URL = "http://10.0.2.2:3000/"
    //private const val BASE_URL = "http://172.16.31.163:3000/"

    val instance: MinLishApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinLishApiService::class.java)
    }
}