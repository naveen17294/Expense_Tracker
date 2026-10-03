package com.nk.expensetracker.data.csv

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.ExpenseTrackerDatabase
import com.nk.expensetracker.domain.csv.BankStatementParser
import com.nk.expensetracker.domain.csv.CSV_PARSER_VERSION
import com.nk.expensetracker.domain.csv.CSV_SOURCE_NAME
import com.nk.expensetracker.domain.csv.ImportCandidate
import com.nk.expensetracker.domain.csv.StatementDeduplicator
import com.nk.expensetracker.domain.csv.StatementDraft
import com.nk.expensetracker.domain.csv.StatementImportOutcome
import com.nk.expensetracker.domain.csv.StatementImportPreview
import com.nk.expensetracker.domain.csv.StatementParseResult
import com.nk.expensetracker.data.db.entity.CategoryEntity
import com.nk.expensetracker.data.db.entity.ImportRecordEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a statement file into transactions.
 *
 * The flow is deliberately two-phase: [preview] parses and reports what *would*
 * happen, and only [commit] writes. Nothing touches the database until the user
 * has seen the row count, the duplicates, and the totals.
 */
@Singleton
class StatementImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ExpenseTrackerDatabase
) {

    /** Guard against a pathological export exhausting memory. */
    private val maxBytes = 8L * 1024 * 1024

    suspend fun preview(
        uri: Uri,
        accountId: String,
        countUnrecognisedCreditsAsIncome: Boolean = true
    ): Result<StatementImportPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val text = readText(uri)
            val parse = BankStatementParser.parse(
                text = text,
                zoneId = TimeUtils.IST,
                countUnrecognisedCreditsAsIncome = countUnrecognisedCreditsAsIncome
            )

            if (!parse.hasUsableHeader) {
                throw StatementImportException.NoRecognisableHeader(parse.issues.firstOrNull())
            }
            if (parse.drafts.isEmpty()) {
                throw StatementImportException.NoTransactionsFound()
            }

            val existing = loadExistingTransactions(parse)
            val candidates = StatementDeduplicator.mark(parse.drafts, existing)

            StatementImportPreview(
                candidates = candidates,
                parse = parse,
                accountId = accountId
            )
        }
    }

    /**
     * Writes the preview to the database in one transaction, so a failure part way
     * through cannot leave a half-imported statement behind.
     */
    suspend fun commit(preview: StatementImportPreview): Result<StatementImportOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val categoryIds = resolveCategories(preview.candidates)
                val now = TimeUtils.nowMillis()
                val importRecords = mutableListOf<ImportRecordEntity>()

                var inserted = 0
                var duplicates = 0
                var failed = 0
                var debit = 0L
                var credit = 0L

                database.withTransaction {
                    preview.candidates.forEach { candidate ->
                        if (candidate.isLikelyDuplicate) {
                            duplicates++
                            importRecords += candidate.toImportRecord("DUPLICATE", now)
                            return@forEach
                        }

                        val transaction = candidate.toEntity(
                            accountId = preview.accountId,
                            categoryId = candidate.categoryId(categoryIds),
                            now = now
                        )

                        // insertTransaction uses OnConflictStrategy.IGNORE, so a
                        // race against a concurrent import returns -1.
                        val rowId = database.transactionDao().insertTransaction(transaction)
                        if (rowId == -1L) {
                            duplicates++
                            importRecords += candidate.toImportRecord("DUPLICATE", now)
                        } else {
                            inserted++
                            when (transaction.type) {
                                com.nk.expensetracker.domain.model.TransactionType.EXPENSE -> debit += transaction.amountPaise
                                com.nk.expensetracker.domain.model.TransactionType.INCOME,
                                com.nk.expensetracker.domain.model.TransactionType.REFUND -> credit += transaction.amountPaise

                                com.nk.expensetracker.domain.model.TransactionType.TRANSFER -> Unit
                            }
                            importRecords += candidate.toImportRecord("PARSED", now)
                        }
                    }

                    importRecords.forEach { database.importRecordDao().insertImportRecord(it) }
                }

                StatementImportOutcome(
                    inserted = inserted,
                    duplicates = duplicates,
                    failed = failed,
                    totalDebitPaise = debit,
                    totalCreditPaise = credit
                )
            }
        }

    private fun readText(uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected file")
        return stream.use { input ->
            val declaredSize = context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: -1L
            if (declaredSize > maxBytes) throw StatementImportException.FileTooLarge()

            val bytes = input.readBytes()
            if (bytes.size > maxBytes) throw StatementImportException.FileTooLarge()
            // Statement exports are frequently UTF-16 or Windows-1252; try UTF-8
            // first and fall back rather than producing mojibake.
            decode(bytes)
        }
    }

    private fun decode(bytes: ByteArray): String {
        val skipBom = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) 3 else 0

        if (bytes.size >= 2) {
            val marker = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
            if (marker == 0xFEFF || marker == 0xFFFE) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
                    .removePrefix("\uFEFF")
            }
        }

        val text = String(bytes, skipBom, bytes.size - skipBom, Charsets.UTF_8)
        // A UTF-8 decode that produced replacement characters was probably not UTF-8.
        return if (text.contains('\uFFFD')) String(bytes, skipBom, bytes.size - skipBom, Charsets.ISO_8859_1)
        else text
    }

    /**
     * Loads only the transactions that could collide, using the statement's own
     * date range padded by a day on each side for timezone edges.
     */
    private suspend fun loadExistingTransactions(parse: StatementParseResult): List<TransactionEntity> {
        if (parse.drafts.isEmpty()) return emptyList()
        val earliest = parse.drafts.minOf { it.occurredAt }
        val latest = parse.drafts.maxOf { it.occurredAt }
        val pad = 24 * 60 * 60 * 1000L
        return database.transactionDao().getTransactionsInRange(earliest - pad, latest + pad)
    }

    /**
     * Maps the parser's suggested category names onto real category rows,
     * creating any that the seeder did not ship.
     */
    private suspend fun resolveCategories(
        candidates: List<ImportCandidate>
    ): Map<String, String> {
        val wanted = candidates.mapNotNull { it.draft.suggestedCategoryName }.toSet()
        if (wanted.isEmpty()) return emptyMap()

        val existing = database.categoryDao().getAllCategoriesSnapshot()
        val resolved = mutableMapOf<String, String>()

        wanted.forEach { name ->
            val match = existing.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (match != null) {
                resolved[name] = match.id
            } else {
                val created = CategoryEntity(
                    name = name,
                    icon = null,
                    colorHex = null,
                    parentCategoryId = null,
                    isExpense = true,
                    isIncome = false,
                    createdAt = TimeUtils.nowMillis(),
                    updatedAt = TimeUtils.nowMillis()
                )
                database.categoryDao().insertCategory(created)
                resolved[name] = created.id
            }
        }
        return resolved
    }
}

