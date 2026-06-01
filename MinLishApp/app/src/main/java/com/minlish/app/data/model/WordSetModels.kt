package com.minlish.app.data.model

data class WordItem(
    val id: String,
    val term: String,
    val meaning: String,
    val example: String = "",
    val note: String = "",
    val partOfSpeech: String = "",
    val tags: List<String> = emptyList()
)

data class WordSet(
    val id: String,
    val name: String,
    val sourceFileName: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val words: List<WordItem>,
    val deckId: Long? = null
)

data class ImportSummary(
    val fileName: String,
    val validCount: Int,
    val skippedCount: Int,
    val duplicateCount: Int,
    val message: String
)
