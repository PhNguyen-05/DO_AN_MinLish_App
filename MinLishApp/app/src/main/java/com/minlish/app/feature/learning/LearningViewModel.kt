package com.minlish.app.feature.learning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.LearningCard
import com.minlish.app.data.model.LearningPlanResponse
import com.minlish.app.data.model.ReviewCardRequest
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class LearningViewModel : ViewModel() {
    var plan by mutableStateOf<LearningPlanResponse?>(null)
        private set

    var cards by mutableStateOf<List<LearningCard>>(emptyList())
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var selectedMode by mutableStateOf("mixed")
        private set

    private var selectedDeckId: Long? = null

    var showingBack by mutableStateOf(false)
        private set

    var loading by mutableStateOf(false)
        private set

    var submitting by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    var message by mutableStateOf("")
        private set

    val currentCard: LearningCard?
        get() = cards.getOrNull(currentIndex)

    val isComplete: Boolean
        get() = cards.isNotEmpty() && currentIndex >= cards.size

    fun loadLearning(mode: String = selectedMode, deckId: Long? = selectedDeckId) {
        val token = UserSession.token ?: run {
            error = "Bạn cần đăng nhập để bắt đầu học."
            return
        }

        selectedMode = mode
        selectedDeckId = deckId
        loading = true
        error = ""
        message = ""
        showingBack = false
        cards = emptyList()
        currentIndex = 0

        viewModelScope.launch {
            try {
                plan = RetrofitClient.instance.getLearningPlan(token)
                val session = RetrofitClient.instance.getLearningSession(token, mode, 20, deckId)
                cards = session.cards
                currentIndex = 0
                if (cards.isEmpty()) {
                    message = "Hôm nay chưa có thẻ cần học trong chế độ này."
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Không tải được phiên học."
            } finally {
                loading = false
            }
        }
    }

    fun flipCard() {
        showingBack = !showingBack
    }

    fun submitReview(quality: Int) {
        val token = UserSession.token ?: run {
            error = "Phiên đăng nhập không hợp lệ."
            return
        }
        val card = currentCard ?: return

        submitting = true
        error = ""
        message = ""
        viewModelScope.launch {
            try {
                val result = RetrofitClient.instance.reviewCard(token, ReviewCardRequest(card.id, quality))
                message = when (quality) {
                    0 -> "Thử lại thẻ này thêm một lần nữa."
                    1 -> "Đã ghi nhận mức Hard. Lần ôn tiếp theo sau ${result.interval_days} ngày."
                    2 -> "Tốt lắm. Lần ôn tiếp theo sau ${result.interval_days} ngày."
                    else -> "Rất ổn. Lần ôn tiếp theo sau ${result.interval_days} ngày."
                }
                plan = RetrofitClient.instance.getLearningPlan(token)
                showingBack = false
                if (quality != 0) {
                    currentIndex += 1
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Không lưu được kết quả học."
            } finally {
                submitting = false
            }
        }
    }
}
