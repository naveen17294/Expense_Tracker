package com.nk.expensetracker.domain.csv

import com.nk.expensetracker.domain.model.TransactionType

/** Logical columns the parser understands, regardless of what the file calls them. */
enum class StatementField {
    DATE,
    DESCRIPTION,
    DEBIT,
    CREDIT,
    AMOUNT,
    DIRECTION,
    BALANCE,
    REFERENCE
}

/** One parsed statement line, before it becomes a database entity. */
data class StatementDraft(
    val occurredAt: Long,
    val amountPaise: Long,
    val type: TransactionType,
    val description: String,
    val counterparty: String?,
    val reference: String?,
    val balancePaise: Long?,
    val suggestedCategoryName: String?,
    val sourceHash: String,
    val rowNumber: Int
)

/** Why a row produced no transaction. */
enum class SkipReason {
    NO_DATE,
    NO_AMOUNT,
    ZERO_AMOUNT,
    SUMMARY_ROW,
    MALFORMED
}

/** Outcome of reading a statement, including enough detail to explain the numbers on screen. */
data class StatementParseResult(
    val drafts: List<StatementDraft>,
    val delimiter: Char,
    val headerRowIndex: Int,
    val columnMapping: Map<StatementField, Int>,
    val dataRowCount: Int,
    val statementTitle: String?,
    val accountHint: String?,
    val skipped: Map<SkipReason, Int>,
    val issues: List<String>,
    val totalDebitPaise: Long,
    val totalCreditPaise: Long
) {
    val skippedCount: Int get() = skipped.values.sum()
    val hasUsableHeader: Boolean
        get() = columnMapping.containsKey(StatementField.DATE) &&
            (columnMapping.containsKey(StatementField.AMOUNT) ||
                columnMapping.containsKey(StatementField.DEBIT) ||
                columnMapping.containsKey(StatementField.CREDIT))
}

/**
 * Reads a bank/credit-card statement export into [StatementDraft]s.
 *
 * Deliberately format-agnostic: it maps whatever headers are present onto the
 * logical columns above, so the same code handles an HDFC savings statement and
 * an HDFC credit-card statement. Classification is heuristic and conservative —
 * when a credit cannot be explained it is surfaced as a question for the user
 * rather than being silently booked as income.
 */
object BankStatementParser {

    fun parse(
        text: String,
        zoneId: java.time.ZoneId,
        countUnrecognisedCreditsAsIncome: Boolean = true
    ): StatementParseResult {
        val delimiter = Csv.detectDelimiter(text)
        val records = Csv.readRecords(text, delimiter)
        if (records.isEmpty()) {
            return emptyResult(delimiter)
        }

        val headerIndex = findHeaderRow(records)
        if (headerIndex < 0) return emptyResult(delimiter)

        val header = records[headerIndex]
        val mapping = mapColumns(header)

        val title = records.take(headerIndex)
            .firstOrNull { record -> record.any { it.isNotBlank() } }
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.takeIf { it.length <= 120 }
        val accountHint = findAccountHint(records.take(headerIndex + 1))

        val drafts = mutableListOf<StatementDraft>()
        val skipped = mutableMapOf<SkipReason, Int>()
        val issues = mutableListOf<String>()
        var debitTotal = 0L
        var creditTotal = 0L
        var dataRows = 0

        records.drop(headerIndex + 1).forEachIndexed { offset, record ->
            val rowNumber = headerIndex + offset + 2
            dataRows++

            fun skip(reason: SkipReason) {
                skipped[reason] = (skipped[reason] ?: 0) + 1
            }

            val description = mapping[StatementField.DESCRIPTION]
                ?.let { record.getOrNull(it) }
                .orEmpty()
                .trim()

            // Opening/closing balance and total lines carry a date but no money
            // movement. Checked before the date so they are reported as summary
            // rows rather than as unparseable dates.
            if (looksLikeSummaryRow(description)) {
                skip(SkipReason.SUMMARY_ROW)
                return@forEachIndexed
            }

            val dateIndex = mapping[StatementField.DATE]
            val rawDate = dateIndex?.let { record.getOrNull(it) }

            // The label sometimes occupies the date column instead.
            if (looksLikeSummaryRow(rawDate)) {
                skip(SkipReason.SUMMARY_ROW)
                return@forEachIndexed
            }

            val occurredAt = rawDate?.let { Csv.parseDateToMillis(it, zoneId) }
            if (occurredAt == null) {
                skip(
                    if (rawDate.isNullOrBlank()) SkipReason.SUMMARY_ROW else SkipReason.NO_DATE
                )
                return@forEachIndexed
            }

            val (amountPaise, type) = resolveAmountAndType(record, mapping, description)
                ?: run {
                    skip(SkipReason.NO_AMOUNT)
                    return@forEachIndexed
                }

            if (amountPaise == 0L) {
                skip(SkipReason.ZERO_AMOUNT)
                return@forEachIndexed
            }

            val reference = mapping[StatementField.REFERENCE]
                ?.let { record.getOrNull(it) }
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: DescriptionHeuristics.extractReference(description)

            val counterparty = DescriptionHeuristics.extractCounterparty(description)
            val suggestedCategory = DescriptionHeuristics.suggestCategoryName(
                description = description,
                type = type,
                countUnrecognisedCreditsAsIncome = countUnrecognisedCreditsAsIncome
            )

            when (type) {
                TransactionType.EXPENSE -> debitTotal += amountPaise
                TransactionType.INCOME, TransactionType.REFUND -> creditTotal += amountPaise
                TransactionType.TRANSFER -> Unit
            }

            drafts += StatementDraft(
                occurredAt = occurredAt,
                amountPaise = amountPaise,
                type = type,
                description = description,
                counterparty = counterparty,
                reference = reference,
                balancePaise = mapping[StatementField.BALANCE]
                    ?.let { record.getOrNull(it) }
                    ?.let { Csv.parseSignedPaise(it) }
                    ?.let { kotlin.math.abs(it) },
                suggestedCategoryName = suggestedCategory,
                sourceHash = hashOf(occurredAt, amountPaise, type, description, reference),
                rowNumber = rowNumber
            )

            if (drafts.size > MAX_DRAFTS) {
                issues += "Stopped after $MAX_DRAFTS rows to stay within memory limits."
                return@forEachIndexed
            }
        }

        return StatementParseResult(
            drafts = drafts,
            delimiter = delimiter,
            headerRowIndex = headerIndex,
            columnMapping = mapping,
            dataRowCount = dataRows,
            statementTitle = title,
            accountHint = accountHint,
            skipped = skipped,
            issues = issues.take(MAX_ISSUES),
            totalDebitPaise = debitTotal,
            totalCreditPaise = creditTotal
        )
    }

