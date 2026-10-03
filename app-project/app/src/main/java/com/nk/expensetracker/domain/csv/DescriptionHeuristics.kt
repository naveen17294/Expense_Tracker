package com.nk.expensetracker.domain.csv

import com.nk.expensetracker.domain.model.TransactionType

/**
 * Reads meaning out of a bank description string.
 *
 * HDFC descriptions pack a lot into one field, e.g.
 * `UPI/P2M/5239234/SWIGGY INSTAMART/HDFC BANK` or
 * `IMPS/P2A/4127/NAVEEN KUMAR/HDFC BANK`. These helpers pull the reference, the
 * counterparty, and a defensible transaction type out of that.
 *
 * These are heuristics, not a parser contract. Anything unrecognised is left
 * alone rather than guessed at, and the review queue exists for the rest.
 */
object DescriptionHeuristics {

    /** Reference prefixes HDFC uses. Anything else is treated as free text. */
    private val referencePrefixes = setOf(
        "UPI", "IMPS", "NEFT", "RTGS", "FT", "CHQ", "POS", "AEPS", "IAT", "NPCI", "CCRD"
    )

    private val bankWords = listOf(
        "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOI", "CANARA", "UNION",
        "IDBI", "YESBANK", "INDUSIND", "FEDERAL", "RBL", "PAYTM", "CRED", "ONE CARD",
        "BANK", "VIJAYA", "CENTRAL", "DENA", "CORP", "INDIAN", "SYNDICATE", "BANDHAN",
        "KARNATAKA", "ANDHRA", "BANK OF"
    )

    private val cashWithdrawalPhrases = listOf(
        "cash withdrawal", "cash wdl", "atm wdl", "atw", "atm cash", "atm ",
        "cash dep", "cash deposit", "cdm"
    )

    private val salaryPhrases = listOf(
        "salary", "salary credit", "payroll", "wages", "sal credit", "monthly salary"
    )

    private val refundPhrases = listOf(
        "refund", "reversal", "cashback", "chargeback", "rebate", "refund credit",
        "payment reversed", "amt reversed"
    )

    private val interestPhrases = listOf(
        "interest", "int credited", "interest credit", "dividend", "tax refund"
    )

    private val ownTransferPhrases = listOf(
        "own account", "self transfer", "self as", "between my accounts",
        "transfer to own", "internal transfer", "self credit", "self debit",
        "a/c transfer", "savings to savings", "account to account"
    )

    private val cardPaymentPhrases = listOf(
        "bill payment", "billpay", "standing instruction", "auto pay",
        "autopay", "cc payment", "credit card payment", "ccpay", "card payment",
        "loan emi", "emi payment", "equity payment", "payment"
    )

    private val rentPhrases = listOf(
        "rent", "house rent", "maintenance", "society charges", "no due"
    )

    private val investmentPhrases = listOf(
        "mutual fund", "lic premium", "sip", "fd ", "recurring deposit",
        "ppf", "nps", "small savings"
    )

    private val foodKeywords = listOf(
        "swiggy", "zomato", "restaurant", "cafe", "hotel", "food", "dominos",
        "mcdonald", "kfc", "pizza", "bakery", "canteen", "mess", "dhaba",
        "truck", "chai", "tea", "beverage"
    )

    private val groceryKeywords = listOf(
        "blinkit", "zepto", "instamart", "bigbasket", "dmart", "reliance fresh",
        "supermarket", "grocer", "kirana", "vegetable", "provision"
    )

    private val transportKeywords = listOf(
        "uber", "ola", "rapido", "metro", "irctc", "indian railways", "ticket",
        "petrol", "fuel", "diesel", "hpcl", "ioc", "bharatpetroleum", "parking",
        "fastag", "toll", "namma yatri", "nmm yatri", "mettro"
    )

    /**
     * Category names are matched against [com.nk.expensetracker.data.db.DatabaseSeeder]
     * entries where possible; unseeded ones are created on demand by the importer.
     */
    private val terminalPrefix = Regex(
        "^(POS|ATW|CDM|FT|NEFT|RTGS|IMPS|UPI|CHQ|AEPS|IAT)\\s+[A-Z0-9]+\\s+",
        RegexOption.IGNORE_CASE
    )

    /**
     * Strips channel/terminal noise so `POS 5239 SWIGGY BANGALORE` reads as
     * `SWIGGY BANGALORE` for both classification and display.
     */
    fun normalize(description: String): String =
        terminalPrefix.replace(description.trim(), "").replace(Regex("\\s+"), " ").trim()

