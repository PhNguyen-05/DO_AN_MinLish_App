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

    var registerError by mutableStateOf("")
        private set

    // Kiểm tra độ mạnh mật khẩu
    fun validatePassword(password: String): String? {
        if (password.length < 8) {
            return "Mật khẩu phải có ít nhất 8 ký tự"
        }
        if (!password.any { it.isUpperCase() }) {
            return "Phải có ít nhất 1 chữ hoa (A-Z)"
        }
        if (!password.any { it.isLowerCase() }) {
            return "Phải có ít nhất 1 chữ thường (a-z)"
        }
        if (!password.any { it.isDigit() }) {
            return "Phải có ít nhất 1 chữ số (0-9)"
        }
        if (!password.any { "!@#\$%^&*()_+=[]{}|;:,.<>?/-".contains(it) }) {
            return "Phải có ít nhất 1 ký tự đặc biệt (!@#\$%^&*)"
        }
        return null
    }

    // Kiểm tra xác nhận mật khẩu
    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return "Vui lòng xác nhận mật khẩu"
        if (password != confirmPassword) return "Mật khẩu xác nhận không khớp"
        return null
    }

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
        val passwordError = validatePassword(password)
        if (passwordError != null) {
            registerError = passwordError
            return
        }

        viewModelScope.launch {
            try {
                val req = RegisterRequest(email, password, fullName, targetGoal)
                val res = RetrofitClient.instance.register(req)

                registerMessage = if (res.isSuccessful) {
                    "Đăng ký tài khoản thành công!"
                } else {
                    "Đăng ký thất bại. Email có thể đã tồn tại."
                }
                registerError = ""
            } catch (e: Exception) {
                registerMessage = "Lỗi kết nối với máy chủ. Vui lòng thử lại."
            }
        }
    }
}