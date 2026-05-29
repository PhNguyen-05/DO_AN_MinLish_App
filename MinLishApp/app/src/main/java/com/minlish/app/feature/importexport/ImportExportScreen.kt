package com.minlish.app.feature.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.data.model.ImportSummary
import com.minlish.app.data.model.WordItem
import com.minlish.app.data.model.WordSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    viewModel: ImportExportViewModel,
    onBack: () -> Unit
) {
    val primaryColor = Color(0xFF26A69A)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFromUri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportSelectedToUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import / Export bộ từ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor.copy(alpha = 0.1f))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ImportExportActions(
                    loading = viewModel.loading,
                    selectedWordSet = viewModel.selectedWordSet,
                    onImport = {
                        importLauncher.launch(
                            arrayOf(
                                "text/*",
                                "text/csv",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/octet-stream"
                            )
                        )
                    },
                    onExport = {
                        val selected = viewModel.selectedWordSet ?: return@ImportExportActions
                        exportLauncher.launch("${selected.name.safeFileName()}.csv")
                    }
                )
            }

            if (viewModel.message.isNotBlank() || viewModel.error.isNotBlank()) {
                item {
                    StatusMessage(
                        message = viewModel.message,
                        error = viewModel.error,
                        primaryColor = primaryColor
                    )
                }
            }

            viewModel.lastImportSummary?.let { summary ->
                item { ImportSummaryCard(summary = summary, primaryColor = primaryColor) }
            }

            item {
                WordSetSelector(
                    wordSets = viewModel.wordSets,
                    selectedWordSet = viewModel.selectedWordSet,
                    onSelected = viewModel::selectWordSet,
                    onDelete = viewModel::deleteSelectedWordSet,
                    primaryColor = primaryColor
                )
            }

            val words = viewModel.selectedWordSet?.words.orEmpty()
            if (words.isNotEmpty()) {
                item {
                    Text("Preview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                items(words.take(20)) { word ->
                    WordPreviewRow(word = word)
                }
            } else {
                item {
                    EmptyWordSetCard()
                }
            }
        }
    }
}

@Composable
private fun ImportExportActions(
    loading: Boolean,
    selectedWordSet: WordSet?,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    val primaryColor = Color(0xFF26A69A)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onImport,
                    enabled = !loading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Import")
                }
                OutlinedButton(
                    onClick = onExport,
                    enabled = !loading && selectedWordSet != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Export CSV")
                }
            }
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Đang xử lý...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String, error: String, primaryColor: Color) {
    val isError = error.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = if (isError) error else message,
            modifier = Modifier.padding(12.dp),
            color = if (isError) MaterialTheme.colorScheme.error else primaryColor
        )
    }
}

@Composable
private fun ImportSummaryCard(summary: ImportSummary, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(summary.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryPill("${summary.validCount} hợp lệ", primaryColor)
                SummaryPill("${summary.skippedCount} bỏ qua", Color(0xFFEF6C00))
                SummaryPill("${summary.duplicateCount} trùng", Color(0xFF6D4C41))
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, fontSize = 13.sp)
    }
}

@Composable
private fun WordSetSelector(
    wordSets: List<WordSet>,
    selectedWordSet: WordSet?,
    onSelected: (String) -> Unit,
    onDelete: () -> Unit,
    primaryColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Thư viện bộ từ", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = wordSets.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        selectedWordSet?.name ?: "Chưa có bộ từ",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                IconButton(onClick = onDelete, enabled = selectedWordSet != null) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                wordSets.forEach { set ->
                    DropdownMenuItem(
                        text = { Text("${set.name} (${set.words.size} từ)") },
                        onClick = {
                            onSelected(set.id)
                            expanded = false
                        }
                    )
                }
            }
            selectedWordSet?.let {
                Text(
                    "${it.words.size} từ - nguồn: ${it.sourceFileName.ifBlank { "MinLish" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WordPreviewRow(word: WordItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(word.term, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (word.partOfSpeech.isNotBlank()) {
                    Text(word.partOfSpeech, color = Color(0xFF26A69A), fontSize = 13.sp)
                }
            }
            Text(word.meaning, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (word.example.isNotBlank()) {
                HorizontalDivider()
                Text(word.example, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyWordSetCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Chưa có bộ từ", fontWeight = FontWeight.SemiBold)
            Text("Import CSV hoặc Excel để bắt đầu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun String.safeFileName(): String {
    return replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "minlish_word_set" }
}
