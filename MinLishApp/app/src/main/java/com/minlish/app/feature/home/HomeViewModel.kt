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
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MinLishRepository.getInstance(application)

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
    }
}
