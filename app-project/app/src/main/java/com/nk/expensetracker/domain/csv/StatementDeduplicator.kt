package com.nk.expensetracker.domain.csv

import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType

/** A statement row paired with the existing record it duplicates, if any. */
data class ImportCandidate(
    val draft: StatementDraft,
    val isLikelyDuplicate: Boolean,
    val duplicateOfId: String? = null,
    val matchedExisting: TransactionEntity? = null
)

/** Summary of what an import would do, before the user commits to it. */
data class StatementImportPreview(
    val candidates: List<ImportCandidate>,
    val parse: StatementParseResult,
    val accountId: String
) {
    val newCount: Int get() = candidates.count { !it.isLikelyDuplicate }
    val duplicateCount: Int get() = candidates.count { it.isLikelyDuplicate }
    val newDebitPaise: Long
        get() = candidates.filter { !it.isLikelyDuplicate && it.draft.type == TransactionType.EXPENSE }
            .sumOf { it.draft.amountPaise }

    val newCreditPaise: Long
        get() = candidates.filter {
            !it.isLikelyDuplicate && it.draft.type in setOf(TransactionType.INCOME, TransactionType.REFUND)
        }.sumOf { it.draft.amountPaise }
}

/** What actually happened when the import was written. */
data class StatementImportOutcome(
    val inserted: Int = 0,
    val duplicates: Int = 0,
    val failed: Int = 0,
    val totalDebitPaise: Long = 0L,
    val totalCreditPaise: Long = 0L
)

/**
 * Decides whether a statement row is already in the database.
 *
 * Two layers, in order of confidence:
 *
 *  1. **Exact fingerprint** — the parser's `sourceHash` covers date, amount,
 *     type, reference and normalised description, so re-importing the same export
 *     is a guaranteed match.
 *  2. **Fuzzy match** — the same amount and direction on the same day with a
 *     compatible description. This is what catches the overlap between a row
 *     already captured from an SMS and the same row appearing in a statement.
 *
 * Layer 2 deliberately errs toward *not* matching: two identical ₹500 coffees on
 * the same day is entirely possible, and silently dropping real spending is worse
 * than showing the user a duplicate they can delete.
 */
object StatementDeduplicator {

    private const val SAME_DAY_WINDOW_MILLIS = 36 * 60 * 60 * 1000L

    fun mark(
        drafts: List<StatementDraft>,
        existing: List<TransactionEntity>
    ): List<ImportCandidate> {
        val existingHashes = existing.mapNotNull { it.sourceHash }.toHashSet()

        // Only rows that can actually collide are considered, to keep this cheap.
        val dayBuckets = existing.groupBy { bucketKey(it.occurredAt) }

        return drafts.map { draft ->
            val exact = existingHashes.contains(draft.sourceHash)
            if (exact) {
                return@map ImportCandidate(draft, isLikelyDuplicate = true)
            }

            val sameDay = dayBuckets[bucketKey(draft.occurredAt)].orEmpty()
            val fuzzy = sameDay.firstOrNull { candidate ->
                candidate.amountPaise == draft.amountPaise &&
                    candidate.type == draft.type &&
                    kotlin.math.abs(candidate.occurredAt - draft.occurredAt) <= SAME_DAY_WINDOW_MILLIS &&
                    descriptionsAgree(candidate, draft)
            }

            ImportCandidate(
                draft = draft,
                isLikelyDuplicate = fuzzy != null,
                duplicateOfId = fuzzy?.id,
                matchedExisting = fuzzy
            )
        }
    }

    private fun bucketKey(millis: Long): Long =
        TimeUtils.millisToLocalDate(millis).toEpochDay()

    /**
     * Descriptions rarely match verbatim across sources: an SMS says
     * `UPI/P2M/5239234/SWIGGY/HDFC` while the statement says
     * `POS 5239 SWIGGY INSTAMART`. Compare on shared word tokens instead, and
     * accept a shared reference outright.
     */
    private fun descriptionsAgree(existing: TransactionEntity, draft: StatementDraft): Boolean {
        draft.reference?.let { reference ->
            if (reference.equals(existing.upiReference, ignoreCase = true)) return true
            if (reference.equals(existing.bankReference, ignoreCase = true)) return true
        }
        existing.upiReference?.let { reference ->
            if (reference.equals(draft.reference, ignoreCase = true)) return true
        }

        val existingText = existing.description ?: existing.counterpartyName.orEmpty()
        val draftText = draft.description

        val sharedTokens = tokens(existingText).intersect(tokens(draftText))
        if (sharedTokens.isNotEmpty()) return true

        // A bare UPI reference embedded in one side and absent from the other is
        // not enough on its own, but a shared long numeric token is.
        val existingNumbers = existingText.filter(Char::isDigit).length
        return existingNumbers >= 6 &&
            existingNumbers == draftText.count { it.isDigit() }
    }

    private fun tokens(text: String): Set<String> = text
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 4 }
        .filterNot { it in STOP_WORDS }
        .toSet()

    private val STOP_WORDS = setOf(
        "bank", "ltd", "limited", "hdfc", "icici", "sbi", "axis", "kotak",
        "payment", "transaction", "narration", "particulars", "upi", "imps", "neft"
    )
}

/** Source label written to [com.nk.expensetracker.data.db.entity.ImportRecordEntity]. */
internal const val CSV_SOURCE_NAME = "CSV_STATEMENT"
internal const val CSV_PARSER_VERSION = 1

internal fun TransactionEntity.isImported(): Boolean =
    status != TransactionStatus.DELETED && source != TransactionSource.RESTORED_BACKUP