package com.minlish.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_cache")
data class DashboardCache(
    @PrimaryKey val id: Int = 1,
    val fullName: String,
    val targetGoal: String,
    val currentLevel: String,
    val currentStreak: Int,
    val totalWordsLearned: Int,
    val accuracyRate: Float,
    val dailyNewWordsGoal: Int,
    val avatarUrl: String?
)

@Entity(tableName = "progress_cache")
data class ProgressCache(
    @PrimaryKey val id: Int = 1,
    val dailyActivityJson: String, // Serialized JSON string of List<DailyActivityItem>
    val retentionRate: Float,
    val estimatedLevel: String,
    val levelReason: String
)

@Entity(tableName = "learning_plan_cache")
data class LearningPlanCache(
    @PrimaryKey val id: Int = 1,
    val dailyNewWordsGoal: Int,
    val dailyReviewGoal: Int,
    val newWordsAvailable: Int,
    val dueReviewCount: Int,
    val wordsLearnedToday: Int,
    val wordsReviewedToday: Int
)

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String?,
    val totalWords: Int,
    val learnedWords: Int,
    val newWordsCount: Int,
    val dueReviewCount: Int,
    val lastStudiedAt: String?,
    val isCompleted: Boolean,
    val isInProgress: Boolean
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Long,
    val deckId: Long,
    val deckTitle: String,
    val word: String,
    val pronunciation: String?,
    val meaning: String,
    val descriptionEn: String?,
    val example: String?,
    val collocation: String?,
    val relatedWords: String?,
    val note: String?,
    val imageUrl: String?,
    val audioUrl: String?,
    val type: String, // "new" or "review"
    val easeFactor: Double,
    val repetitions: Int,
    val intervalDays: Int,
    val nextReviewAt: String?
)

@Entity(tableName = "pending_reviews")
data class PendingReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val quality: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_cache")
data class ProfileCache(
    @PrimaryKey val id: Int = 1,
    val email: String,
    val fullName: String,
    val targetGoal: String,
    val currentLevel: String,
    val avatarUrl: String?
)

@Entity(tableName = "user_settings_cache")
data class UserSettingsCache(
    @PrimaryKey val id: Int = 1,
    val theme: String,
    val dailyReminderTime: String?,
    val notificationsEnabled: Int,
    val emailNotificationsEnabled: Int,
    val dailyNewWordsGoal: Int,
    val dailyReviewGoal: Int
)

@Entity(tableName = "notification_summary_cache")
data class NotificationSummaryCache(
    @PrimaryKey val id: Int = 1,
    val email: String,
    val fullName: String?,
    val dailyNewWordsGoal: Int,
    val dailyReviewGoal: Int,
    val newWordsAvailable: Int,
    val dueReviewCount: Int,
    val wordsLearnedToday: Int,
    val wordsReviewedToday: Int,
    val pushTitle: String,
    val pushBody: String
)
