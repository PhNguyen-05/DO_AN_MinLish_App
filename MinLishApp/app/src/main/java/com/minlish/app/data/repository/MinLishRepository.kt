package com.minlish.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.minlish.app.data.local.db.*
import com.minlish.app.data.model.*
import com.minlish.app.data.remote.MinLishApiService
import com.minlish.app.data.remote.RetrofitClient
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MinLishRepository private constructor(context: Context) {

    private val db = MinLishDatabase.getInstance(context)
    private val dao = db.dao()
    private val api: MinLishApiService = RetrofitClient.instance
    private val gson = Gson()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        @Volatile
        private var INSTANCE: MinLishRepository? = null

        fun getInstance(context: Context): MinLishRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MinLishRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }


    suspend fun importWordSet(wordSet: WordSet): WordSet {
        val deckId = wordSet.deckId ?: newLocalId()
        val imported = wordSet.copy(deckId = deckId)
        val sourceDescription = imported.sourceFileName.takeIf { it.isNotBlank() }?.let { "Imported from $it" }

        dao.insertDecks(
            listOf(
                DeckEntity(
                    id = deckId,
                    title = imported.name,
                    description = sourceDescription,
                    totalWords = imported.words.size,
                    learnedWords = 0,
                    newWordsCount = imported.words.size,
                    dueReviewCount = 0,
                    lastStudiedAt = null,
                    isCompleted = imported.words.isEmpty(),
                    isInProgress = false
                )
            )
        )

        val usedIds = mutableSetOf<Long>()
        val cards = imported.words.map { word ->
            val cardId = generateSequence { newLocalId() }.first { usedIds.add(it) }
            CardEntity(
                id = cardId,
                deckId = deckId,
                deckTitle = imported.name,
                word = word.term,
                pronunciation = null,
                meaning = word.meaning,
                descriptionEn = null,
                example = word.example.ifBlank { null },
                collocation = null,
                relatedWords = word.tags.joinToString(", ").ifBlank { null },
                note = word.note.ifBlank { null },
                imageUrl = null,
                audioUrl = null,
                type = "new",
                easeFactor = 2.5,
                repetitions = 0,
                intervalDays = 0,
                nextReviewAt = null
            )
        }
        if (cards.isNotEmpty()) dao.insertCards(cards)
        adjustLearningPlan(deltaNewWords = cards.size)
        return imported
    }

    suspend fun deleteImportedWordSet(deckId: Long) {
        if (deckId >= 0) return
        val cards = dao.getCardsForDeck(deckId)
        dao.deleteCardsByDeckId(deckId)
        dao.deleteDeckById(deckId)
        adjustLearningPlan(deltaNewWords = -cards.count { it.repetitions == 0 })
    }

    // Synchronize offline review queue
    suspend fun syncPendingReviews(token: String) {
        val pending = dao.getPendingReviews()
        if (pending.isEmpty()) return

        for (review in pending) {
            try {
                api.reviewCard(token, ReviewCardRequest(review.cardId, review.quality))
                dao.deletePendingReview(review.id)
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 400..499) {
                    // Client error (e.g. card deleted on server), discard to prevent lockups
                    dao.deletePendingReview(review.id)
                } else {
                    // Server error (5xx), stop and retry next time
                    break
                }
            } catch (e: Exception) {
                // Connection or other errors, stop and retry next time
                break
            }
        }
    }

    // 1. Dashboard Cache & Sync
    suspend fun getDashboard(token: String, forceRefresh: Boolean = false): DashboardResponse {
        if (!forceRefresh) {
            try {
                syncPendingReviews(token)
                val response = api.getDashboard(token)
                dao.insertDashboard(
                    DashboardCache(
                        fullName = response.full_name,
                        targetGoal = response.target_goal,
                        currentLevel = response.current_level,
                        currentStreak = response.current_streak,
                        totalWordsLearned = response.total_words_learned,
                        accuracyRate = response.accuracy_rate,
                        dailyNewWordsGoal = response.daily_new_words_goal,
                        avatarUrl = response.avatar_url
                    )
                )
                return response
            } catch (e: Exception) {
                // Fallback to cache
            }
        }

        val cached = dao.getDashboard() ?: throw IOException("Không có kết nối mạng và không có dữ liệu cache.")
        return DashboardResponse(
            full_name = cached.fullName,
            target_goal = cached.targetGoal,
            current_level = cached.currentLevel,
            current_streak = cached.currentStreak,
            total_words_learned = cached.totalWordsLearned,
            accuracy_rate = cached.accuracyRate,
            daily_new_words_goal = cached.dailyNewWordsGoal,
            avatar_url = cached.avatarUrl
        )
    }

    // 2. Progress Cache & Sync
    suspend fun getProgress(token: String): ProgressResponse {
        try {
            val response = api.getProgress(token)
            dao.insertProgress(
                ProgressCache(
                    dailyActivityJson = gson.toJson(response.daily_activity),
                    retentionRate = response.retention_rate,
                    estimatedLevel = response.estimated_level,
                    levelReason = response.level_reason
                )
            )
            return response
        } catch (e: Exception) {
            // Fallback to cache
        }

        val cached = dao.getProgress() ?: throw IOException("Không có kết nối mạng và không có dữ liệu cache.")
        val type = object : TypeToken<List<DailyActivityItem>>() {}.type
        val list: List<DailyActivityItem> = gson.fromJson(cached.dailyActivityJson, type)
        return ProgressResponse(
            daily_activity = list,
            retention_rate = cached.retentionRate,
            estimated_level = cached.estimatedLevel,
            level_reason = cached.levelReason
        )
    }

    // 3. Learning Plan Cache & Sync
    suspend fun getLearningPlan(token: String): LearningPlanResponse {
        try {
            val response = api.getLearningPlan(token)
            val localDecks = dao.getDecks().filter { it.id < 0 }
            val combined = response.copy(
                new_words_available = response.new_words_available + localDecks.sumOf { it.newWordsCount },
                due_review_count = response.due_review_count + localDecks.sumOf { it.dueReviewCount }
            )
            dao.insertLearningPlan(
                LearningPlanCache(
                    dailyNewWordsGoal = combined.daily_new_words_goal,
                    dailyReviewGoal = combined.daily_review_goal,
                    newWordsAvailable = combined.new_words_available,
                    dueReviewCount = combined.due_review_count,
                    wordsLearnedToday = combined.words_learned_today,
                    wordsReviewedToday = combined.words_reviewed_today
                )
            )
            return combined
        } catch (e: Exception) {
            // Fallback to cache
        }

        val cached = dao.getLearningPlan() ?: throw IOException("Không có kết nối mạng và không có dữ liệu cache.")
        return LearningPlanResponse(
            daily_new_words_goal = cached.dailyNewWordsGoal,
            daily_review_goal = cached.dailyReviewGoal,
            new_words_available = cached.newWordsAvailable,
            due_review_count = cached.dueReviewCount,
            words_learned_today = cached.wordsLearnedToday,
            words_reviewed_today = cached.wordsReviewedToday
        )
    }

    // 4. Decks Cache & Sync
    suspend fun getLearningDecks(token: String): LearningDeckListResponse {
        try {
            val response = api.getLearningDecks(token)
            val entities = response.decks.map {
                DeckEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    totalWords = it.total_words,
                    learnedWords = it.learned_words,
                    newWordsCount = it.new_words_count,
                    dueReviewCount = it.due_review_count,
                    lastStudiedAt = it.last_studied_at,
                    isCompleted = it.is_completed,
                    isInProgress = it.is_in_progress
                )
            }
            dao.clearServerDecks()
            dao.insertDecks(entities)

            // Trigger eager preloading of all server deck cards in background
            repositoryScope.launch {
                preloadAllDecks(token, response.decks)
            }

            return getCachedLearningDeckList()
        } catch (e: Exception) {
            // Fallback to cache
        }

        return getCachedLearningDeckList()
    }

    private suspend fun preloadAllDecks(token: String, decks: List<LearningDeckSummary>) {
        for (deck in decks) {
            try {
                // Preload up to 50 cards (backend's maximum limit per call) for this deck
                val session = api.getLearningSession(token, "mixed", 50, deck.id)
                val cardEntities = session.cards.map {
                    CardEntity(
                        id = it.id,
                        deckId = it.deck_id,
                        deckTitle = it.deck_title,
                        word = it.word,
                        pronunciation = it.pronunciation,
                        meaning = it.meaning,
                        descriptionEn = it.description_en,
                        example = it.example,
                        collocation = it.collocation,
                        relatedWords = it.related_words,
                        note = it.note,
                        imageUrl = it.image_url,
                        audioUrl = it.audio_url,
                        type = it.type,
                        easeFactor = it.progress?.ease_factor ?: 2.5,
                        repetitions = it.progress?.repetitions ?: 0,
                        intervalDays = it.progress?.interval_days ?: 0,
                        nextReviewAt = it.progress?.next_review_at
                    )
                }
                dao.insertCards(cardEntities)
            } catch (e: Exception) {
                // Suppress single failure and continue preloading other decks
            }
        }
    }

    // 5. Get Learning Session Cache & Sync
    suspend fun getLearningSession(
        token: String,
        mode: String,
        limit: Int,
        deckId: Long?
    ): LearningSessionResponse {
        if (deckId != null && deckId < 0) {
            return getLocalLearningSession(mode, limit, deckId)
        }

        try {
            syncPendingReviews(token)
            val response = api.getLearningSession(token, mode, limit, deckId)

            // Cache cards locally
            val cardEntities = response.cards.map {
                CardEntity(
                    id = it.id,
                    deckId = it.deck_id,
                    deckTitle = it.deck_title,
                    word = it.word,
                    pronunciation = it.pronunciation,
                    meaning = it.meaning,
                    descriptionEn = it.description_en,
                    example = it.example,
                    collocation = it.collocation,
                    relatedWords = it.related_words,
                    note = it.note,
                    imageUrl = it.image_url,
                    audioUrl = it.audio_url,
                    type = it.type,
                    easeFactor = it.progress?.ease_factor ?: 2.5,
                    repetitions = it.progress?.repetitions ?: 0,
                    intervalDays = it.progress?.interval_days ?: 0,
                    nextReviewAt = it.progress?.next_review_at
                )
            }
            dao.insertCards(cardEntities)
            return response
        } catch (e: Exception) {
            // Fallback to local offline SM-2 queue selection
        }

        return getLocalLearningSession(mode, limit, deckId)
    }

    // 6. Review Card SM-2 Algorithm & Offline queue
    suspend fun reviewCard(token: String, request: ReviewCardRequest): ReviewProgressResponse {
        val card = dao.getCardById(request.cardId) ?: throw IOException("Không tìm thấy thẻ này trong cơ sở dữ liệu local.")
        val sm2 = calculateOfflineSm2(card.easeFactor, card.repetitions, card.intervalDays, request.quality)

        // Update offline daily statistics, deck summaries, and dashboard stats ALWAYS
        val wasNew = card.repetitions == 0
        val learnedInc = if (wasNew && request.quality > 0) 1 else 0
        val reviewedInc = if (!wasNew) 1 else 0

        // 1. Update LearningPlanCache
        val stats = dao.getLearningPlan() ?: LearningPlanCache(
            dailyNewWordsGoal = 20,
            dailyReviewGoal = 50,
            newWordsAvailable = 20,
            dueReviewCount = 0,
            wordsLearnedToday = 0,
            wordsReviewedToday = 0
        )
        val alreadyStudiedToday = stats.wordsLearnedToday + stats.wordsReviewedToday > 0
        val nextWordsLearnedToday = stats.wordsLearnedToday + learnedInc
        val nextWordsReviewedToday = stats.wordsReviewedToday + reviewedInc
        val nextDueReviewCount = maxOf(0, stats.dueReviewCount - reviewedInc)
        val nextNewWordsAvailable = maxOf(0, stats.newWordsAvailable - learnedInc)

        dao.insertLearningPlan(
            stats.copy(
                wordsLearnedToday = nextWordsLearnedToday,
                wordsReviewedToday = nextWordsReviewedToday,
                dueReviewCount = nextDueReviewCount,
                newWordsAvailable = nextNewWordsAvailable
            )
        )

        // 2. Update DeckEntity list
        val decks = dao.getDecks()
        val deck = decks.find { it.id == card.deckId }
        if (deck != null) {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val nowStr = format.format(Date())

            val nextLearnedWords = deck.learnedWords + learnedInc
            val isDeckCompleted = nextLearnedWords >= deck.totalWords

            val updatedDecks = decks.map {
                if (it.id == deck.id) {
                    it.copy(
                        learnedWords = nextLearnedWords,
                        newWordsCount = maxOf(0, deck.newWordsCount - learnedInc),
                        dueReviewCount = maxOf(0, deck.dueReviewCount - reviewedInc),
                        lastStudiedAt = nowStr,
                        isInProgress = !isDeckCompleted,
                        isCompleted = isDeckCompleted
                    )
                } else {
                    it.copy(isInProgress = false)
                }
            }
            dao.insertDecks(updatedDecks)
        }

        // 3. Update DashboardCache
        val dashboard = dao.getDashboard() ?: DashboardCache(
            fullName = "Người dùng",
            targetGoal = "mixed",
            currentLevel = "Beginner",
            currentStreak = 0,
            totalWordsLearned = 0,
            accuracyRate = 0.0f,
            dailyNewWordsGoal = 20,
            avatarUrl = null
        )

        val isFirstStudyToday = !alreadyStudiedToday && (learnedInc + reviewedInc > 0)
        val updatedStreak = if (isFirstStudyToday) dashboard.currentStreak + 1 else dashboard.currentStreak
        val isCorrect = if (request.quality >= 2) 1.0f else 0.0f
        val totalReviewsEst = dashboard.totalWordsLearned * 2
        val newAccuracy = if (totalReviewsEst == 0) {
            isCorrect
        } else {
            ((dashboard.accuracyRate * totalReviewsEst) + isCorrect) / (totalReviewsEst + 1)
        }
        val nextTotalWords = dashboard.totalWordsLearned + learnedInc

        // Determine Level based on updated stats
        val currentProgress = dao.getProgress()
        val nextRetention = if (totalReviewsEst == 0) {
            isCorrect
        } else {
            val currentRetention = currentProgress?.retentionRate ?: 0.0f
            ((currentRetention * totalReviewsEst) + isCorrect) / (totalReviewsEst + 1)
        }

        val nextLevel = when {
            nextTotalWords >= 500 && newAccuracy >= 0.75f && nextRetention >= 0.70f -> "Advanced"
            nextTotalWords >= 150 && newAccuracy >= 0.60f && nextRetention >= 0.55f -> "Intermediate"
            else -> "Beginner"
        }

        dao.insertDashboard(
            dashboard.copy(
                totalWordsLearned = nextTotalWords,
                currentStreak = updatedStreak,
                accuracyRate = newAccuracy,
                currentLevel = nextLevel
            )
        )

        // 4. Update ProgressCache (Daily activity chart & Level details)
        val progress = dao.getProgress() ?: ProgressCache(
            dailyActivityJson = "[]",
            retentionRate = 0.0f,
            estimatedLevel = "Beginner",
            levelReason = "Bắt đầu học để hệ thống ước lượng level chính xác hơn."
        )

        val type = object : TypeToken<List<DailyActivityItem>>() {}.type
        val list: MutableList<DailyActivityItem> = try {
            gson.fromJson<List<DailyActivityItem>>(progress.dailyActivityJson, type).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val index = list.indexOfFirst { it.date == todayStr }
        if (index != -1) {
            val item = list[index]
            list[index] = item.copy(
                words_learned = item.words_learned + learnedInc,
                words_reviewed = item.words_reviewed + reviewedInc
            )
        } else {
            list.add(DailyActivityItem(todayStr, learnedInc, reviewedInc))
        }

        val progressRetention = nextRetention
        val progressLevel = nextLevel
        val progressReason = "$nextTotalWords từ đã học, đúng ${Math.round(newAccuracy * 100)}%, ghi nhớ ${Math.round(progressRetention * 100)}% trong 30 ngày gần nhất."

        dao.insertProgress(
            progress.copy(
                dailyActivityJson = gson.toJson(list),
                retentionRate = progressRetention,
                estimatedLevel = progressLevel,
                levelReason = progressReason
            )
        )

        // 5. Update NotificationSummaryCache
        val notif = dao.getNotificationSummary() ?: NotificationSummaryCache(
            email = "",
            fullName = "Người dùng",
            dailyNewWordsGoal = 20,
            dailyReviewGoal = 50,
            newWordsAvailable = 20,
            dueReviewCount = 0,
            wordsLearnedToday = 0,
            wordsReviewedToday = 0,
            pushTitle = "MinLish nhắc học",
            pushBody = "Mở MinLish để giữ nhịp học hằng ngày."
        )

        val newDueReviewCount = maxOf(0, notif.dueReviewCount - reviewedInc)
        val newWordsAvailable = maxOf(0, notif.newWordsAvailable - learnedInc)
        val newWordsLearnedToday = notif.wordsLearnedToday + learnedInc
        val newWordsReviewedToday = notif.wordsReviewedToday + reviewedInc

        val newPushBody = when {
            newDueReviewCount > 0 -> "Bạn có $newDueReviewCount thẻ cần ôn hôm nay."
            newWordsAvailable > 0 -> "Bạn còn $newWordsAvailable từ mới có thể học hôm nay."
            else -> "Mở MinLish để giữ nhịp học hằng ngày."
        }

        dao.insertNotificationSummary(
            notif.copy(
                dueReviewCount = newDueReviewCount,
                newWordsAvailable = newWordsAvailable,
                wordsLearnedToday = newWordsLearnedToday,
                wordsReviewedToday = newWordsReviewedToday,
                pushBody = newPushBody
            )
        )

        if (card.deckId < 0 || card.id < 0) {
            dao.insertCards(
                listOf(
                    card.copy(
                        easeFactor = sm2.easeFactor,
                        repetitions = sm2.repetitions,
                        intervalDays = sm2.intervalDays,
                        nextReviewAt = sm2.nextReviewAt
                    )
                )
            )
            return ReviewProgressResponse(
                card_id = request.cardId,
                quality = request.quality,
                ease_factor = sm2.easeFactor,
                repetitions = sm2.repetitions,
                interval_days = sm2.intervalDays,
                next_review_at = sm2.nextReviewAt
            )
        }

        // Now attempt network sync
        try {
            val response = api.reviewCard(token, request)
            // Save updated card progress to local Room DB with progress returned by server
            dao.insertCards(
                listOf(
                    card.copy(
                        easeFactor = response.ease_factor,
                        repetitions = response.repetitions,
                        intervalDays = response.interval_days,
                        nextReviewAt = response.next_review_at
                    )
                )
            )
            // Clear any pending sync for this card if it exists
            dao.deletePendingReviewByCardId(request.cardId)
            return response
        } catch (e: Exception) {
            // Save local offline calculation progress on network failure
            val updatedCard = card.copy(
                easeFactor = sm2.easeFactor,
                repetitions = sm2.repetitions,
                intervalDays = sm2.intervalDays,
                nextReviewAt = sm2.nextReviewAt
            )
            dao.insertCards(listOf(updatedCard))

            // Save review event in sync queue
            dao.deletePendingReviewByCardId(request.cardId)
            dao.insertPendingReview(
                PendingReviewEntity(
                    cardId = request.cardId,
                    quality = request.quality
                )
            )

            return ReviewProgressResponse(
                card_id = request.cardId,
                quality = request.quality,
                ease_factor = sm2.easeFactor,
                repetitions = sm2.repetitions,
                interval_days = sm2.intervalDays,
                next_review_at = sm2.nextReviewAt
            )
        }
    }

    private fun calculateOfflineSm2(
        easeFactor: Double,
        repetitions: Int,
        intervalDays: Int,
        quality: Int
    ): Sm2Result {
        var nextEase = easeFactor
        var nextRepetitions = repetitions
        var nextInterval = intervalDays
        val calendar = Calendar.getInstance()

        when (quality) {
            0 -> { // AGAIN
                nextEase = maxOf(1.3, easeFactor - 0.2)
                nextRepetitions = 0
                nextInterval = 0
                calendar.add(Calendar.MINUTE, 5)
            }
            1 -> { // HARD
                nextEase = maxOf(1.3, easeFactor - 0.15)
                nextRepetitions = repetitions + 1
                nextInterval = maxOf(1, Math.round(maxOf(intervalDays, 1) * 1.2).toInt())
                calendar.add(Calendar.DAY_OF_YEAR, nextInterval)
            }
            2 -> { // GOOD
                nextRepetitions = repetitions + 1
                nextInterval = when (repetitions) {
                    0 -> 1
                    1 -> 3
                    else -> maxOf(1, Math.round(intervalDays * easeFactor).toInt())
                }
                calendar.add(Calendar.DAY_OF_YEAR, nextInterval)
            }
            3 -> { // EASY
                nextEase = easeFactor + 0.15
                nextRepetitions = repetitions + 1
                nextInterval = when (repetitions) {
                    0 -> 3
                    1 -> 7
                    else -> maxOf(1, Math.round(intervalDays * easeFactor * 1.3).toInt())
                }
                calendar.add(Calendar.DAY_OF_YEAR, nextInterval)
            }
        }

        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val nextReviewAtStr = format.format(calendar.time)

        return Sm2Result(nextEase, nextRepetitions, nextInterval, nextReviewAtStr)
    }

    private data class Sm2Result(
        val easeFactor: Double,
        val repetitions: Int,
        val intervalDays: Int,
        val nextReviewAt: String
    )


    private suspend fun adjustLearningPlan(deltaNewWords: Int = 0, deltaDueReviews: Int = 0) {
        if (deltaNewWords == 0 && deltaDueReviews == 0) return
        val current = dao.getLearningPlan() ?: LearningPlanCache(
            dailyNewWordsGoal = 20,
            dailyReviewGoal = 50,
            newWordsAvailable = 0,
            dueReviewCount = 0,
            wordsLearnedToday = 0,
            wordsReviewedToday = 0
        )
        dao.insertLearningPlan(
            current.copy(
                newWordsAvailable = maxOf(0, current.newWordsAvailable + deltaNewWords),
                dueReviewCount = maxOf(0, current.dueReviewCount + deltaDueReviews)
            )
        )
    }

    private fun newLocalId(): Long {
        val bits = UUID.randomUUID().mostSignificantBits xor UUID.randomUUID().leastSignificantBits
        val positive = bits and Long.MAX_VALUE
        return -maxOf(1L, positive)
    }

    private suspend fun getCachedLearningDeckList(): LearningDeckListResponse {
        val decks = dao.getDecks().map { it.toLearningDeckSummary() }
        return LearningDeckListResponse(
            continue_deck = decks.firstOrNull { it.is_in_progress },
            decks = decks
        )
    }

    private fun DeckEntity.toLearningDeckSummary(): LearningDeckSummary {
        return LearningDeckSummary(
            id = id,
            title = title,
            description = description,
            total_words = totalWords,
            learned_words = learnedWords,
            new_words_count = newWordsCount,
            due_review_count = dueReviewCount,
            last_studied_at = lastStudiedAt,
            is_completed = isCompleted,
            is_in_progress = isInProgress
        )
    }

    private fun CardEntity.toLearningCard(cardType: String = type): LearningCard {
        return LearningCard(
            id = id,
            deck_id = deckId,
            deck_title = deckTitle,
            word = word,
            pronunciation = pronunciation,
            meaning = meaning,
            description_en = descriptionEn,
            example = example,
            collocation = collocation,
            related_words = relatedWords,
            note = note,
            image_url = imageUrl,
            audio_url = audioUrl,
            type = cardType,
            progress = LearningCardProgress(
                ease_factor = easeFactor,
                repetitions = repetitions,
                interval_days = intervalDays,
                next_review_at = nextReviewAt
            )
        )
    }

    private suspend fun getLocalLearningSession(mode: String, limit: Int, deckId: Long?): LearningSessionResponse {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val nowStr = format.format(Date())

        var localCards = when (mode) {
            "review" -> dao.getDueReviewCardsOffline(nowStr).map { it to "review" }
            "new" -> dao.getNewCardsOffline().map { it to "new" }
            else -> {
                val due = dao.getDueReviewCardsOffline(nowStr).map { it to "review" }
                val newCards = dao.getNewCardsOffline().map { it to "new" }
                due + newCards
            }
        }

        if (deckId != null) {
            localCards = localCards.filter { it.first.deckId == deckId }
        }

        val cards = localCards.take(limit).map { (card, cardType) -> card.toLearningCard(cardType) }
        return LearningSessionResponse(
            mode = mode,
            count = cards.size,
            cards = cards
        )
    }

    // 7. Profile Cache & Sync
    suspend fun getProfile(token: String): ProfileResponse {
        try {
            val response = api.getProfile(token)
            dao.insertProfile(
                ProfileCache(
                    email = response.email.orEmpty(),
                    fullName = response.full_name.orEmpty(),
                    targetGoal = response.target_goal.orEmpty(),
                    currentLevel = response.current_level ?: "A1",
                    avatarUrl = response.avatar_url
                )
            )
            return response
        } catch (e: Exception) {
            // Fallback
        }

        val cached = dao.getProfile() ?: throw IOException("Không thể kết nối mạng và không có hồ sơ cache.")
        return ProfileResponse(
            id = 0,
            email = cached.email,
            full_name = cached.fullName,
            target_goal = cached.targetGoal,
            current_level = cached.currentLevel,
            avatar_url = cached.avatarUrl,
            avatarBase64 = null,
            avatarMimeType = null
        )
    }

    suspend fun updateProfile(token: String, request: ProfileUpdateRequest): Response<ProfileResponse> {
        val response = api.updateProfile(token, request)
        if (response.isSuccessful) {
            response.body()?.let { body ->
                dao.insertProfile(
                    ProfileCache(
                        email = body.email.orEmpty(),
                        fullName = body.full_name.orEmpty(),
                        targetGoal = body.target_goal.orEmpty(),
                        currentLevel = body.current_level ?: "A1",
                        avatarUrl = body.avatar_url
                    )
                )
            }
        }
        return response
    }

    // 8. User Settings Cache & Sync
    suspend fun getSettings(token: String): UserSettingsResponse {
        try {
            val response = api.getSettings(token)
            dao.insertUserSettings(
                UserSettingsCache(
                    theme = response.theme ?: "system",
                    dailyReminderTime = response.daily_reminder_time,
                    notificationsEnabled = response.notifications_enabled ?: 1,
                    emailNotificationsEnabled = response.email_notifications_enabled ?: 1,
                    dailyNewWordsGoal = response.daily_new_words_goal ?: 20,
                    dailyReviewGoal = response.daily_review_goal ?: 50
                )
            )
            return response
        } catch (e: Exception) {
            // Fallback
        }

        val cached = dao.getUserSettings() ?: throw IOException("Không thể kết nối mạng và không có cài đặt cache.")
        return UserSettingsResponse(
            theme = cached.theme,
            daily_reminder_time = cached.dailyReminderTime,
            notifications_enabled = cached.notificationsEnabled,
            email_notifications_enabled = cached.emailNotificationsEnabled,
            daily_new_words_goal = cached.dailyNewWordsGoal,
            daily_review_goal = cached.dailyReviewGoal
        )
    }

    suspend fun updateSettings(token: String, request: UserSettingsRequest): UserSettingsResponse {
        try {
            val response = api.updateSettings(token, request)
            dao.insertUserSettings(
                UserSettingsCache(
                    theme = response.theme ?: "system",
                    dailyReminderTime = response.daily_reminder_time,
                    notificationsEnabled = response.notifications_enabled ?: 1,
                    emailNotificationsEnabled = response.email_notifications_enabled ?: 1,
                    dailyNewWordsGoal = response.daily_new_words_goal ?: 20,
                    dailyReviewGoal = response.daily_review_goal ?: 50
                )
            )
            return response
        } catch (e: Exception) {
            // Save local cache even if offline
            val current = dao.getUserSettings()
            val localSettings = UserSettingsCache(
                theme = request.theme ?: current?.theme ?: "system",
                dailyReminderTime = request.dailyReminderTime ?: current?.dailyReminderTime ?: "20:00:00",
                notificationsEnabled = if (request.notificationsEnabled == true) 1 else 0,
                emailNotificationsEnabled = if (request.emailNotificationsEnabled == true) 1 else 0,
                dailyNewWordsGoal = request.dailyNewWordsGoal ?: current?.dailyNewWordsGoal ?: 20,
                dailyReviewGoal = request.dailyReviewGoal ?: current?.dailyReviewGoal ?: 50
            )
            dao.insertUserSettings(localSettings)
            return UserSettingsResponse(
                theme = localSettings.theme,
                daily_reminder_time = localSettings.dailyReminderTime,
                notifications_enabled = localSettings.notificationsEnabled,
                email_notifications_enabled = localSettings.emailNotificationsEnabled,
                daily_new_words_goal = localSettings.dailyNewWordsGoal,
                daily_review_goal = localSettings.dailyReviewGoal
            )
        }
    }

    // 9. Notifications Cache & Sync
    suspend fun getNotificationSummary(token: String): NotificationSummaryResponse {
        try {
            val response = api.getNotificationSummary(token)
            dao.insertNotificationSummary(
                NotificationSummaryCache(
                    email = response.email,
                    fullName = response.full_name,
                    dailyNewWordsGoal = response.daily_new_words_goal,
                    dailyReviewGoal = response.daily_review_goal,
                    newWordsAvailable = response.new_words_available,
                    dueReviewCount = response.due_review_count,
                    wordsLearnedToday = response.words_learned_today,
                    wordsReviewedToday = response.words_reviewed_today,
                    pushTitle = response.push_title,
                    pushBody = response.push_body
                )
            )
            return response
        } catch (e: Exception) {
            // Fallback
        }

        val cached = dao.getNotificationSummary() ?: throw IOException("Không thể kết nối mạng và không có thông báo cache.")
        return NotificationSummaryResponse(
            email = cached.email,
            full_name = cached.fullName,
            daily_new_words_goal = cached.dailyNewWordsGoal,
            daily_review_goal = cached.dailyReviewGoal,
            new_words_available = cached.newWordsAvailable,
            due_review_count = cached.dueReviewCount,
            words_learned_today = cached.wordsLearnedToday,
            words_reviewed_today = cached.wordsReviewedToday,
            push_title = cached.pushTitle,
            push_body = cached.pushBody
        )
    }

    // Direct proxy endpoints
    suspend fun sendStudyReminderEmail(token: String): ApiMessageResponse {
        return api.sendStudyReminderEmail(token)
    }
}
