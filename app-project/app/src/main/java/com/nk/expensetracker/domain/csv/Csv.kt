package com.nk.expensetracker.domain.csv

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Low-level CSV plumbing: delimiter sniffing, an RFC4180-style tokenizer, and
 * tolerant date/amount coercion.
 *
 * Bank statements are not a standard. The same HDFC export can arrive as comma,
 * semicolon or tab separated; dates change format between statements; amounts
 * pick up currency symbols, thousands separators, or accounting parentheses.
 * Everything here is deliberately forgiving and pure, so it is unit-testable
 * without Android.
 */
object Csv {

    private const val SAMPLE_LINES = 25

    /** Picks the delimiter that splits the sample into the most columns. */
    fun detectDelimiter(text: String): Char {
        val lines = text.lineSequence()
            .filter { it.isNotBlank() }
            .take(SAMPLE_LINES)
            .toList()
        if (lines.isEmpty()) return ','

        val candidates = charArrayOf(',', ';', '\t', '|')
        return candidates.maxByOrNull { candidate ->
            // Reward appearing in many rows, not just one dense row.
            val rowsContaining = lines.count { it.contains(candidate) }
            val totalOccurrences = lines.sumOf { it.count { ch -> ch == candidate } }
            rowsContaining * 1000 + totalOccurrences
        } ?: ','
    }

    /**
     * Splits text into records of fields, honouring quoted fields that contain
     * the delimiter, embedded newlines, and doubled quotes (`""` -> `"`).
     */
    fun readRecords(text: String, delimiter: Char): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        val record = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < source.length) {
            val ch = source[index]
            if (inQuotes) {
                when {
                    ch == '"' && index + 1 < source.length && source[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }

                    ch == '"' -> inQuotes = false
                    else -> field.append(ch)
                }
            } else {
                when {
                    ch == '"' -> inQuotes = true
                    ch == delimiter -> {
                        record.add(field.toString())
                        field.setLength(0)
                    }

                    ch == '\r' -> Unit // handled by the following '\n'
                    ch == '\n' -> {
                        record.add(field.toString())
                        field.setLength(0)
                        records.add(record.toList())
                        record.clear()
                    }

                    else -> field.append(ch)
                }
            }
            index++
        }

        if (field.isNotEmpty() || record.isNotEmpty()) {
            record.add(field.toString())
            records.add(record.toList())
        }
        return records
    }

    /** Collapses a header cell to a comparable key: lowercase alphanumerics only. */
    fun normalizeHeader(raw: String): String =
        raw.lowercase().filter { it.isLetterOrDigit() }

    private val numericMatch = Regex("-?\\d[\\d,]*(?:\\.\\d{1,2})?")
    private val debitSuffix = Regex("(?:DR|Debit)\\s*$", RegexOption.IGNORE_CASE)

    /**
     * Coerces a cell into signed paise. Understands `₹1,23,456.78`, `Rs. 500`,
     * `(500.00)` for negatives, and a trailing `DR`.
     *
     * The first numeric run is extracted rather than the whole cell stripped of
     * noise, because currency words contain periods — naively stripping
     * non-numerics turns `Rs. 500` into `.500`.
     */
    fun parseSignedPaise(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val negative = when {
            trimmed.startsWith("(") && trimmed.endsWith(")") -> true
            trimmed.startsWith("-") -> true
            else -> debitSuffix.containsMatchIn(trimmed)
        }

        val match = numericMatch.find(trimmed) ?: return null
        val cleaned = match.value.replace(",", "")
        if (cleaned.toLongOrNull() == null && cleaned.toDoubleOrNull() == null) return null

        val magnitude = cleaned.toBigDecimalOrNull() ?: return null
        val paise = magnitude.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP)
            .longValueExact()
        return if (negative) -paise else paise
    }

    private val dateWithOptionalTime = Regex(
        """^\s*(\d{1,4}[-/.]\d{1,2}[-/.]\d{1,4}|\d{1,2}[-/. ][A-Za-z]{3,}[-/. ]\d{2,4}|\d{1,2}[- ]?[A-Za-z]{3,}[- ]\d{2,4})""" +
            """\s*(?:[T ]\s*(\d{1,2}:\d{2}(?::\d{2})?\s*(?:[AaPp]\.?[Mm]\.?)?))?""",
        RegexOption.IGNORE_CASE
    )

    private val datePatterns = listOf(
        "d/M/yyyy", "d-M-yyyy", "d.M.yyyy", "d_M_yyyy",
        "d/M/yy", "d-M-yy", "d.M.yy",
        "yyyy-MM-dd", "yyyy/MM/dd",
        "d-MMM-yyyy", "d MMM yyyy", "d-MMM-yy", "d MMM yy", "dMMMyyyy", "d MMM yyyy"
    )

    private val timePatterns = listOf(
        "H:mm:ss", "H:mm", "h:mm:ss a", "h:mm a", "HH:mm:ss", "HH:mm"
    )

    /**
     * Parses a statement date cell into epoch millis in [zoneId].
     *
     * Indian statements are day-first, so `03/04/2026` is 3 April. US month-first
     * is only used when the leading number cannot be a day (>12), which avoids
     * silently misreading every ambiguous date.
     */
    fun parseDateToMillis(raw: String, zoneId: ZoneId): Long? {
        val match = dateWithOptionalTime.find(raw) ?: return null
        val dateText = match.groupValues[1].replace(Regex("\\s+"), " ").trim()
        val timeText = match.groupValues[2].trim()

        val date = parseDateFlexible(dateText) ?: return null
        val time = if (timeText.isEmpty()) {
            LocalTime.MIDNIGHT
        } else {
            parseTimeFlexible(timeText) ?: LocalTime.MIDNIGHT
        }

        return LocalDateTime.of(date, time).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun parseDateFlexible(text: String): LocalDate? {
        for (pattern in datePatterns) {
            val formatter = runCatching {
                DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
            }.getOrNull() ?: continue
            runCatching { return LocalDate.parse(text, formatter) }
        }

        // Fall back to swapping day/month for US-ordered input like 04/03/2026.
        val slashParts = text.split('/', '-', '.', '_')
        if (slashParts.size == 3 && slashParts.all { it.all(Char::isDigit) }) {
            val a = slashParts[0].toIntOrNull()
            val b = slashParts[1].toIntOrNull()
            if (a != null && b != null && a > 12) {
                for (pattern in datePatterns.filter { it.startsWith("y") }) {
                    val formatter = runCatching {
                        DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
                    }.getOrNull() ?: continue
                    runCatching { return LocalDate.parse(text, formatter) }
                }
            }
        }
        return null
    }

    private fun parseTimeFlexible(text: String): LocalTime? {
        for (pattern in timePatterns) {
            val formatter = runCatching {
                DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
            }.getOrNull() ?: continue
            runCatching { return LocalTime.parse(text.uppercase(), formatter) }
        }
        return null
    }
}