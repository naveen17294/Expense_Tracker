package com.nk.expensetracker.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.categorization.CategorizationEngine
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.parser.SmsParserRegistry
import com.nk.expensetracker.domain.repository.AccountRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var categorizationEngine: CategorizationEngine
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parserRegistry = SmsParserRegistry()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        
        val pendingResult = goAsync()

        scope.launch {
            try {
                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: continue
                    val body = sms.displayMessageBody ?: continue
                    val timestamp = sms.timestampMillis

                    Log.d("SmsReceiver", "Received SMS from $sender")
                    
                    val parsedResult = parserRegistry.parse(sender, body)
                    
                    if (parsedResult != null) {
                        Log.d("SmsReceiver", "Successfully parsed SMS! Amount: ${parsedResult.amountPaise}")
                        
                        // Hash for deduplication
                        val sourceHash = hashString("$sender|$body|$timestamp")
                        
                        // Match account
                        val accounts = accountRepository.getAllAccounts().first()
                        val matchedAccount = accounts.find { 
                            it.maskedIdentifier == parsedResult.accountLast4 
                        } ?: accounts.firstOrNull() // Fallback to first account (usually Cash) if no match

                        var entity = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            type = parsedResult.type,
                            status = TransactionStatus.NEEDS_REVIEW, // Needs manual review
                            amountPaise = parsedResult.amountPaise,
                            currencyCode = "INR",
                            occurredAt = timestamp,
                            accountId = matchedAccount?.id ?: "unknown",
                            destinationAccountId = null,
                            merchantId = null,
                            categoryId = null,
                            counterpartyName = parsedResult.merchantName,
                            description = null,
                            note = "Auto-imported from SMS",
                            paymentMethod = null, // Could infer from regex
                            upiReference = parsedResult.upiReference,
                            bankReference = null,
                            parentTransactionId = null,
                            source = TransactionSource.SMS,
                            sourceRecordId = null,
                            sourceHash = sourceHash,
                            confidenceScore = 0.8,
                            isUserModified = false,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        // Apply Categorization Rules
                        entity = categorizationEngine.categorize(entity)
                        
                        try {
                            transactionRepository.insertTransaction(entity)
                            Log.d("SmsReceiver", "Transaction saved as DRAFT/NEEDS_REVIEW.")
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Transaction might be a duplicate: ${e.message}")
                        }
                    } else {
                        Log.d("SmsReceiver", "Ignored SMS - no parser matched.")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
