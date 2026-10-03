package com.nk.expensetracker.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

object TimeUtils {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    fun nowMillis(): Long = Instant.now().toEpochMilli()

    fun currentMonth(): YearMonth = YearMonth.now(IST)

    fun startOfMonth(yearMonth: YearMonth): Long {
        return yearMonth.atDay(1)
            .atStartOfDay(IST)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfMonth(yearMonth: YearMonth): Long {
        return yearMonth.atEndOfMonth()
            .atTime(23, 59, 59, 999_999_999)
            .atZone(IST)
            .toInstant()
            .toEpochMilli()
    }

    fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis)
            .atZone(IST)
            .toLocalDate()
    }

    fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return Instant.ofEpochMilli(millis)
            .atZone(IST)
            .toLocalDateTime()
    }
}
