package com.minlish.app.feature.importexport

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.local.WordFileParser
import com.minlish.app.data.local.WordSetRepository
import com.minlish.app.data.model.ImportSummary
import com.minlish.app.data.model.WordSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ImportExportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WordSetRepository(application)

    var wordSets by mutableStateOf<List<WordSet>>(repository.getWordSets())
        private set

    var selectedWordSetId by mutableStateOf<String?>(wordSets.firstOrNull()?.id)
        private set

    var loading by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var error by mutableStateOf("")
        private set

    var lastImportSummary by mutableStateOf<ImportSummary?>(null)
        private set

    val selectedWordSet: WordSet?
        get() = wordSets.firstOrNull { it.id == selectedWordSetId }

    fun selectWordSet(id: String) {
        selectedWordSetId = id
        message = ""
        error = ""
    }

    fun importFromUri(uri: Uri) {
        loading = true
        message = ""
        error = ""
        lastImportSummary = null
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val fileName = context.displayName(uri)
                val parsed = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        if (fileName.endsWith(".xlsx", ignoreCase = true)) {
                            WordFileParser.parseXlsx(input)
                        } else {
                            WordFileParser.parseCsv(input.readBytes().toString(Charsets.UTF_8))
                        }
                    } ?: error("Không đọc được file đã chọn.")
                }

                if (parsed.words.isEmpty()) {
                    error = "Không tìm thấy dòng hợp lệ. File cần có ít nhất cột word và meaning."
                    return@launch
                }

                val now = System.currentTimeMillis()
                val wordSet = WordSet(
                    id = UUID.randomUUID().toString(),
                    name = fileName.substringBeforeLast('.').ifBlank { "Bộ từ mới" },
                    sourceFileName = fileName,
                    createdAt = now,
                    updatedAt = now,
                    words = parsed.words
                )
                repository.saveWordSet(wordSet)
                refresh(selectedId = wordSet.id)

                lastImportSummary = ImportSummary(
                    fileName = fileName,
                    validCount = parsed.words.size,
                    skippedCount = parsed.skippedCount,
                    duplicateCount = parsed.duplicateCount,
                    message = "Đã nhập ${parsed.words.size} từ vào bộ \"${wordSet.name}\"."
                )
                message = lastImportSummary?.message.orEmpty()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Import thất bại. Vui lòng kiểm tra định dạng file."
            } finally {
                loading = false
            }
        }
    }

    fun exportSelectedToUri(uri: Uri) {
        val wordSet = selectedWordSet ?: run {
            error = "Chưa có bộ từ để export."
            return
        }
        loading = true
        message = ""
        error = ""
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val csv = WordFileParser.toCsv(wordSet.name, wordSet.words)
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Không ghi được file export.")
                }
                message = "Đã export ${wordSet.words.size} từ từ bộ \"${wordSet.name}\"."
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Export thất bại."
            } finally {
                loading = false
            }
        }
    }

    fun deleteSelectedWordSet() {
        val id = selectedWordSetId ?: return
        repository.deleteWordSet(id)
        refresh()
        message = "Đã xóa bộ từ."
        error = ""
        lastImportSummary = null
    }

    private fun refresh(selectedId: String? = null) {
        wordSets = repository.getWordSets()
        selectedWordSetId = selectedId ?: wordSets.firstOrNull()?.id
    }

    private fun Application.displayName(uri: Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "word_set.csv"
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else fallback
        } ?: fallback
    }
}
