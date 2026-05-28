package com.minlish.app.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.RegisterRequest
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var loginError by mutableStateOf("")
        private set

    var registerMessage by mutableStateOf("")
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.login(mapOf("email" to email, "password" to password))
                UserSession.token = "Bearer ${response.token}"
                loginError = ""
                onSuccess()
            } catch (e: Exception) {
                loginError = "Sai tài khoản hoặc mật khẩu!"
            }
        }
    }

    fun register(email: String, password: String, fullName: String, targetGoal: String) {
        viewModelScope.launch {
            try {
                val req = RegisterRequest(email, password, fullName, targetGoal)
                val res = RetrofitClient.instance.register(req)
                registerMessage = if (res.isSuccessful) {
                    "Đăng ký thành công! Hãy quay lại đăng nhập."
                } else {
                    "Đăng ký thất bại. Email có thể đã tồn tại."
                }
            } catch (e: Exception) {
                registerMessage = "Lỗi kết nối tới hệ thống máy chủ!"
            }
        }
    }
}