    fun suggestCategoryName(
        description: String,
        type: TransactionType,
        countUnrecognisedCreditsAsIncome: Boolean
    ): String? {
        val text = normalize(description).lowercase()

        if (cashWithdrawalPhrases.any { text.contains(it) }) return "Cash"
        if (ownTransferPhrases.any { text.contains(it) }) return "Transfers"
        if (salaryPhrases.any { text.contains(it) }) return "Salary"
        if (refundPhrases.any { text.contains(it) }) return "Refunds"
        if (rentPhrases.any { text.contains(it) }) return "Rent & Housing"
        if (investmentPhrases.any { text.contains(it) }) return "Investments"
        if (interestPhrases.any { text.contains(it) }) return "Interest & Dividends"
        if (cardPaymentPhrases.any { text.contains(it) }) return "Transfers"
        if (text.contains("loan") || text.contains("insurance")) return "Transfers"
        if (text.contains("recharge") || text.contains("bill") || text.contains("utility")) {
            return "Bills & Utilities"
        }
        if (groceryKeywords.any { text.contains(it) }) return "Groceries"
        if (foodKeywords.any { text.contains(it) }) return "Food & Dining"
        if (transportKeywords.any { text.contains(it) }) return "Transportation"

        return when (type) {
            TransactionType.INCOME, TransactionType.REFUND -> "Salary & Income"
            TransactionType.TRANSFER -> "Transfers"
            else -> null
        }
    }

    /** A debit that is really you moving your own money. */
    fun classifyDebit(description: String): TransactionType {
        val text = normalize(description).lowercase()
        return if (ownTransferPhrases.any { text.contains(it) }) {
            TransactionType.TRANSFER
        } else {
            TransactionType.EXPENSE
        }
    }

    /**
     * A credit could be income, a refund, or money you moved yourself.
     *
     * Salary and interest are checked before the generic "payment" rule, so
     * `SALARY PAYMENT` stays income while `AMAZON PAYMENT` on a card statement
     * is treated as a transfer and does not inflate monthly income.
     */
    fun classifyCredit(description: String): TransactionType {
        val text = normalize(description).lowercase()

        if (refundPhrases.any { text.contains(it) }) return TransactionType.REFUND
        if (ownTransferPhrases.any { text.contains(it) }) return TransactionType.TRANSFER
        if (salaryPhrases.any { text.contains(it) }) return TransactionType.INCOME
        if (interestPhrases.any { text.contains(it) }) return TransactionType.INCOME
        if (cardPaymentPhrases.any { text.contains(it) }) return TransactionType.TRANSFER
        if (text.contains("loan") || text.contains("insurance") || text.contains("premium")) {
            return TransactionType.TRANSFER
        }
        return TransactionType.INCOME
    }

    /**
     * Pulls a reference number out of HDFC's `TYPE/.../<ref>/...` prefix scheme.
     * Returns null for free-text descriptions rather than inventing one.
     */
    fun extractReference(description: String): String? {
        val segments = description.trim().split('/', '-', ' ')
        val head = segments.firstOrNull()?.uppercase() ?: return null
        if (head !in referencePrefixes) return null

        val reference = segments.drop(1).firstOrNull { segment ->
            segment.isNotBlank() && segment.count { it.isDigit() } >= 4
        }
        return reference?.takeIf { it.isNotBlank() }?.uppercase()
    }

    /**
     * Best-effort counterparty name. Strips the reference segments and the
     * trailing bank name, then falls back to the raw description when nothing
     * recognisable is left.
     */
    fun extractCounterparty(description: String): String? {
        val cleaned = description.trim()
        if (cleaned.isEmpty()) return null

        val segments = cleaned.split('/', '-')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (segments.isEmpty()) return null

        val head = segments.first().uppercase()
        val candidates = if (head in referencePrefixes) segments.drop(1) else segments

        val meaningful = candidates.filter { segment ->
            // Drop mode/reference tokens: P2A, CR, DR, 5239 — short, no spaces.
            val looksLikeCode = segment.length <= 5 && !segment.contains(' ')
            val looksLikeRef = segment.count { it.isDigit() } >= 4
            val isBank = bankWords.any { word -> segment.uppercase().contains(word) }
            !looksLikeCode && !looksLikeRef && !isBank
        }

        val name = meaningful
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (name.length in 2..60) return tidy(name)

        // Fall back to the description minus any reference prefix.
        return if (head in referencePrefixes) {
            cleaned.substringAfter('/').trim().takeIf { it.isNotEmpty() }?.let { tidy(it.take(60)) }
        } else {
            cleaned.take(60).takeIf { it.isNotBlank() }
        }
    }

    /**
     * Collapses whitespace but preserves the bank's own casing.
     *
     * Statements are written in capitals and names genuinely are `NAVEEN KUMAR`;
     * title-casing mangles them into `Naveen KUMAR`. The field stays editable,
     * so faithfulness beats prettiness here.
     */
    private fun tidy(raw: String): String =
        raw.replace(Regex("\\s+"), " ").trim()
}