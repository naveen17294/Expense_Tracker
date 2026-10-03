package com.nk.expensetracker.domain.parser

object BankParsers {

    // Helper for amounts: captures numbers with optional commas and decimals
    private const val AMOUNT_REGEX = "(?i)(?:rs\\.?|inr)\\s*(?<amount>[\\d,]+\\.?\\d{0,2})"
    
    // Helper for accounts: matches a/c, acct, ending with XXXXX1234
    private const val ACCOUNT_REGEX = "(?i)(?:a/c|acct|account|a/c no\\.?)\\s*(?:[*x-]*)*(?<account>\\d{3,6})"
    
    // Helper for UPI ref
    private const val UPI_REF_REGEX = "(?i)(?:upi ref no|ref no|upi ref|ref)\\s*(?:[:-])?\\s*(?<ref>\\d+)"

    val HDFC = BaseBankSmsParser(
        senderPattern = Regex(".*HDFC.*"),
        debitRegexes = listOf(
            Regex("$AMOUNT_REGEX has been debited from $ACCOUNT_REGEX.*(?:to|vpa) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX"),
            Regex("$AMOUNT_REGEX debited from $ACCOUNT_REGEX.*(?:to) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX")
        ),
        creditRegexes = listOf(
            Regex("$AMOUNT_REGEX has been credited to $ACCOUNT_REGEX.*(?:from|vpa) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX"),
            Regex("$AMOUNT_REGEX credited to $ACCOUNT_REGEX.*(?:from) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX")
        )
    )

    val SBI = BaseBankSmsParser(
        senderPattern = Regex(".*SBI.*"),
        debitRegexes = listOf(
            Regex("(?i)your $ACCOUNT_REGEX-(?:debited by|debited)\\s*$AMOUNT_REGEX.*(?:trf to) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX")
        ),
        creditRegexes = listOf(
            Regex("(?i)your $ACCOUNT_REGEX-(?:credited by|credited)\\s*$AMOUNT_REGEX.*(?:trf from) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX")
        )
    )

    val ICICI = BaseBankSmsParser(
        senderPattern = Regex(".*ICICI.*"),
        debitRegexes = listOf(
            Regex("$ACCOUNT_REGEX is debited with $AMOUNT_REGEX.*Info:\\s*(?:UPI/)?(?<ref>\\d+)/(?<merchant>[\\w@\\s]+)"),
            Regex("$ACCOUNT_REGEX debited for $AMOUNT_REGEX.*(?:to|vpa) (?<merchant>[\\w@\\s]+).*$UPI_REF_REGEX")
        ),
        creditRegexes = listOf(
            Regex("$ACCOUNT_REGEX is credited with $AMOUNT_REGEX.*Info:\\s*(?:UPI/)?(?<ref>\\d+)/(?<merchant>[\\w@\\s]+)")
        )
    )

    val AXIS = BaseBankSmsParser(
        senderPattern = Regex(".*AXIS.*"),
        debitRegexes = listOf(
            Regex("$AMOUNT_REGEX debited from $ACCOUNT_REGEX.*(?:at) (?<merchant>[\\w@\\s]+)\\.?\\s*$UPI_REF_REGEX")
        ),
        creditRegexes = listOf(
            Regex("$AMOUNT_REGEX credited to $ACCOUNT_REGEX.*(?:from) (?<merchant>[\\w@\\s]+)\\.?\\s*$UPI_REF_REGEX")
        )
    )

    val ALL_PARSERS = listOf(HDFC, SBI, ICICI, AXIS)
}
