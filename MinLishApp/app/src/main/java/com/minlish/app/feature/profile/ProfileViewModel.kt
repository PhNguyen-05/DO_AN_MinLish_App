package com.minlish.app.feature.profile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.ProfileUpdateRequest
import com.minlish.app.data.model.UserSettingsRequest
import com.minlish.app.data.repository.MinLishRepository
import com.minlish.app.data.remote.RetrofitClient
import com.minlish.app.feature.notification.NotificationScheduler
import com.minlish.app.feature.notification.ReminderPreferences
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MinLishRepository.getInstance(application)
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

    // Settings States
    var pushEnabled by mutableStateOf(true)
    var emailEnabled by mutableStateOf(true)
    var reminderHour by mutableStateOf(20)
    var reminderMinute by mutableStateOf(0)

    var sendingTestEmail by mutableStateOf(false)
        private set
    var emailTestResult by mutableStateOf("")
    private var reminderSettingsSaveJob: Job? = null

    fun onPushEnabledChange(value: Boolean) {
        pushEnabled = value
        persistReminderSettings()
    }

    fun onEmailEnabledChange(value: Boolean) {
        emailEnabled = value
        persistReminderSettings()
    }

    fun onReminderTimeChange(hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
        persistReminderSettings()
    }
    fun clearEmailTestResult() { emailTestResult = "" }

    fun loadProfile() {
        val token = UserSession.token ?: return
        loading = true
        viewModelScope.launch {
            try {
                // Fetch profile
                val resp = repository.getProfile(token)
                fullName = resp.full_name.orEmpty()
                targetGoal = resp.target_goal.orEmpty()
                level = resp.current_level ?: "A1"
                avatarBase64 = resp.avatarBase64
                avatarMimeType = resp.avatarMimeType
                avatarUrl = RetrofitClient.resolveServerUrl(resp.avatar_url)
                removeAvatar = false
                error = ""

                // Fetch settings
                val settings = repository.getSettings(token)
                pushEnabled = settings.notifications_enabled == 1
                emailEnabled = settings.email_notifications_enabled == 1
                val timeParts = settings.daily_reminder_time?.split(":") ?: listOf("20", "00")
                reminderHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
                reminderMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                applyReminderSettingsLocally(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading profile/settings", e)
                error = "Không thể tải hồ sơ hoặc cài đặt nhắc nhở: ${e.message ?: e.localizedMessage ?: e.toString()}"
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

    fun saveProfile(context: Context, onSaved: () -> Unit = {}) {
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
                val res = repository.updateProfile(token, req)

                applyReminderSettingsLocally(context)
                repository.updateSettings(token, reminderSettingsRequest())

                if (res.isSuccessful) {
                    res.body()?.let { updated ->
                        fullName = updated.full_name.orEmpty()
                        targetGoal = updated.target_goal.orEmpty()
                        level = updated.current_level ?: "A1"
                        avatarUrl = RetrofitClient.resolveServerUrl(updated.avatar_url)
                        avatarBase64 = null
                        avatarMimeType = null
                        removeAvatar = false
                    }
                    successMessage = "Lưu hồ sơ và cài đặt thành công."
                    error = ""
                    onSaved()
                } else {
                    error = res.errorBody()?.string()?.toApiErrorMessage() ?: "Cập nhật hồ sơ thất bại"
                }
            } catch (e: Exception) {
                error = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                loading = false
            }
        }
    }

    fun sendTestEmail() {
        val token = UserSession.token ?: run {
            emailTestResult = "Phiên đăng nhập không hợp lệ"
            return
        }
        sendingTestEmail = true
        emailTestResult = ""
        viewModelScope.launch {
            try {
                val response = repository.sendStudyReminderEmail(token)
                emailTestResult = response.message ?: "Đã gửi email nhắc học thành công. Vui lòng kiểm tra hộp thư!"
            } catch (e: Exception) {
                emailTestResult = "Không gửi được email: ${e.localizedMessage}"
            } finally {
                sendingTestEmail = false
            }
        }
    }

    fun resetState() {
        fullName = ""
        targetGoal = ""
        level = "A1"
        avatarBase64 = null
        avatarMimeType = null
        avatarUrl = null
        removeAvatar = false
        loading = false
        error = ""
        successMessage = ""
        pushEnabled = true
        emailEnabled = true
        reminderHour = 20
        reminderMinute = 0
        sendingTestEmail = false
        emailTestResult = ""
        reminderSettingsSaveJob?.cancel()
        reminderSettingsSaveJob = null
    }

    private fun persistReminderSettings() {
        applyReminderSettingsLocally(getApplication())
        val token = UserSession.token ?: return

        reminderSettingsSaveJob?.cancel()
        reminderSettingsSaveJob = viewModelScope.launch {
            delay(400)
            try {
                repository.updateSettings(token, reminderSettingsRequest())
                error = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Đã lưu lịch nhắc trên thiết bị nhưng chưa thể đồng bộ email với server: ${e.localizedMessage}"
            }
        }
    }

    private fun reminderSettingsRequest(): UserSettingsRequest {
        val time = String.format(java.util.Locale.US, "%02d:%02d:00", reminderHour, reminderMinute)
        return UserSettingsRequest(
            dailyReminderTime = time,
            notificationsEnabled = pushEnabled,
            emailNotificationsEnabled = emailEnabled
        )
    }

    private fun applyReminderSettingsLocally(context: Context) {
        ReminderPreferences.setEmailNotificationsEnabled(context, emailEnabled)
        NotificationScheduler.syncDailyReminder(context, pushEnabled, reminderHour, reminderMinute)
    }

    private fun String.toApiErrorMessage(): String? {
        return runCatching {
            val json = JSONObject(this)
            json.optString("message").ifBlank { json.optString("error") }.ifBlank { null }
        }.getOrNull()
    }
}
