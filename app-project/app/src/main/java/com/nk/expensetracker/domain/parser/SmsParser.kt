package com.nk.expensetracker.domain.parser

import com.nk.expensetracker.domain.model.TransactionType

data class ParsedSmsResult(
    val amountPaise: Long,
    val type: TransactionType,
    val merchantName: String?,
    val accountLast4: String?,
    val upiReference: String?,
    val timestamp: Long?
)

interface SmsParser {
    fun canParse(sender: String, message: String): Boolean
    fun parse(message: String): ParsedSmsResult?
}
