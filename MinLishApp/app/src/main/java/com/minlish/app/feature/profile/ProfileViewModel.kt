package com.minlish.app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.ProfileUpdateRequest
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class ProfileViewModel : ViewModel() {
    var fullName by mutableStateOf("")
        private set

    var targetGoal by mutableStateOf("")
        private set

    var level by mutableStateOf("A1")
        private set

    var avatarBase64 by mutableStateOf<String?>(null)
        private set

    var avatarMimeType by mutableStateOf<String?>(null)
        private set

    var avatarUrl by mutableStateOf<String?>(null)
        private set

    private var removeAvatar by mutableStateOf(false)

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    var successMessage by mutableStateOf("")
        private set

    fun loadProfile() {
        val token = UserSession.token ?: return
        loading = true
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.instance.getProfile(token)
                fullName = resp.full_name
                targetGoal = resp.target_goal
                level = resp.current_level
                avatarBase64 = resp.avatarBase64
                avatarMimeType = resp.avatarMimeType
                avatarUrl = RetrofitClient.resolveServerUrl(resp.avatar_url)
                removeAvatar = false
                error = ""
            } catch (e: Exception) {
                error = "Không thể tải hồ sơ"
            } finally {
                loading = false
            }
        }
    }

    fun onFullNameChange(value: String) { fullName = value }
    fun onTargetGoalChange(value: String) { targetGoal = value }
    fun onLevelChange(value: String) { level = value }
    fun onAvatarChange(base64: String?, mimeType: String?) {
        avatarBase64 = base64
        avatarMimeType = mimeType
        avatarUrl = null
        removeAvatar = base64 == null
        error = ""
        successMessage = ""
    }

    fun saveProfile(onSaved: () -> Unit = {}) {
        val token = UserSession.token ?: run {
            error = "Không có token đăng nhập"
            return
        }
        loading = true
        error = ""
        successMessage = ""
        viewModelScope.launch {
            try {
                val req = ProfileUpdateRequest(
                    fullName = fullName.trim(),
                    targetGoal = targetGoal.trim(),
                    currentLevel = level.trim(),
                    avatarBase64 = avatarBase64,
                    avatarMimeType = avatarMimeType,
                    removeAvatar = removeAvatar
                )
                val res = RetrofitClient.instance.updateProfile(token, req)
                if (res.isSuccessful) {
                    res.body()?.let { updated ->
                        fullName = updated.full_name
                        targetGoal = updated.target_goal
                        level = updated.current_level
                        avatarUrl = RetrofitClient.resolveServerUrl(updated.avatar_url)
                        avatarBase64 = null
                        avatarMimeType = null
                        removeAvatar = false
                    }
                    successMessage = "Cập nhật thành công"
                    error = ""
                    onSaved()
                } else {
                    error = res.errorBody()?.string()?.toApiErrorMessage() ?: "Cập nhật thất bại"
                }
            } catch (e: Exception) {
                error = "Lỗi kết nối"
            } finally {
                loading = false
            }
        }
    }

    private fun String.toApiErrorMessage(): String? {
        return runCatching {
            val json = JSONObject(this)
            json.optString("message").ifBlank { json.optString("error") }.ifBlank { null }
        }.getOrNull()
    }
}
