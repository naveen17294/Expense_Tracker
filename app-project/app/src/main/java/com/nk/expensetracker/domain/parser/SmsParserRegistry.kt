package com.nk.expensetracker.domain.parser

class SmsParserRegistry {
    private val parsers = BankParsers.ALL_PARSERS

    fun parse(sender: String, message: String): ParsedSmsResult? {
        val parser = parsers.firstOrNull { it.canParse(sender, message) }
        return parser?.parse(message)
    }
}
