package com.minlish.app.feature.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.RegisterRequest
import com.minlish.app.data.remote.RetrofitClient
import java.io.IOException
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException

class AuthViewModel : ViewModel() {

    var loginError by mutableStateOf("")
        private set
    var loginLoading by mutableStateOf(false)
        private set

    var registerMessage by mutableStateOf("")
        private set
    var registerError by mutableStateOf("")
        private set
    var registerLoading by mutableStateOf(false)
        private set

    var forgotMessage by mutableStateOf("")
        private set
    var forgotError by mutableStateOf("")
        private set
    var forgotLoading by mutableStateOf(false)
        private set

    var resetLoading by mutableStateOf(false)
        private set

    fun validatePassword(password: String): String? {
        if (password.length < 8) return "Mật khẩu phải có ít nhất 8 ký tự"
        if (!password.any { it.isUpperCase() }) return "Mật khẩu cần ít nhất 1 chữ hoa"
        if (!password.any { it.isLowerCase() }) return "Mật khẩu cần ít nhất 1 chữ thường"
        if (!password.any { it.isDigit() }) return "Mật khẩu cần ít nhất 1 chữ số"
        if (!password.any { "!@#\$%^&*()_+=[]{}|;:,.<>?/-".contains(it) }) {
            return "Mật khẩu cần ít nhất 1 ký tự đặc biệt"
        }
        return null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        if (confirmPassword.isBlank()) return "Vui lòng xác nhận mật khẩu"
        if (password != confirmPassword) return "Mật khẩu xác nhận không khớp"
        return null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            loginError = "Vui lòng nhập email và mật khẩu."
            return
        }

        loginLoading = true
        loginError = ""
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.login(
                    mapOf("email" to normalizedEmail, "password" to password)
                )
                UserSession.token = "Bearer ${response.token}"
                loginError = ""
                onSuccess()
            } catch (e: HttpException) {
                loginError = e.response()?.errorBody().toApiErrorMessage()
                    ?: "Sai tài khoản hoặc mật khẩu."
            } catch (e: IOException) {
                loginError = "Không kết nối được server. Kiểm tra backend đã chạy chưa."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "login failed", e)
                loginError = "Đăng nhập thất bại. Vui lòng thử lại."
            } finally {
                loginLoading = false
            }
        }
    }

    fun register(email: String, password: String, fullName: String, targetGoal: String) {
        registerWithAvatar(email, password, fullName, targetGoal)
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
        val normalizedEmail = email.trim()
        val normalizedFullName = fullName.trim()
        val normalizedGoal = targetGoal.trim().ifBlank { "TOEIC 700" }

        registerMessage = ""
        registerError = ""

        if (normalizedFullName.isBlank() || normalizedEmail.isBlank()) {
            registerError = "Vui lòng nhập họ tên và email."
            return
        }

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            registerError = passwordError
            return
        }

        registerLoading = true
        viewModelScope.launch {
            try {
                val request = RegisterRequest(
                    email = normalizedEmail,
                    passwordHash = password,
                    fullName = normalizedFullName,
                    targetGoal = normalizedGoal,
                    avatarBase64 = avatarBase64,
                    avatarMimeType = avatarMimeType
                )
                val response = RetrofitClient.instance.register(request)

                if (response.isSuccessful) {
                    registerMessage = "Đăng ký tài khoản thành công!"
                    registerError = ""
                    onSuccess()
                } else {
                    registerMessage = ""
                    registerError = response.errorBody().toApiErrorMessage()
                        ?: "Đăng ký thất bại. Email có thể đã tồn tại."
                }
            } catch (e: IOException) {
                registerMessage = ""
                registerError = "Không kết nối được server. Kiểm tra backend đã chạy chưa."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register failed", e)
                registerMessage = ""
                registerError = "Đăng ký thất bại. Vui lòng thử lại."
            } finally {
                registerLoading = false
            }
        }
    }

    fun sendForgotPassword(email: String, onSent: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            forgotMessage = ""
            forgotError = "Vui lòng nhập email."
            onError(forgotError)
            return
        }

        forgotLoading = true
        forgotMessage = ""
        forgotError = ""
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.forgotPassword(mapOf("email" to normalizedEmail))
                if (response.isSuccessful) {
                    forgotMessage = response.body()?.message ?: "Mã OTP đã được gửi tới email."
                    forgotError = ""
                    onSent()
                } else {
                    forgotMessage = ""
                    forgotError = response.errorBody().toApiErrorMessage()
                        ?: "Không thể gửi mã OTP. Vui lòng thử lại."
                    onError(forgotError)
                }
            } catch (e: IOException) {
                forgotMessage = ""
                forgotError = "Không kết nối được server. Kiểm tra backend đã chạy chưa."
                onError(forgotError)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "forgot password failed", e)
                forgotMessage = ""
                forgotError = "Không thể gửi mã OTP. Vui lòng thử lại."
                onError(forgotError)
            } finally {
                forgotLoading = false
            }
        }
    }

    fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val normalizedEmail = email.trim()
        val normalizedOtp = otp.trim()

        if (normalizedEmail.isBlank() || normalizedOtp.isBlank()) {
            onError("Vui lòng nhập đầy đủ email và mã OTP.")
            return
        }

        val passwordError = validatePassword(newPassword)
        if (passwordError != null) {
            onError(passwordError)
            return
        }

        resetLoading = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.resetPassword(
                    mapOf(
                        "email" to normalizedEmail,
                        "otp" to normalizedOtp,
                        "newPassword" to newPassword
                    )
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError(
                        response.errorBody().toApiErrorMessage()
                            ?: "Đặt lại mật khẩu thất bại. OTP có thể không đúng."
                    )
                }
            } catch (e: IOException) {
                onError("Không kết nối được server. Kiểm tra backend đã chạy chưa.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "reset password failed", e)
                onError("Đặt lại mật khẩu thất bại. Vui lòng thử lại.")
            } finally {
                resetLoading = false
            }
        }
    }

    fun clearRegisterState() {
        registerMessage = ""
        registerError = ""
    }

    fun clearForgotState() {
        forgotMessage = ""
        forgotError = ""
    }

    fun logout() {
        UserSession.token = null
        loginError = ""
        registerMessage = ""
        registerError = ""
        forgotMessage = ""
        forgotError = ""
    }

    private fun ResponseBody?.toApiErrorMessage(): String? {
        val raw = this?.string().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { null }
        }.getOrNull() ?: raw.takeIf { it.length <= 160 }
    }
}
