package com.minlish.app.feature.home


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.DashboardResponse
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var dashboardState by mutableStateOf<DashboardResponse?>(null)
        private set

    fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                UserSession.token?.let { token ->
                    dashboardState = RetrofitClient.instance.getDashboard(token)
                }
            } catch (e: Exception) {
                // Xử lý khi gặp lỗi mất kết nối internet
            }
        }
    }
}