private fun ImportCandidate.categoryId(resolved: Map<String, String>): String? =
    draft.suggestedCategoryName?.let { resolved[it] }

private fun ImportCandidate.toEntity(accountId: String, categoryId: String?, now: Long) = TransactionEntity(
    type = draft.type,
    // A statement is an authoritative record, so rows land confirmed rather than
    // in the review queue. Anything misread is corrected in Add/Edit.
    status = TransactionStatus.CONFIRMED,
    amountPaise = draft.amountPaise,
    currencyCode = "INR",
    occurredAt = draft.occurredAt,
    accountId = accountId,
    destinationAccountId = null,
    merchantId = null,
    categoryId = categoryId,
    counterpartyName = draft.counterparty,
    description = draft.description.takeIf { it.isNotBlank() },
    note = null,
    paymentMethod = null,
    upiReference = draft.reference,
    bankReference = null,
    parentTransactionId = null,
    source = TransactionSource.CSV,
    sourceRecordId = null,
    sourceHash = draft.sourceHash,
    confidenceScore = null,
    isUserModified = false,
    createdAt = now,
    updatedAt = now
)

/**
 * The stored excerpt is the parsed description, which is already statement text
 * rather than an SMS body. Kept short so a diagnostic dump cannot leak a whole file.
 */
private fun ImportCandidate.toImportRecord(outcome: String, now: Long) = ImportRecordEntity(
    sourceHash = draft.sourceHash,
    sender = CSV_SOURCE_NAME,
    receivedTime = draft.occurredAt,
    parserVersion = CSV_PARSER_VERSION,
    outcome = outcome,
    redactedText = draft.description.take(120),
    createdAt = now
)

/** Failures the user can act on, as opposed to an unexpected crash. */
sealed class StatementImportException(message: String) : Exception(message) {
    class FileTooLarge :
        StatementImportException("That file is too large to import. Try a shorter statement period.")

    class NoRecognisableHeader(detail: String?) :
        StatementImportException(
            detail ?: "No statement header was recognised in this file."
        )

    class NoTransactionsFound :
        StatementImportException("No transactions were found in this statement.")
}