package com.minlish.app.feature.practice

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.UserSession
import com.minlish.app.data.model.LearningCard
import com.minlish.app.data.model.LearningDeckSummary
import com.minlish.app.data.model.PracticeResultRequest
import com.minlish.app.data.repository.MinLishRepository
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PracticeMode(val title: String, val description: String) {
    QUIZ("Trắc nghiệm", "Chọn từ đúng để điền vào câu trống."),
    MATCHING("Nối từ", "Chạm từ tiếng Anh rồi chạm nghĩa tiếng Việt."),
    LISTENING("Nghe và điền từ", "Nghe phát âm rồi nhập chính xác từ vừa nghe.")
}

data class QuizQuestion(
    val card: LearningCard,
    val prompt: String,
    val options: List<String>,
    val answer: String
)

data class MatchTile(
    val id: Long,
    val text: String
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MinLishRepository.getInstance(application)

    var decks by mutableStateOf<List<LearningDeckSummary>>(emptyList())
        private set

    var selectedDeck by mutableStateOf<LearningDeckSummary?>(null)
        private set

    var cards by mutableStateOf<List<LearningCard>>(emptyList())
        private set

    var selectedMode by mutableStateOf<PracticeMode?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    var message by mutableStateOf("")
        private set

    var quizQuestions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set

    var quizIndex by mutableStateOf(0)
        private set

    var quizScore by mutableStateOf(0)
        private set

    var selectedQuizAnswer by mutableStateOf<String?>(null)
        private set

    var quizAnswerCorrect by mutableStateOf<Boolean?>(null)
        private set

    var matchWords by mutableStateOf<List<MatchTile>>(emptyList())
        private set

    var matchMeanings by mutableStateOf<List<MatchTile>>(emptyList())
        private set

    var selectedMatchWordId by mutableStateOf<Long?>(null)
        private set

    var matchedIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    var correctMatchIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    var wrongMatchIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    var matchAttempts by mutableStateOf(0)
        private set

    var matchMistakes by mutableStateOf(0)
        private set

    var listenCards by mutableStateOf<List<LearningCard>>(emptyList())
        private set

    var listenIndex by mutableStateOf(0)
        private set

    var listenInput by mutableStateOf("")
        private set

    var listenScore by mutableStateOf(0)
        private set

    var listenAnswered by mutableStateOf(false)
        private set

    var listenFeedback by mutableStateOf("")
        private set

    val currentQuizQuestion: QuizQuestion?
        get() = quizQuestions.getOrNull(quizIndex)

    val currentListenCard: LearningCard?
        get() = listenCards.getOrNull(listenIndex)

    val listenHint: String
        get() = buildListenHint()

    fun loadDecks() {
        val token = UserSession.token ?: run {
            error = "Bạn cần đăng nhập để luyện tập."
            return
        }

        loading = true
        error = ""
        message = ""

        viewModelScope.launch {
            try {
                val response = repository.getPracticeDecks(token)
                decks = response.decks
                if (decks.isEmpty()) {
                    message = "Bạn chưa có chủ đề đã học. Hãy học ít nhất một từ trước khi luyện tập."
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Không tải được danh sách chủ đề luyện tập."
            } finally {
                loading = false
            }
        }
    }

    fun selectDeck(deck: LearningDeckSummary) {
        val token = UserSession.token ?: run {
            error = "Phiên đăng nhập không hợp lệ."
            return
        }

        selectedDeck = deck
        selectedMode = null
        resetActivities()
        loading = true
        error = ""
        message = ""

        viewModelScope.launch {
            try {
                val response = repository.getPracticeCards(token, deck.id)
                cards = response.cards.distinctBy { it.id }
                if (cards.isEmpty()) {
                    message = "Chủ đề này chưa có từ đã học để luyện tập."
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Không tải được từ vựng của chủ đề này."
            } finally {
                loading = false
            }
        }
    }

    fun clearDeckSelection() {
        selectedDeck = null
        cards = emptyList()
        selectedMode = null
        resetActivities()
        error = ""
        message = ""
    }

    fun clearMode() {
        selectedMode = null
        resetActivities()
        error = ""
        message = ""
    }

    fun resetState() {
        decks = emptyList()
        selectedDeck = null
        cards = emptyList()
        selectedMode = null
        loading = false
        error = ""
        message = ""
        resetActivities()
    }

    fun startMode(mode: PracticeMode) {
        selectedMode = mode
        error = ""
        message = ""

        when (mode) {
            PracticeMode.QUIZ -> startQuiz()
            PracticeMode.MATCHING -> startMatching()
            PracticeMode.LISTENING -> startListening()
        }
    }

    fun submitQuizAnswer(answer: String) {
        val question = currentQuizQuestion ?: return
        if (selectedQuizAnswer != null) return

        val correct = answer == question.answer
        selectedQuizAnswer = answer
        quizAnswerCorrect = correct
        if (correct) {
            quizScore += 1
            message = "Chính xác."
        } else {
            message = "Đáp án đúng là ${question.answer}."
        }
    }

    fun nextQuizQuestion() {
        if (quizIndex < quizQuestions.lastIndex) {
            quizIndex += 1
            selectedQuizAnswer = null
            quizAnswerCorrect = null
            message = ""
        } else {
            val summary = "Hoàn thành trắc nghiệm: $quizScore/${quizQuestions.size} câu đúng."
            val total = quizQuestions.size
            if (total > 0) {
                submitPracticeResult("quiz", quizScore, total)
            }
            selectedMode = null
            resetActivities()
            message = summary
        }
    }

    fun selectMatchWord(id: Long) {
        if (id in matchedIds || id in correctMatchIds) return
        selectedMatchWordId = id
        message = ""
    }

    fun selectMatchMeaning(id: Long) {
        val wordId = selectedMatchWordId ?: return
        if (id in matchedIds || id in correctMatchIds) return

        matchAttempts += 1
        if (wordId == id) {
            val nextMatchedCount = matchedIds.size + 1
            correctMatchIds = setOf(id)
            selectedMatchWordId = null
            message = "Đúng rồi."
            viewModelScope.launch {
                delay(420)
                matchedIds = matchedIds + id
                correctMatchIds = emptySet()
                if (nextMatchedCount == matchWords.size) {
                    message = "Hoàn thành nối từ với $matchMistakes lần sai."
                }
            }
        } else {
            matchMistakes += 1
            wrongMatchIds = setOf(wordId, id)
            message = "Chưa đúng, thử lại nhé."
            viewModelScope.launch {
                delay(550)
                wrongMatchIds = emptySet()
            }
        }
    }

    fun updateListenInput(value: String) {
        if (listenAnswered) return
        listenInput = value
        listenFeedback = ""
    }

    fun appendListenInput(value: String) {
        if (listenAnswered) return
        listenInput += value
        listenFeedback = ""
    }

    fun deleteListenInput() {
        if (listenAnswered || listenInput.isEmpty()) return
        listenInput = listenInput.dropLast(1)
        listenFeedback = ""
    }

    fun clearListenInput() {
        if (listenAnswered) return
        listenInput = ""
        listenFeedback = ""
    }

    fun checkListenAnswer() {
        val expected = currentListenCard?.word?.practiceAnswer().orEmpty()
        if (expected.isBlank()) return

        if (normalize(listenInput) == normalize(expected)) {
            listenAnswered = true
            listenScore += 1
            listenFeedback = "Chính xác."
        } else {
            listenFeedback = buildListenHint().ifBlank {
                val nextIndex = listenInput.length.coerceAtMost(expected.lastIndex)
                "Gợi ý: chữ thứ ${nextIndex + 1} là \"${expected[nextIndex]}\"."
            }
        }
    }

    fun nextListenQuestion() {
        if (listenIndex < listenCards.lastIndex) {
            listenIndex += 1
            listenInput = ""
            listenAnswered = false
            listenFeedback = ""
            message = ""
        } else {
            val summary = "Hoàn thành nghe và điền từ: $listenScore/${listenCards.size} câu đúng."
            val total = listenCards.size
            if (total > 0) {
                submitPracticeResult("listening", listenScore, total)
            }
            selectedMode = null
            resetActivities()
            message = summary
        }
    }

    fun finishMatchingPractice() {
        val total = matchWords.size
        val correct = total
        val totalAttempts = total + matchMistakes
        if (total > 0) {
            submitPracticeResult("matching", correct, totalAttempts)
        }
        val summary = "Hoàn thành nối từ: $total/$total cặp, $matchMistakes lần sai."
        selectedMode = null
        resetActivities()
        message = summary
    }

    private fun startQuiz() {
        val quizCards = cards.distinctBy { normalize(it.word.practiceAnswer()) }
        if (quizCards.size < 4) {
            quizQuestions = emptyList()
            error = "Cần ít nhất 4 từ đã học khác nhau trong chủ đề để tạo câu trắc nghiệm."
            return
        }

        val source = quizCards.shuffled().take(10)
        quizQuestions = source.map { card ->
            val answer = card.word.practiceAnswer()
            val distractors = quizCards
                .filter { normalize(it.word.practiceAnswer()) != normalize(answer) }
                .shuffled()
                .take(3)
                .map { it.word.practiceAnswer() }
            QuizQuestion(
                card = card,
                prompt = card.quizPrompt(),
                options = (distractors + answer).shuffled(),
                answer = answer
            )
        }
        quizIndex = 0
        quizScore = 0
        selectedQuizAnswer = null
        quizAnswerCorrect = null
    }

    private fun startMatching() {
        val matchCards = uniqueMatchingCards()
        if (matchCards.size < 2) {
            error = "Cần ít nhất 2 từ đã học để luyện nối từ."
            return
        }

        val sample = matchCards.take(6)
        matchWords = sample.map { MatchTile(it.id, it.word.practiceAnswer()) }.shuffled()
        matchMeanings = sample.map { MatchTile(it.id, cleanMatchMeaning(it.meaning)) }.shuffled()
        selectedMatchWordId = null
        matchedIds = emptySet()
        correctMatchIds = emptySet()
        wrongMatchIds = emptySet()
        matchAttempts = 0
        matchMistakes = 0
    }

    private fun startListening() {
        if (cards.isEmpty()) {
            error = "Chủ đề này chưa có từ để luyện nghe."
            return
        }

        listenCards = cards.shuffled().take(10)
        listenIndex = 0
        listenInput = ""
        listenScore = 0
        listenAnswered = false
        listenFeedback = ""
    }

    private fun resetActivities() {
        quizQuestions = emptyList()
        quizIndex = 0
        quizScore = 0
        selectedQuizAnswer = null
        quizAnswerCorrect = null
        matchWords = emptyList()
        matchMeanings = emptyList()
        selectedMatchWordId = null
        matchedIds = emptySet()
        correctMatchIds = emptySet()
        wrongMatchIds = emptySet()
        matchAttempts = 0
        matchMistakes = 0
        listenCards = emptyList()
        listenIndex = 0
        listenInput = ""
        listenScore = 0
        listenAnswered = false
        listenFeedback = ""
    }

    private fun LearningCard.quizPrompt(): String {
        val answer = word.practiceAnswer()
        val exampleText = example.orEmpty()
        if (exampleText.isNotBlank()) {
            val replaced = exampleText.replace(
                Regex("\\b${Regex.escape(answer)}\\b", RegexOption.IGNORE_CASE),
                "_____"
            )
            if (replaced != exampleText) return replaced
        }
        return "A word that means \"$meaning\" is _____."
    }

    private fun buildListenHint(): String {
        val expected = currentListenCard?.word?.practiceAnswer().orEmpty()
        if (expected.isBlank() || listenInput.isBlank() || listenAnswered) return ""

        val max = minOf(listenInput.length, expected.length)
        for (index in 0 until max) {
            if (!listenInput[index].equals(expected[index], ignoreCase = true)) {
                return "Gợi ý: chữ thứ ${index + 1} là \"${expected[index]}\"."
            }
        }

        return if (listenInput.length > expected.length) {
            "Từ này ngắn hơn phần bạn đã nhập. Hãy xóa bớt ký tự cuối."
        } else {
            ""
        }
    }

    private fun uniqueMatchingCards(): List<LearningCard> {
        val usedWords = mutableSetOf<String>()
        val usedMeanings = mutableSetOf<String>()
        val result = mutableListOf<LearningCard>()

        cards.shuffled().forEach { card ->
            val wordKey = normalize(card.word.practiceAnswer())
            val meaningKey = normalize(cleanMatchMeaning(card.meaning))
            if (wordKey.isNotBlank() && meaningKey.isNotBlank() && usedWords.add(wordKey) && usedMeanings.add(meaningKey)) {
                result.add(card)
            }
        }

        return result
    }

    private fun cleanMatchMeaning(value: String): String {
        fun stripPartOfSpeech(text: String): String {
            return text.replace(Regex("^\\s*(\\([^)]*\\)\\s*:?\\s*)+"), "").trim()
        }

        val stripped = stripPartOfSpeech(value)
        val firstMeaning = stripped
            .split(";", "\n")
            .firstOrNull()
            .orEmpty()
            .split(",")
            .firstOrNull()
            .orEmpty()
            .let(::stripPartOfSpeech)

        return firstMeaning.ifBlank { stripped.ifBlank { value.trim() } }
    }

    private fun String.practiceAnswer(): String = trim().replace("_", " ")

    private fun normalize(value: String): String {
        return value.trim()
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.US)
    }

    private fun submitPracticeResult(mode: String, correctCount: Int, totalCount: Int) {
        val token = UserSession.token ?: return
        viewModelScope.launch {
            try {
                repository.recordPracticeResult(token, PracticeResultRequest(mode, correctCount, totalCount))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
