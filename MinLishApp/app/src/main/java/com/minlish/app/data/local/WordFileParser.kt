package com.minlish.app.data.local

import com.minlish.app.data.model.WordItem
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedWordFile(
    val words: List<WordItem>,
    val skippedCount: Int,
    val duplicateCount: Int
)

object WordFileParser {
    fun parseCsv(content: String): ParsedWordFile {
        val rows = content
            .removePrefix("\uFEFF")
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
            .toList()
        return rows.toParsedWordFile()
    }

    fun parseXlsx(inputStream: InputStream): ParsedWordFile {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name == SHARED_STRINGS || entry.name == FIRST_SHEET)) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheetBytes = entries[FIRST_SHEET] ?: error("Không tìm thấy sheet đầu tiên trong file Excel.")
        val sharedStrings = entries[SHARED_STRINGS]?.let(::parseSharedStrings).orEmpty()
        return parseSheetRows(sheetBytes, sharedStrings).toParsedWordFile()
    }

    fun toCsv(wordSetName: String, words: List<WordItem>): String {
        val header = listOf("word", "meaning", "example", "part_of_speech", "note", "tags")
        val body = words.map { word ->
            listOf(
                word.term,
                word.meaning,
                word.example,
                word.partOfSpeech,
                word.note,
                word.tags.joinToString(";")
            ).joinToString(",") { it.csvEscaped() }
        }
        return buildString {
            appendLine("# MinLish export: $wordSetName")
            appendLine(header.joinToString(","))
            body.forEach { appendLine(it) }
        }
    }

    private fun List<List<String>>.toParsedWordFile(): ParsedWordFile {
        if (isEmpty()) return ParsedWordFile(emptyList(), skippedCount = 0, duplicateCount = 0)

        val firstRow = first().map { it.normalizedHeader() }
        val hasHeader = HEADER_ALIASES.values.flatten().any { alias -> firstRow.contains(alias) }
        val dataRows = if (hasHeader) drop(1) else this
        val columnMap = if (hasHeader) buildColumnMap(first()) else defaultColumnMap()

        var skipped = 0
        var duplicate = 0
        val seen = mutableSetOf<String>()
        val words = buildList {
            dataRows.forEach { row ->
                val term = row.valueAt(columnMap["term"]).trim()
                val meaning = row.valueAt(columnMap["meaning"]).trim()
                if (term.isBlank() || meaning.isBlank()) {
                    skipped++
                    return@forEach
                }

                val key = term.lowercase()
                if (!seen.add(key)) {
                    duplicate++
                    return@forEach
                }

                add(
                    WordItem(
                        id = UUID.randomUUID().toString(),
                        term = term,
                        meaning = meaning,
                        example = row.valueAt(columnMap["example"]).trim(),
                        note = row.valueAt(columnMap["note"]).trim(),
                        partOfSpeech = row.valueAt(columnMap["partOfSpeech"]).trim(),
                        tags = row.valueAt(columnMap["tags"])
                            .split(";", ",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    )
                )
            }
        }
        return ParsedWordFile(words, skipped, duplicate)
    }

    private fun buildColumnMap(header: List<String>): Map<String, Int?> {
        val normalized = header.map { it.normalizedHeader() }
        return HEADER_ALIASES.mapValues { (_, aliases) ->
            normalized.indexOfFirst { it in aliases }.takeIf { it >= 0 }
        }
    }

    private fun defaultColumnMap(): Map<String, Int?> {
        return mapOf(
            "term" to 0,
            "meaning" to 1,
            "example" to 2,
            "partOfSpeech" to 3,
            "note" to 4,
            "tags" to 5
        )
    }

    private fun List<String>.valueAt(index: Int?): String {
        return if (index != null && index in indices) this[index] else ""
    }

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    cells.add(cell.toString())
                    cell.clear()
                }
                else -> cell.append(char)
            }
            index++
        }
        cells.add(cell.toString())
        return cells
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val document = newSecureDocumentBuilderFactory().newDocumentBuilder()
            .parse(bytes.inputStream())
        val nodes = document.getElementsByTagName("si")
        return buildList {
            for (index in 0 until nodes.length) {
                add(nodes.item(index).allText())
            }
        }
    }

    private fun parseSheetRows(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val document = newSecureDocumentBuilderFactory().newDocumentBuilder()
            .parse(bytes.inputStream())
        val rows = document.getElementsByTagName("row")
        return buildList {
            for (rowIndex in 0 until rows.length) {
                val row = rows.item(rowIndex) as? Element ?: continue
                val cells = row.getElementsByTagName("c")
                val values = sortedMapOf<Int, String>()
                for (cellIndex in 0 until cells.length) {
                    val cell = cells.item(cellIndex) as? Element ?: continue
                    val ref = cell.getAttribute("r")
                    val columnIndex = ref.takeWhile { it.isLetter() }.columnIndex()
                    values[columnIndex] = cell.cellValue(sharedStrings)
                }
                add(values.toRow())
            }
        }
    }

    private fun Element.cellValue(sharedStrings: List<String>): String {
        val type = getAttribute("t")
        val value = when {
            type == "inlineStr" -> getElementsByTagName("is").item(0)?.allText().orEmpty()
            else -> getElementsByTagName("v").item(0)?.textContent.orEmpty()
        }
        return if (type == "s") sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value
    }

    private fun Map<Int, String>.toRow(): List<String> {
        val max = keys.maxOrNull() ?: return emptyList()
        return (0..max).map { this[it].orEmpty() }
    }

    private fun String.columnIndex(): Int {
        if (isBlank()) return 0
        var result = 0
        uppercase().forEach { char ->
            result = result * 26 + (char - 'A' + 1)
        }
        return result - 1
    }

    private fun Node.allText(): String {
        val builder = StringBuilder()
        fun appendText(node: Node) {
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.CDATA_SECTION_NODE) {
                builder.append(node.nodeValue)
            }
            val children = node.childNodes
            for (index in 0 until children.length) {
                appendText(children.item(index))
            }
        }
        appendText(this)
        return builder.toString()
    }

    private fun String.normalizedHeader(): String {
        return trim()
            .lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
    }

    private fun String.csvEscaped(): String {
        val escaped = replace("\"", "\"\"")
        return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
    }

    private fun newSecureDocumentBuilderFactory(): DocumentBuilderFactory {
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
    }

    private const val SHARED_STRINGS = "xl/sharedStrings.xml"
    private const val FIRST_SHEET = "xl/worksheets/sheet1.xml"

    private val HEADER_ALIASES = mapOf(
        "term" to listOf("word", "term", "vocabulary", "tu", "tuvung", "english"),
        "meaning" to listOf("meaning", "definition", "nghia", "vietnamese", "translation"),
        "example" to listOf("example", "vidu", "sentence", "cauvidu"),
        "note" to listOf("note", "ghichu", "notes"),
        "partOfSpeech" to listOf("partofspeech", "pos", "loaitu"),
        "tags" to listOf("tag", "tags", "topic", "chude")
    )
}
