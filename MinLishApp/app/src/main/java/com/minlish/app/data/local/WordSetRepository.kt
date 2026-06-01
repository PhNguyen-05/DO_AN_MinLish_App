package com.minlish.app.data.local

import android.content.Context
import com.minlish.app.data.model.WordItem
import com.minlish.app.data.model.WordSet
import org.json.JSONArray
import org.json.JSONObject

class WordSetRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getWordSets(): List<WordSet> {
        val raw = prefs.getString(KEY_WORD_SETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toWordSet())
                }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun saveWordSet(wordSet: WordSet) {
        val next = getWordSets()
            .filterNot { it.id == wordSet.id }
            .plus(wordSet)
            .sortedByDescending { it.updatedAt }
        prefs.edit().putString(KEY_WORD_SETS, JSONArray(next.map { it.toJson() }).toString()).apply()
    }

    fun deleteWordSet(id: String) {
        val next = getWordSets().filterNot { it.id == id }
        prefs.edit().putString(KEY_WORD_SETS, JSONArray(next.map { it.toJson() }).toString()).apply()
    }

    private fun WordSet.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("sourceFileName", sourceFileName)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
            .put("deckId", deckId ?: JSONObject.NULL)
            .put("words", JSONArray(words.map { it.toJson() }))
    }

    private fun WordItem.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("term", term)
            .put("meaning", meaning)
            .put("example", example)
            .put("note", note)
            .put("partOfSpeech", partOfSpeech)
            .put("tags", JSONArray(tags))
    }

    private fun JSONObject.toWordSet(): WordSet {
        val wordsArray = optJSONArray("words") ?: JSONArray()
        val words = buildList {
            for (index in 0 until wordsArray.length()) {
                add(wordsArray.getJSONObject(index).toWordItem())
            }
        }
        return WordSet(
            id = optString("id"),
            name = optString("name"),
            sourceFileName = optString("sourceFileName"),
            createdAt = optLong("createdAt"),
            updatedAt = optLong("updatedAt"),
            words = words,
            deckId = optLongOrNull("deckId")
        )
    }

    private fun JSONObject.toWordItem(): WordItem {
        val tagsArray = optJSONArray("tags") ?: JSONArray()
        val tags = buildList {
            for (index in 0 until tagsArray.length()) {
                add(tagsArray.optString(index))
            }
        }.filter { it.isNotBlank() }
        return WordItem(
            id = optString("id"),
            term = optString("term"),
            meaning = optString("meaning"),
            example = optString("example"),
            note = optString("note"),
            partOfSpeech = optString("partOfSpeech"),
            tags = tags
        )
    }

    private fun JSONObject.optLongOrNull(name: String): Long? {
        return if (has(name) && !isNull(name)) optLong(name) else null
    }

    private companion object {
        const val PREF_NAME = "minlish_word_sets"
        const val KEY_WORD_SETS = "word_sets"
    }
}
