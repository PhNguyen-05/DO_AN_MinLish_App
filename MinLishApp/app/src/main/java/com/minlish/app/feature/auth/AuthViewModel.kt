package com.minlish.app.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.RegisterRequest
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel : ViewModel() {

    var loginError by mutableStateOf("")
        private set

    var registerMessage by mutableStateOf("")
        private set

    var registerError by mutableStateOf("")
        private set
    var forgotMessage by mutableStateOf("")
        private set

    var forgotError by mutableStateOf("")
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
                val response = RetrofitClient.instance.login(mapOf("email" to email.trim(), "password" to password))
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
                val req = RegisterRequest(email.trim(), password, fullName.trim(), targetGoal.trim())
                val req = RegisterRequest(email, password, fullName, targetGoal)
                val res = RetrofitClient.instance.register(req)

                registerMessage = if (res.isSuccessful) {
                    "Đăng ký tài khoản thành công!"
                } else {
                    res.errorBody()?.string()?.toApiErrorMessage()
                        ?: "Đăng ký thất bại. Email có thể đã tồn tại."
                }
                registerError = ""
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register failed", e)
                registerMessage = "Lỗi kết nối với máy chủ. Vui lòng thử lại."
                registerError = e.localizedMessage ?: e.toString()
            }
        }
    }

    fun registerWithAvatar(
        email: String,
        password: String,
        fullName: String,
        targetGoal: String,
        avatarBase64: String? = null,
        avatarMimeType: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        registerMessage = ""
        registerError = ""

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            registerError = passwordError
            return
        }

        viewModelScope.launch {
            try {
                val req = RegisterRequest(
                    email = email.trim(),
                    passwordHash = password,
                    fullName = fullName.trim(),
                    targetGoal = targetGoal.trim(),
                    avatarBase64 = avatarBase64,
                    avatarMimeType = avatarMimeType
                )
                val res = RetrofitClient.instance.register(req)

                if (res.isSuccessful) {
                    registerMessage = "Đăng ký tài khoản thành công!"
                    registerError = ""
                    onSuccess()
                } else {
                    registerMessage = ""
                    registerError = res.errorBody()?.string()?.toApiErrorMessage()
                        ?: "Đăng ký thất bại. Email có thể đã tồn tại."
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register failed", e)
                registerMessage = ""
                registerError = "Lỗi kết nối với máy chủ. Vui lòng thử lại."
            }
        }
    }

    fun clearRegisterState() {
        registerMessage = ""
        registerError = ""
    }

    fun sendForgotPassword(email: String, onSent: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isEmpty()) {
            forgotMessage = ""
            forgotError = "Vui lòng nhập email."
            onError(forgotError)
            return
        }

        forgotMessage = ""
        forgotError = ""
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.forgotPassword(mapOf("email" to normalizedEmail))
                if (response.isSuccessful) {
                    forgotMessage = response.body()?.message ?: "Mã OTP đã được gửi tới email"
                    forgotError = ""
                    onSent()
                } else {
                    forgotError = response.errorBody()?.string()?.toApiErrorMessage()
                        ?: "Không thể gửi mã OTP. Vui lòng thử lại."
                    onError(forgotError)
                }
            } catch (e: Exception) {
                forgotError = "Lỗi kết nối. Vui lòng thử lại sau."
                onError(forgotError)
            }
        }
    }

    fun resetPassword(email: String, otp: String, newPassword: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val body = mapOf(
                    "email" to email.trim(),
                    "otp" to otp.trim(),
                    "newPassword" to newPassword
                )
                val response = RetrofitClient.instance.resetPassword(body)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError(
                        response.errorBody()?.string()?.toApiErrorMessage()
                            ?: "Đặt lại mật khẩu thất bại. OTP có thể không đúng."
                    )
                }
            } catch (e: Exception) {
                onError("Lỗi máy chủ. Vui lòng thử lại.")
            }
        }
    }

    private fun String.toApiErrorMessage(): String? {
        return runCatching {
            val json = JSONObject(this)
            json.optString("message").ifBlank { json.optString("error") }.ifBlank { null }
        }.getOrNull()
    }

    // Đăng xuất: xóa token phiên và reset thông báo
    fun logout() {
        UserSession.token = null
        loginError = ""
        registerMessage = ""
        registerError = ""
        forgotMessage = ""
        forgotError = ""
    }
}
                    "Đăng ký thất bại. Email có thể đã tồn tại."
                }
                registerError = ""
            } catch (e: Exception) {
                registerMessage = "Lỗi kết nối với máy chủ. Vui lòng thử lại."
            }
        }
    }
}