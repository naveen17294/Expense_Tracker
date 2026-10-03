package com.nk.expensetracker.domain.parser

import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object NotepadParser {

    private val dateRegex = Regex("^(\\d{2}-\\d{2}-\\d{4})\\s*\\([A-Za-z]{3}\\)\\s*:(.*)\$")
    private val amountRegex = Regex("^(.*?)\\s+([+-]?\\s*\\d+(?:\\.\\d+)?(?:\\s*\\+\\s*\\d+(?:\\.\\d+)?)*)\$")

    fun parse(input: String): List<TransactionEntity> {
        val results = mutableListOf<TransactionEntity>()
        var currentDateMillis = TimeUtils.nowMillis()
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        input.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("August") || line.startsWith("September")) return@forEach

            val dateMatch = dateRegex.find(line)
            if (dateMatch != null) {
                val dateStr = dateMatch.groupValues[1]
                try {
                    val localDate = LocalDate.parse(dateStr, dateFormatter)
                    currentDateMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    // Ignore date parsing errors
                }
                val remainder = dateMatch.groupValues[2].trim()
                if (remainder.isNotEmpty()) {
                    val tx = parseTransaction(remainder, currentDateMillis)
                    if (tx != null) {
                        results.add(tx)
                    }
                }
            } else {
                val tx = parseTransaction(line, currentDateMillis)
                if (tx != null) {
                    results.add(tx)
                }
            }
        }
        return results
    }

    private fun parseTransaction(line: String, dateMillis: Long): TransactionEntity? {
        val match = amountRegex.find(line) ?: return null
        
        var desc = match.groupValues[1].trim()
        val amountStr = match.groupValues[2].replace(" ", "")
        
        var isIncome = false
        var finalAmountStr = amountStr

        if (amountStr.startsWith("+")) {
            isIncome = true
            finalAmountStr = amountStr.substring(1)
        } else if (amountStr.startsWith("-")) {
            isIncome = false
            finalAmountStr = amountStr.substring(1)
        } else if (desc.contains("returned", ignoreCase = true) || (desc.contains("share", ignoreCase = true) && !desc.contains("power", ignoreCase = true))) {
            isIncome = true
        } else if (desc.contains("refund", ignoreCase = true)) {
            isIncome = true
        }

        if (desc.endsWith("=")) {
            desc = desc.dropLast(1).trim()
        }

        val parts = finalAmountStr.split("+")
        var totalRupees = 0.0
        try {
            totalRupees = parts.sumOf { it.toDouble() }
        } catch(e: Exception) {
            return null
        }

        val amountPaise = Money.rupeesToPaise(totalRupees)
        if (amountPaise <= 0L) return null

        val now = TimeUtils.nowMillis()
        val id = UUID.randomUUID().toString()

        return TransactionEntity(
            id = id,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
            status = TransactionStatus.NEEDS_REVIEW,
            amountPaise = amountPaise,
            currencyCode = "INR",
            occurredAt = dateMillis,
            accountId = "",
            destinationAccountId = null,
            merchantId = null,
            categoryId = null,
            counterpartyName = null,
            description = desc,
            note = "Imported from Notepad",
            paymentMethod = null,
            upiReference = null,
            bankReference = null,
            parentTransactionId = null,
            source = TransactionSource.MANUAL,
            sourceRecordId = null,
            sourceHash = id, 
            confidenceScore = 1.0,
            isUserModified = false,
            createdAt = now,
            updatedAt = now
        )
    }
}
