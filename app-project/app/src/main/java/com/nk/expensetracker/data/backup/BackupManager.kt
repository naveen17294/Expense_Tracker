package com.nk.expensetracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nk.expensetracker.data.db.ExpenseTrackerDatabase
import com.nk.expensetracker.domain.model.BackupPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ExpenseTrackerDatabase
) {
    private val jsonFormat = Json { 
        prettyPrint = false 
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Gather all data
            val payload = BackupPayload(
                exportedAt = System.currentTimeMillis(),
                accounts = database.accountDao().getAllAccounts().first(),
                categories = database.categoryDao().getAllCategories().first(),
                merchants = database.merchantDao().getAllMerchants().first(),
                rules = database.categorizationRuleDao().getAllRules().first(),
                transactions = database.transactionDao().getAllTransactions().first()
            )

            val jsonString = jsonFormat.encodeToString(payload)

            // Write to content URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: return@withContext Result.failure(Exception("Could not open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read from content URI
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Could not open input stream"))

            val payload = jsonFormat.decodeFromString<BackupPayload>(jsonString)

            database.withTransaction {
                database.clearAllTables()
                payload.accounts.forEach { database.accountDao().insertAccount(it) }
                payload.categories.forEach { database.categoryDao().insertCategory(it) }
                payload.merchants.forEach { database.merchantDao().insertMerchant(it) }
                payload.rules.forEach { database.categorizationRuleDao().insertRule(it) }
                payload.transactions.forEach { database.transactionDao().insertTransaction(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