    private const val MAX_DRAFTS = 20_000
    private const val MAX_ISSUES = 5

    private fun emptyResult(delimiter: Char) = StatementParseResult(
        drafts = emptyList(),
        delimiter = delimiter,
        headerRowIndex = -1,
        columnMapping = emptyMap(),
        dataRowCount = 0,
        statementTitle = null,
        accountHint = null,
        skipped = emptyMap(),
        issues = listOf(
            "No statement header was recognised. Expected a row with a date column and an amount column."
        ),
        totalDebitPaise = 0L,
        totalCreditPaise = 0L
    )

    /**
     * Statement exports usually start with a title block ("HDFC Bank Statement",
     * account number, date range) before the real column headers.
     */
    private fun findHeaderRow(records: List<List<String>>): Int {
        val limit = minOf(records.size, HEADER_SEARCH_DEPTH)
        var bestIndex = -1
        var bestScore = 0
        for (index in 0 until limit) {
            val score = mapColumns(records[index]).size
            val mapping = mapColumns(records[index])
            val isUsable = mapping.containsKey(StatementField.DATE) &&
                (mapping.containsKey(StatementField.AMOUNT) ||
                    mapping.containsKey(StatementField.DEBIT) ||
                    mapping.containsKey(StatementField.CREDIT))
            if (isUsable && score > bestScore) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex
    }

    private const val HEADER_SEARCH_DEPTH = 20

    /**
     * Header aliases, most specific first. Matching is done on a normalised key
     * (lowercase alphanumerics), so "Transaction Date" and "transaction_date"
     * collapse to the same thing.
     */
    private val aliases: Map<StatementField, List<String>> = mapOf(
        StatementField.DATE to listOf(
            "transactiondate", "txndate", "transdate", "valuedate", "postingdate",
            "date", "txndates"
        ),
        StatementField.DESCRIPTION to listOf(
            "transactiondescription", "description", "particulars", "particular",
            "narrative", "narration", "details", "remarks", "payee", "beneficiary",
            "transactiondetails", "memo"
        ),
        StatementField.DEBIT to listOf(
            "debitamount", "debit", "withdrawalamount", "withdrawal", "withdrawals",
            "paidout", "debits", "debitinr", "amountdebited", "withdrawalamt"
        ),
        StatementField.CREDIT to listOf(
            "creditamount", "credit", "depositamount", "deposit", "deposits",
            "paidin", "credits", "creditinr", "amountcredited", "depositamt"
        ),
        StatementField.AMOUNT to listOf(
            "transactionamount", "amount", "amt", "txnamount", "value", "amountinr"
        ),
        StatementField.DIRECTION to listOf(
            "debitcredit", "drcr", "dr/cr", "transactiontype", "txntype",
            "indicator", "dr_cr", "debit/credit"
        ),
        StatementField.BALANCE to listOf(
            "runningbalance", "closingbalance", "balance", "bal", "availablebalance",
            "runningbal", "balanceaftertransaction"
        ),
        StatementField.REFERENCE to listOf(
            "transactionreference", "transactionid", "reference", "referencenumber",
            "refno", "ref", "utr", "rrn", "rrnnumber", "upireference", "upiref",
            "txnid", "transactionrefno"
        )
    )

    /** Fields a single column may satisfy. AMOUNT and DIRECTION can share nothing else. */
    private fun mapColumns(header: List<String>): Map<StatementField, Int> {
        val normalized = header.map { Csv.normalizeHeader(it) }
        val mapping = mutableMapOf<StatementField, Int>()
        val claimed = mutableSetOf<Int>()

        // Exact alias matches win, so "credit" never steals the "credit amount" column.
        for (pass in 0..1) {
            for ((field, names) in aliases) {
                if (field in mapping) continue
                for (index in normalized.indices) {
                    if (index in claimed || normalized[index].isEmpty()) continue
                    val hit = names.any { candidate ->
                        if (pass == 0) candidate == normalized[index]
                        else normalized[index].contains(candidate) || candidate.contains(normalized[index])
                    }
                    if (hit) {
                        mapping[field] = index
                        claimed.add(index)
                        break
                    }
                }
            }
        }
        return mapping
    }

    private val summaryPhrases = listOf(
        "opening balance", "closing balance", "brought forward", "carried forward",
        "grand total", "openingbalance", "closingbalance",
        "statement generated", "page "
    )

    /**
     * Statements label their total lines in the description column, or in the
     * date column when the row has no real transaction. Both are checked.
     *
     * Bare "total" is deliberately not a marker: a payee called "Total Fitness"
     * would otherwise be silently dropped.
     */
    private fun looksLikeSummaryRow(vararg cells: String?): Boolean {
        val text = cells.filterNotNull().joinToString(" ").lowercase()
        return text.isNotBlank() && summaryPhrases.any { text.contains(it) }
    }

    /**
     * Works out the amount and the transaction type.
     *
     * Separate debit/credit columns are unambiguous. A single amount column relies
     * on the sign, or on a Dr/Cr indicator column when present.
     */
    private fun resolveAmountAndType(
        record: List<String>,
        mapping: Map<StatementField, Int>,
        description: String
    ): Pair<Long, TransactionType>? {
        mapping[StatementField.DEBIT]?.let { debitIndex ->
            val debit = record.getOrNull(debitIndex)?.let { Csv.parseSignedPaise(it) }
            if (debit != null && debit != 0L) {
                return kotlin.math.abs(debit) to TransactionType.EXPENSE
            }
        }
        mapping[StatementField.CREDIT]?.let { creditIndex ->
            val credit = record.getOrNull(creditIndex)?.let { Csv.parseSignedPaise(it) }
            if (credit != null && credit != 0L) {
                val type = DescriptionHeuristics.classifyCredit(description)
                return kotlin.math.abs(credit) to type
            }
        }
        mapping[StatementField.AMOUNT]?.let { amountIndex ->
            val raw = record.getOrNull(amountIndex) ?: return null
            val signed = Csv.parseSignedPaise(raw) ?: return null
            val indicator = mapping[StatementField.DIRECTION]
                ?.let { record.getOrNull(it) }
                .orEmpty()
                .trim()
            val isDebit = when {
                indicator.equals("dr", true) || indicator.equals("debit", true) -> true
                indicator.equals("cr", true) || indicator.equals("credit", true) -> false
                else -> signed < 0
            }
            val type = if (isDebit) {
                DescriptionHeuristics.classifyDebit(description)
            } else {
                DescriptionHeuristics.classifyCredit(description)
            }
            return kotlin.math.abs(signed) to type
        }
        return null
    }

    private val accountHintPattern = Regex("(?:X{2,4}|\\d{2,4}X{2,4}|X{4})(\\d{4})", RegexOption.IGNORE_CASE)

    private fun findAccountHint(records: List<List<String>>): String? =
        records.asSequence()
            .take(HEADER_SEARCH_DEPTH)
            .flatMap { it.asSequence() }
            .mapNotNull { accountHintPattern.find(it)?.groupValues?.getOrNull(1) }
            .firstOrNull()

    /**
     * Stable fingerprint for deduplication.
     *
     * Uses only fields that describe the transaction itself, so re-importing the
     * same statement twice produces identical hashes and the unique index on
     * `sourceHash` rejects them. Deliberately not tied to the row position or
     * file name, which change between exports.
     */
    private fun hashOf(
        occurredAt: Long,
        amountPaise: Long,
        type: TransactionType,
        description: String,
        reference: String?
    ): String {
        val canonical = listOf(
            "csv",
            occurredAt.toString(),
            amountPaise.toString(),
            type.name,
            reference?.lowercase().orEmpty(),
            description.lowercase().replace(Regex("\\s+"), " ").trim()
        ).joinToString("|")

        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}