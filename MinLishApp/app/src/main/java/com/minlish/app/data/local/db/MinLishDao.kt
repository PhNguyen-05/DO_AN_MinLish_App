package com.minlish.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MinLishDao {

    // Dashboard
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardCache)

    @Query("SELECT * FROM dashboard_cache WHERE id = 1 LIMIT 1")
    suspend fun getDashboard(): DashboardCache?

    // Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressCache)

    @Query("SELECT * FROM progress_cache WHERE id = 1 LIMIT 1")
    suspend fun getProgress(): ProgressCache?

    // Learning Plan
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningPlan(plan: LearningPlanCache)

    @Query("SELECT * FROM learning_plan_cache WHERE id = 1 LIMIT 1")
    suspend fun getLearningPlan(): LearningPlanCache?

    // Decks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Query("SELECT * FROM decks ORDER BY id ASC")
    suspend fun getDecks(): List<DeckEntity>

    @Query("DELETE FROM decks")
    suspend fun clearDecks()

    @Query("DELETE FROM decks WHERE id > 0")
    suspend fun clearServerDecks()

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    // Cards
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    suspend fun getCardsForDeck(deckId: Long): List<CardEntity>

    // Get due review cards offline (repetitions > 0 and nextReviewAt <= now, sorted by nextReviewAt)
    // nextReviewAt is serialized string format (e.g. ISO-8601 or similar). We compare strings or timestamps.
    // To make it simple and safe offline, we query all cards and filter in memory or compare as ISO date.
    @Query("SELECT * FROM cards WHERE repetitions > 0 AND (nextReviewAt <= :now OR nextReviewAt IS NULL)")
    suspend fun getDueReviewCardsOffline(now: String): List<CardEntity>

    // Get new cards offline (repetitions = 0 or NULL)
    @Query("SELECT * FROM cards WHERE repetitions = 0")
    suspend fun getNewCardsOffline(): List<CardEntity>

    @Query("DELETE FROM cards")
    suspend fun clearCards()

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: Long)

    // Pending Reviews (Sync Queue)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingReview(review: PendingReviewEntity)

    @Query("SELECT * FROM pending_reviews ORDER BY timestamp ASC")
    suspend fun getPendingReviews(): List<PendingReviewEntity>

    @Query("DELETE FROM pending_reviews WHERE id = :id")
    suspend fun deletePendingReview(id: Long)

    @Query("DELETE FROM pending_reviews WHERE cardId = :cardId")
    suspend fun deletePendingReviewByCardId(cardId: Long)

    // Profile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileCache)

    @Query("SELECT * FROM profile_cache WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): ProfileCache?

    // Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsCache)

    @Query("SELECT * FROM user_settings_cache WHERE id = 1 LIMIT 1")
    suspend fun getUserSettings(): UserSettingsCache?

    // Notification Summary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationSummary(summary: NotificationSummaryCache)

    @Query("SELECT * FROM notification_summary_cache WHERE id = 1 LIMIT 1")
    suspend fun getNotificationSummary(): NotificationSummaryCache?
}
