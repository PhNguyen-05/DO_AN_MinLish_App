package com.minlish.app.data.model

data class LearningPlanResponse(
    val daily_new_words_goal: Int,
    val daily_review_goal: Int,
    val new_words_available: Int,
    val due_review_count: Int,
    val words_learned_today: Int,
    val words_reviewed_today: Int
)

data class LearningDeckListResponse(
    val continue_deck: LearningDeckSummary? = null,
    val decks: List<LearningDeckSummary> = emptyList()
)

data class LearningDeckSummary(
    val id: Long,
    val title: String,
    val description: String? = null,
    val total_words: Int,
    val learned_words: Int,
    val new_words_count: Int,
    val due_review_count: Int,
    val last_studied_at: String? = null,
    val is_completed: Boolean,
    val is_in_progress: Boolean
)

data class LearningSessionResponse(
    val mode: String,
    val count: Int,
    val cards: List<LearningCard>
)

data class LearningCard(
    val id: Long,
    val deck_id: Long,
    val deck_title: String,
    val word: String,
    val pronunciation: String? = null,
    val meaning: String,
    val description_en: String? = null,
    val example: String? = null,
    val collocation: String? = null,
    val related_words: String? = null,
    val note: String? = null,
    val image_url: String? = null,
    val audio_url: String? = null,
    val type: String,
    val progress: LearningCardProgress? = null
)

data class LearningCardProgress(
    val ease_factor: Double,
    val repetitions: Int,
    val interval_days: Int,
    val next_review_at: String? = null
)

data class ReviewCardRequest(
    val cardId: Long,
    val quality: Int
)

data class ReviewProgressResponse(
    val card_id: Long,
    val quality: Int,
    val ease_factor: Double,
    val repetitions: Int,
    val interval_days: Int,
    val next_review_at: String
)
