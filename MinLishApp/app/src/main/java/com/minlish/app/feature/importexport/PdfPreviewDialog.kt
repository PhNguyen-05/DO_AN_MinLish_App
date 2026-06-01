package com.minlish.app.feature.importexport

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfPreviewDialog(
    pdfFile: File,
    fileName: String,
    onDismiss: () -> Unit
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(pdfFile, pageIndex) {
        loading = true
        bitmap = withContext(Dispatchers.IO) {
            renderPdfPage(pdfFile, pageIndex)
        }
        loading = false
    }

    LaunchedEffect(pdfFile) {
        pageCount = withContext(Dispatchers.IO) { readPageCount(pdfFile) }
        pageIndex = 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    bitmap?.let { pageBitmap ->
                        Image(
                            bitmap = pageBitmap.asImageBitmap(),
                            contentDescription = "Trang PDF ${pageIndex + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                if (pageCount > 0) {
                    Text("Trang ${pageIndex + 1} / $pageCount")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { if (pageIndex > 0) pageIndex-- }, enabled = pageIndex > 0) {
                    Text("Trước")
                }
                TextButton(onClick = { if (pageIndex < pageCount - 1) pageIndex++ }, enabled = pageIndex < pageCount - 1) {
                    Text("Sau")
                }
                TextButton(onClick = onDismiss) { Text("Đóng") }
            }
        }
    )
}

private fun readPageCount(file: File): Int {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { return it.pageCount }
    }
}

private fun renderPdfPage(file: File, pageIndex: Int): Bitmap? {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { renderer ->
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            renderer.openPage(pageIndex).use { page ->
                val width = (page.width * 1.2f).toInt().coerceAtMost(1200)
                val height = (page.height * 1.2f).toInt().coerceAtMost(1600)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }
}
