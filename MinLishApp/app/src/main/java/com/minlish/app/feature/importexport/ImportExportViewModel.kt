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
import com.minlish.app.data.local.WordPdfExporter
import com.minlish.app.data.model.WordSet
import com.minlish.app.data.repository.WordImportExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ImportExportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WordImportExportRepository(application)
    private val exportPrefs = application.getSharedPreferences(PREF_EXPORT, Application.MODE_PRIVATE)

    var wordSets by mutableStateOf(repository.getWordSets())
        private set

    var selectedWordSetId by mutableStateOf<String?>(wordSets.firstOrNull()?.id)
        private set

    var loading by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var error by mutableStateOf("")
        private set

    var lastExportFileName by mutableStateOf("")
        private set

    var lastExportPdfFile by mutableStateOf<File?>(null)
        private set

    var showPdfPreview by mutableStateOf(false)
        private set

    val selectedWordSet: WordSet?
        get() = wordSets.firstOrNull { it.id == selectedWordSetId }

    val hasExportReady: Boolean
        get() = lastExportPdfFile?.exists() == true

    init {
        restoreLastExportFromCache()
        viewModelScope.launch {
            repository.syncLegacySetsToLearning()
            refresh()
        }
    }

    fun selectWordSet(id: String) {
        selectedWordSetId = id
        message = ""
        error = ""
    }

    fun dismissPdfPreview() {
        showPdfPreview = false
    }

    fun showPdfPreview() {
        if (lastExportPdfFile?.exists() == true) {
            showPdfPreview = true
        }
    }

    fun importFromUri(uri: Uri) {
        loading = true
        message = ""
        error = ""
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val fileName = app.displayName(uri)
                val parsed = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        WordFileParser.parse(fileName, input)
                    } ?: error("Không đọc được file.")
                }

                if (parsed.words.isEmpty()) {
                    error = "Không tìm thấy từ hợp lệ. File cần cột word và meaning."
                    return@launch
                }

                val now = System.currentTimeMillis()
                val draft = WordSet(
                    id = UUID.randomUUID().toString(),
                    name = fileName.substringBeforeLast('.').ifBlank { "Bộ từ mới" },
                    sourceFileName = fileName,
                    createdAt = now,
                    updatedAt = now,
                    words = parsed.words
                )

                val saved = repository.importWordSet(draft)
                refresh(selectedId = saved.id)
                message = buildImportMessage(saved.name, parsed.words.size, parsed.skippedCount, parsed.duplicateCount)
            } catch (e: Exception) {
                error = e.toImportErrorMessage()
            } finally {
                loading = false
            }
        }
    }

    fun exportSelectedToUri(uri: Uri) {
        val wordSet = selectedWordSet ?: run {
            error = "Chọn một bộ từ trước khi xuất."
            return
        }
        loading = true
        message = ""
        error = ""
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val fileName = "${wordSet.name.safeFileName()}.pdf"

                val cacheFile = withContext(Dispatchers.IO) {
                    val exportDir = File(app.cacheDir, "exports").apply { mkdirs() }
                    val pdfFile = File(exportDir, fileName)
                    WordPdfExporter.writeToFile(wordSet.name, wordSet.words, pdfFile)

                    app.contentResolver.openOutputStream(uri)?.use { output ->
                        pdfFile.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Không ghi được file PDF.")

                    exportPrefs.edit()
                        .putString(KEY_CACHE_FILE, pdfFile.absolutePath)
                        .putString(KEY_FILE_NAME, fileName)
                        .apply()

                    pdfFile
                }

                lastExportPdfFile = cacheFile
                lastExportFileName = fileName
                showPdfPreview = true
                message = "Đã xuất PDF (${wordSet.words.size} từ) vào Downloads. Bấm «Xem PDF» hoặc mở file .pdf trong Downloads."
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Export PDF thất bại."
            } finally {
                loading = false
            }
        }
    }

    fun deleteSelectedWordSet() {
        val id = selectedWordSetId ?: return
        loading = true
        message = ""
        error = ""
        viewModelScope.launch {
            try {
                repository.deleteWordSet(id)
                refresh()
                message = "Đã xóa bộ từ."
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Không xóa được bộ từ."
            } finally {
                loading = false
            }
        }
    }

    private fun restoreLastExportFromCache() {
        val path = exportPrefs.getString(KEY_CACHE_FILE, null) ?: return
        val file = File(path)
        if (!file.exists() || !file.name.endsWith(".pdf", ignoreCase = true)) return
        lastExportPdfFile = file
        lastExportFileName = exportPrefs.getString(KEY_FILE_NAME, file.name) ?: file.name
    }

    private fun refresh(selectedId: String? = null) {
        wordSets = repository.getWordSets()
        selectedWordSetId = selectedId ?: wordSets.firstOrNull()?.id
    }

    private fun buildImportMessage(name: String, valid: Int, skipped: Int, duplicate: Int): String {
        val extra = buildList {
            if (skipped > 0) add("$skipped dòng bỏ qua")
            if (duplicate > 0) add("$duplicate từ trùng")
        }
        val suffix = if (extra.isEmpty()) "" else " (${extra.joinToString(", ")})"
        return "Đã nhập $valid từ vào \"$name\"$suffix. Vào Trang chủ để học bộ này."
    }

    private fun Application.displayName(uri: Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "bo_tu.csv"
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else fallback
        } ?: fallback
    }

    private fun String.safeFileName(): String {
        return replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "minlish_bo_tu" }
    }

    private fun Exception.toImportErrorMessage(): String {
        val raw = localizedMessage.orEmpty()
        return when {
            raw.isBlank() -> "Import thất bại. Kiểm tra định dạng file."
            raw.startsWith("http://") || raw.startsWith("https://") ->
                "Không đọc được file Excel. Hãy dùng CSV hoặc .xlsx."
            else -> raw
        }
    }

    private companion object {
        const val PREF_EXPORT = "minlish_last_export"
        const val KEY_CACHE_FILE = "cache_file_path"
        const val KEY_FILE_NAME = "file_name"
    }
}
