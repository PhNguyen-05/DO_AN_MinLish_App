package com.minlish.app.data.local

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.minlish.app.data.model.WordItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WordPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    fun writeToFile(wordSetName: String, words: List<WordItem>, outputFile: File) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            color = Color.BLACK
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.DKGRAY
        }
        val termPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            color = Color.BLACK
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.BLACK
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(38, 166, 154)
            isFakeBoldText = true
        }

        var y = MARGIN.toFloat()

        fun startNewPage() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN.toFloat()
        }

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) {
                startNewPage()
            }
        }

        canvas.drawText("MinLish", MARGIN.toFloat(), y, labelPaint)
        y += 18f
        canvas.drawText(wordSetName, MARGIN.toFloat(), y, titlePaint)
        y += 26f

        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date())
        canvas.drawText("$date · ${words.size} từ", MARGIN.toFloat(), y, metaPaint)
        y += 28f

        canvas.drawLine(
            MARGIN.toFloat(),
            y,
            (PAGE_WIDTH - MARGIN).toFloat(),
            y,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
        )
        y += 20f

        words.forEachIndexed { index, word ->
            val blockHeight = estimateBlockHeight(word, termPaint, bodyPaint)
            ensureSpace(blockHeight)

            canvas.drawText("${index + 1}. ${word.term}", MARGIN.toFloat(), y, termPaint)
            y += 18f

            y = drawWrappedText(canvas, "Nghĩa: ${word.meaning}", MARGIN.toFloat(), y, bodyPaint) + 4f

            if (word.example.isNotBlank()) {
                y = drawWrappedText(canvas, "VD: ${word.example}", MARGIN.toFloat(), y, bodyPaint) + 4f
            }
            if (word.partOfSpeech.isNotBlank()) {
                canvas.drawText("Loại từ: ${word.partOfSpeech}", MARGIN.toFloat(), y, metaPaint)
                y += 16f
            }

            y += 10f
        }

        document.finishPage(page)
        FileOutputStream(outputFile).use { output -> document.writeTo(output) }
        document.close()
    }

    private fun estimateBlockHeight(word: WordItem, termPaint: Paint, bodyPaint: Paint): Float {
        var height = 18f + lineCount("Nghĩa: ${word.meaning}", bodyPaint) * 15f
        if (word.example.isNotBlank()) {
            height += lineCount("VD: ${word.example}", bodyPaint) * 15f + 4f
        }
        if (word.partOfSpeech.isNotBlank()) height += 16f
        return height + 10f
    }

    private fun lineCount(text: String, paint: Paint): Int {
        return wrapLines(text, paint).size.coerceAtLeast(1)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint
    ): Float {
        var y = startY
        wrapLines(text, paint).forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += 15f
        }
        return y
    }

    private fun wrapLines(text: String, paint: Paint): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= CONTENT_WIDTH) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }
}
