package com.nk.expensetracker.domain.categorization

import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.repository.CategorizationRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationEngine @Inject constructor(
    private val ruleRepository: CategorizationRuleRepository
) {
    /**
     * Applies categorization rules to the given transaction.
     * Returns a modified copy of the transaction with categoryId and merchantId set if a rule matched.
     */
    suspend fun categorize(transaction: TransactionEntity): TransactionEntity {
        // Only categorize if not already categorized by user
        if (transaction.categoryId != null && transaction.isUserModified) {
            return transaction
        }

        val rules = ruleRepository.getAllRulesSync()
        
        for (rule in rules) {
            if (matchesRule(transaction, rule)) {
                return transaction.copy(
                    categoryId = rule.targetCategoryId,
                    merchantId = rule.targetMerchantId ?: transaction.merchantId,
                    destinationAccountId = rule.targetAccountId ?: transaction.destinationAccountId,
                    // If we have a very specific rule, we might increase confidence score
                    confidenceScore = 0.95
                )
            }
        }
        
        return transaction
    }

    private fun matchesRule(tx: TransactionEntity, rule: CategorizationRuleEntity): Boolean {
        val targetText = when (rule.matchField) {
            "merchant" -> tx.counterpartyName ?: tx.merchantId ?: ""
            "note", "description" -> tx.note ?: tx.description ?: ""
            "upi" -> tx.upiReference ?: ""
            else -> ""
        }
        
        if (targetText.isBlank()) return false
        
        val regex = Regex(rule.pattern, RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(targetText)
    }
}
