package com.nk.expensetracker.domain.parser

import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.domain.model.TransactionType

class BaseBankSmsParser(
    private val senderPattern: Regex,
    private val debitRegexes: List<Regex>,
    private val creditRegexes: List<Regex>
) : SmsParser {

    override fun canParse(sender: String, message: String): Boolean {
        return senderPattern.matches(sender) || sender.contains(senderPattern.pattern, ignoreCase = true)
    }

    override fun parse(message: String): ParsedSmsResult? {
        // Try Debit
        for (regex in debitRegexes) {
            val match = regex.find(message)
            if (match != null) {
                return extractResult(match, TransactionType.EXPENSE)
            }
        }

        // Try Credit
        for (regex in creditRegexes) {
            val match = regex.find(message)
            if (match != null) {
                return extractResult(match, TransactionType.INCOME)
            }
        }

        return null
    }

    private fun extractResult(matchResult: MatchResult, type: TransactionType): ParsedSmsResult {
        val groups = matchResult.groups
        
        // Amount parsing
        val amountStr = groups["amount"]?.value?.replace(",", "") ?: "0"
        val amountPaise = Money.rupeesToPaise(amountStr.toDoubleOrNull() ?: 0.0)

        // Merchant parsing (could be labeled as merchant, payee, info, or to)
        val merchant = groups["merchant"]?.value?.trim() 
            ?: groups["payee"]?.value?.trim()
            ?: groups["info"]?.value?.trim()
            
        // Clean merchant
        val cleanMerchant = merchant?.removePrefix("VPA ")?.removePrefix("- ")?.trim()

        // Account parsing
        val account = groups["account"]?.value?.trim()?.takeLast(4)

        // Ref parsing
        val ref = groups["ref"]?.value?.trim()

        return ParsedSmsResult(
            amountPaise = amountPaise,
            type = type,
            merchantName = cleanMerchant,
            accountLast4 = account,
            upiReference = ref,
            timestamp = null // Timestamp will be set by the SMS received time usually, or we can parse it from string
        )
    }
}
