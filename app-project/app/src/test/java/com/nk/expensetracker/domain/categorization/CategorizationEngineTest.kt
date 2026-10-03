package com.nk.expensetracker.domain.categorization

import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import com.nk.expensetracker.domain.repository.CategorizationRuleRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class CategorizationEngineTest {

    private val ruleRepository = mockk<CategorizationRuleRepository>()
    private val engine = CategorizationEngine(ruleRepository)

    @Test
    fun `categorize applies matching merchant rule`() = runTest {
        val foodRule = CategorizationRuleEntity(
            id = "rule1",
            priority = 10,
            matchField = "merchant",
            pattern = "zomato|swiggy",
            targetCategoryId = "food_cat",
            targetMerchantId = null,
            targetAccountId = null,
            createdAt = 0,
            updatedAt = 0
        )
        
        coEvery { ruleRepository.getAllRulesSync() } returns listOf(foodRule)

        val tx = createTestTransaction(merchantName = "Swiggy Instamart")
        
        val result = engine.categorize(tx)
        
        assertEquals("food_cat", result.categoryId)
        assertEquals(0.95, result.confidenceScore ?: 0.0, 0.01)
    }

    @Test
    fun `categorize ignores rule if already user modified`() = runTest {
        val foodRule = CategorizationRuleEntity(
            id = "rule1",
            priority = 10,
            matchField = "merchant",
            pattern = "zomato",
            targetCategoryId = "food_cat",
            targetMerchantId = null,
            targetAccountId = null,
            createdAt = 0,
            updatedAt = 0
        )
        
        coEvery { ruleRepository.getAllRulesSync() } returns listOf(foodRule)

        val tx = createTestTransaction(merchantName = "zomato").copy(
            categoryId = "existing_cat",
            isUserModified = true
        )
        
        val result = engine.categorize(tx)
        
        // Should not apply rule
        assertEquals("existing_cat", result.categoryId)
    }

    @Test
    fun `categorize ignores non-matching rules`() = runTest {
        val foodRule = CategorizationRuleEntity(
            id = "rule1",
            priority = 10,
            matchField = "merchant",
            pattern = "zomato",
            targetCategoryId = "food_cat",
            targetMerchantId = null,
            targetAccountId = null,
            createdAt = 0,
            updatedAt = 0
        )
        
        coEvery { ruleRepository.getAllRulesSync() } returns listOf(foodRule)

        val tx = createTestTransaction(merchantName = "Uber")
        
        val result = engine.categorize(tx)
        
        assertNull(result.categoryId)
    }

    private fun createTestTransaction(merchantName: String): TransactionEntity {
        return TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.EXPENSE,
            status = TransactionStatus.CONFIRMED,
            amountPaise = 10000,
            currencyCode = "INR",
            occurredAt = 0,
            accountId = "acc1",
            destinationAccountId = null,
            merchantId = null,
            categoryId = null,
            counterpartyName = merchantName,
            description = null,
            note = null,
            paymentMethod = null,
            upiReference = null,
            bankReference = null,
            parentTransactionId = null,
            source = TransactionSource.MANUAL,
            sourceRecordId = null,
            sourceHash = null,
            confidenceScore = 1.0,
            isUserModified = false,
            createdAt = 0,
            updatedAt = 0
        )
    }
}
