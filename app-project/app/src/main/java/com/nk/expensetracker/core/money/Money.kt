package com.nk.expensetracker.core.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * All monetary values are stored as Long paise (1/100 of a Rupee).
 * ₹123.45 = 12345L paise. Never use Double for storage or arithmetic.
 */
object Money {

    private val indiaLocale = Locale("en", "IN")
    private val rupeeSymbol = "₹"

    /** Full precision, Indian digit grouping. ₹1,23,456.00 */
    fun formatPaise(paise: Long): String {
        val formatter = DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(indiaLocale))
        return rupeeSymbol + formatter.format(paise / 100.0)
    }

    /** Drops a trailing `.00` for whole-rupee amounts. ₹1,23,456 */
    fun formatPaiseTrimmed(paise: Long): String {
        val symbols = DecimalFormatSymbols.getInstance(indiaLocale)
        val pattern = if (paise % 100L == 0L) "#,##0" else "#,##0.00"
        return rupeeSymbol + DecimalFormat(pattern, symbols).format(paise / 100.0)
    }

    /** Short form for tight spaces such as chart labels. ₹1.2L / ₹45.3K / ₹820 */
    fun formatCompact(paise: Long): String {
        val rupees = paise / 100.0
        val magnitude = abs(rupees)
        val (scaled, suffix) = when {
            magnitude >= 10_000_000 -> rupees / 10_000_000 to "Cr"
            magnitude >= 100_000 -> rupees / 100_000 to "L"
            magnitude >= 1_000 -> rupees / 1_000 to "K"
            else -> rupees to ""
        }
        val pattern = if (suffix.isEmpty()) "#,##0.##" else "#,##0.#"
        val text = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(indiaLocale)).format(scaled)
        return rupeeSymbol + text + suffix
    }

    /** Explicit sign for flow figures. Expense is negative, income positive. */
    fun formatSigned(paise: Long, isOutflow: Boolean): String {
        val prefix = if (isOutflow) "−" else "+"
        return prefix + formatPaiseTrimmed(kotlin.math.abs(paise))
    }

    /**
     * Exact decimal-string to paise conversion. Avoids the rounding drift you get
     * from parsing through Double (0.29 * 100 == 28.999…).
     * Returns null when the text is not a valid non-negative amount.
     */
    fun parseRupeesToPaise(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0L
        if (!trimmed.matches(Regex("^\\d{1,12}(\\.\\d{0,2})?$"))) return null

        val parts = trimmed.split('.')
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = when {
            parts.size == 1 -> "00"
            parts[1].isEmpty() -> "00"
            else -> parts[1].padEnd(2, '0')
        }
        return whole * 100L + fraction.toLong()
    }

    /** Rupees entered as a Double. Rounded, not truncated, to the nearest paisa. */
    fun rupeesToPaise(rupees: Double): Long = (rupees * 100.0).roundToLong()

    fun paiseToRupees(paise: Long): Double = paise / 100.0
}