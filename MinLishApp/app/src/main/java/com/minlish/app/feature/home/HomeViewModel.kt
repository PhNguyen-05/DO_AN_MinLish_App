package com.minlish.app.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.model.LearningDeckListResponse
import com.minlish.app.data.model.LearningPlanResponse
import com.minlish.app.data.model.NotificationSummaryResponse
import com.minlish.app.data.model.ProgressResponse
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
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
        viewModelScope.launch {
            try {
                UserSession.token?.let { token ->
                    dashboardState = RetrofitClient.instance.getDashboard(token)
                    progressState = RetrofitClient.instance.getProgress(token)
                    learningPlanState = RetrofitClient.instance.getLearningPlan(token)
                    learningDecksState = RetrofitClient.instance.getLearningDecks(token)
                    notificationSummaryState = RetrofitClient.instance.getNotificationSummary(token)
                }
            } catch (e: Exception) {
                // Keep the current cached state when the network is unavailable.
            }
        }
    }
}
