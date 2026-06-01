package com.minlish.app.data.repository

import android.content.Context
import com.minlish.app.data.local.WordSetRepository
import com.minlish.app.data.model.WordSet

/**
 * Một điểm truy cập cho nhập/xuất bộ từ (đồ án MVVM – tầng data):
 * - Danh mục bộ từ: [WordSetRepository] (SharedPreferences)
 * - Dữ liệu học: [MinLishRepository] (Room – deck & flashcard)
 */
class WordImportExportRepository(context: Context) {

    private val catalog = WordSetRepository(context.applicationContext)
    private val learning = MinLishRepository.getInstance(context.applicationContext)

    fun getWordSets(): List<WordSet> = catalog.getWordSets()

    suspend fun importWordSet(wordSet: WordSet): WordSet {
        val linked = learning.importWordSet(wordSet)
        catalog.saveWordSet(linked)
        return linked
    }

    suspend fun deleteWordSet(id: String) {
        catalog.getWordSets().find { it.id == id }?.deckId?.let { deckId ->
            learning.deleteImportedWordSet(deckId)
        }
        catalog.deleteWordSet(id)
    }

    /** Bộ cũ chỉ lưu catalog, chưa có deck Room → tạo deck học tương ứng. */
    suspend fun syncLegacySetsToLearning() {
        catalog.getWordSets()
            .filter { it.deckId == null && it.words.isNotEmpty() }
            .forEach { importWordSet(it) }
    }
}
