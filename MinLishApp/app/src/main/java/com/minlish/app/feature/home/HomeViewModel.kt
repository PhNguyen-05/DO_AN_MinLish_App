package com.minlish.app.feature.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.model.LearningDeckListResponse
import com.minlish.app.data.model.LearningPlanResponse
import com.minlish.app.data.model.NotificationSummaryResponse
import com.minlish.app.data.model.ProgressResponse
import com.minlish.app.data.repository.MinLishRepository
import com.minlish.app.feature.notification.NotificationScheduler
import com.minlish.app.feature.notification.ReminderPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MinLishRepository.getInstance(application)
    private val appContext = application.applicationContext
    private var reminderSettingsSynced = false
    private var reminderSettingsSyncing = false

    var dashboardState by mutableStateOf<DashboardResponse?>(null)
        private set

    var learningPlanState by mutableStateOf<LearningPlanResponse?>(null)
        private set

    var learningDecksState by mutableStateOf(LearningDeckListResponse())
        private set

    var progressState by mutableStateOf<ProgressResponse?>(null)
        private set

    var notificationSummaryState by mutableStateOf<NotificationSummaryResponse?>(null)
        private set

    fun fetchDashboardData() {
        val token = UserSession.token ?: return
        viewModelScope.launch {
            if (!reminderSettingsSynced && !reminderSettingsSyncing) {
                reminderSettingsSyncing = true
                launch {
                    try {
                        val settings = repository.getSettings(token)
                        val timeParts = settings.daily_reminder_time?.split(":").orEmpty()
                        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
                        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                        ReminderPreferences.setEmailNotificationsEnabled(
                            appContext,
                            settings.email_notifications_enabled == 1
                        )
                        NotificationScheduler.syncDailyReminder(
                            context = appContext,
                            enabled = settings.notifications_enabled == 1,
                            hour = hour,
                            minute = minute
                        )
                        reminderSettingsSynced = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Retry the settings sync the next time the dashboard refreshes.
                    } finally {
                        reminderSettingsSyncing = false
                    }
                }
            }

            // 1. Sync pending reviews sequentially first to prevent race conditions
            try {
                repository.syncPendingReviews(token)
            } catch (e: Exception) {
                // Ignore sync errors and continue fetching cached/updated values
            }

            // 2. Fetch all states in parallel
            launch {
                try {
                    dashboardState = repository.getDashboard(token)
                } catch (e: Exception) {
                    // Fallback to cache or keep current state
                }
            }
            launch {
                try {
                    progressState = repository.getProgress(token)
                } catch (e: Exception) {
                    // Keep current state
                }
            }
            launch {
                try {
                    learningPlanState = repository.getLearningPlan(token)
                } catch (e: Exception) {
                    // Keep current state
                }
            }
            launch {
                try {
                    learningDecksState = repository.getLearningDecks(token)
                } catch (e: Exception) {
                    // Keep current state
                }
            }
            launch {
                try {
                    notificationSummaryState = repository.getNotificationSummary(token)
                } catch (e: Exception) {
                    // Keep current state
                }
            }
        }
    }

    fun resetState() {
        dashboardState = null
        learningPlanState = null
        learningDecksState = LearningDeckListResponse()
        progressState = null
        notificationSummaryState = null
        reminderSettingsSynced = false
        reminderSettingsSyncing = false
    }
}
