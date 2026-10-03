package com.nk.expensetracker.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    private val dayHeader = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)
    private val dayHeaderWithYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeOfDay = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun nowMillis(): Long = Instant.now().toEpochMilli()

    fun currentMonth(): YearMonth = YearMonth.now(IST)

    fun startOfMonth(yearMonth: YearMonth): Long = yearMonth.atDay(1)
        .atStartOfDay(IST)
        .toInstant()
        .toEpochMilli()

    fun endOfMonth(yearMonth: YearMonth): Long = yearMonth.atEndOfMonth()
        .atTime(23, 59, 59, 999_999_999)
        .atZone(IST)
        .toInstant()
        .toEpochMilli()

    fun millisToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis)
        .atZone(IST)
        .toLocalDate()

    fun millisToLocalDateTime(millis: Long): LocalDateTime = Instant.ofEpochMilli(millis)
        .atZone(IST)
        .toLocalDateTime()

    /** "October 2026" */
    fun formatMonth(millis: Long): String = millisToLocalDate(millis).format(monthYear)

    /** "Today" / "Yesterday" / "Monday, 3 Oct" / "3 Oct 2026" for older dates. */
    fun formatDayHeader(millis: Long, today: LocalDate = LocalDate.now(IST)): String {
        val date = millisToLocalDate(millis)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> if (date.year == today.year) date.format(dayHeader) else date.format(dayHeaderWithYear)
        }
    }

    fun formatShortDate(millis: Long): String = millisToLocalDate(millis).format(shortDate)

    /** "3 Oct, 2:45 pm" */
    fun formatDateTime(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return "${dateTime.format(shortDate)}, ${dateTime.format(timeOfDay)}"
    }
}