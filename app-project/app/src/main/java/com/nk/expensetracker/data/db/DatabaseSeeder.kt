package com.nk.expensetracker.data.db

import android.util.Log
import com.nk.expensetracker.core.common.Constants
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.dao.AccountDao
import com.nk.expensetracker.data.db.dao.CategorizationRuleDao
import com.nk.expensetracker.data.db.dao.CategoryDao
import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val ruleDao: CategorizationRuleDao
) {
    suspend fun seedDatabaseIfNeeded() {
        try {
            val existingCategories = categoryDao.getAllCategories().first()
            if (existingCategories.isEmpty()) {
                seedCategories()
                Log.d("DatabaseSeeder", "Database seeded with initial categories.")
            } else {
                Log.d("DatabaseSeeder", "Categories already seeded. Skipping.")
            }

            val existingAccounts = accountDao.getAllAccounts().first()
            val accountNames = existingAccounts.map { it.name }
            val defaultAccountNames = listOf("GPay", "PhonePe", "CRED", "Cash", "Others")
            
            val missingAccounts = defaultAccountNames.filter { it !in accountNames }
            if (missingAccounts.isNotEmpty()) {
                val now = TimeUtils.nowMillis()
                missingAccounts.forEach { name ->
                    val account = AccountEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = if (name == "Cash") "cash" else "wallet",
                        institutionName = null,
                        maskedIdentifier = null,
                        openingBalancePaise = 0L,
                        currencyCode = Constants.DEFAULT_CURRENCY,
                        isOwned = true,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                    accountDao.insertAccount(account)
                }
                Log.d("DatabaseSeeder", "Seeded missing accounts: $missingAccounts")
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding database", e)
        }
    }

    private suspend fun seedCategories() {
        val now = TimeUtils.nowMillis()
        val defaultCategories = listOf(
            CategoryEntity(name = "Food & Dining", icon = "ic_food", colorHex = "#FF5722", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Groceries", icon = "ic_grocery", colorHex = "#4CAF50", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Transportation", icon = "ic_transport", colorHex = "#2196F3", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Shopping", icon = "ic_shopping", colorHex = "#9C27B0", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Entertainment", icon = "ic_movie", colorHex = "#E91E63", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Bills & Utilities", icon = "ic_bill", colorHex = "#607D8B", parentCategoryId = null, isExpense = true, isIncome = false, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Salary", icon = "ic_money", colorHex = "#4CAF50", parentCategoryId = null, isExpense = false, isIncome = true, createdAt = now, updatedAt = now),
            CategoryEntity(name = "Transfers", icon = "ic_transfer", colorHex = "#9E9E9E", parentCategoryId = null, isExpense = false, isIncome = false, createdAt = now, updatedAt = now)
        )

        defaultCategories.forEach { categoryDao.insertCategory(it) }

        // Seed some default Categorization Rules for these categories
        val foodId = defaultCategories.first { it.name == "Food & Dining" }.id
        val groceriesId = defaultCategories.first { it.name == "Groceries" }.id
        val transportId = defaultCategories.first { it.name == "Transportation" }.id
        
        val defaultRules = listOf(
            CategorizationRuleEntity(priority = 10, matchField = "merchant", pattern = "zomato|swiggy|eatclub|mcdonald|kfc", targetCategoryId = foodId, targetMerchantId = null, targetAccountId = null, createdAt = now, updatedAt = now),
            CategorizationRuleEntity(priority = 10, matchField = "merchant", pattern = "blinkit|zepto|instamart|swiggy instamart|bigbasket", targetCategoryId = groceriesId, targetMerchantId = null, targetAccountId = null, createdAt = now, updatedAt = now),
            CategorizationRuleEntity(priority = 10, matchField = "merchant", pattern = "uber|ola|rapido|namma yatri", targetCategoryId = transportId, targetMerchantId = null, targetAccountId = null, createdAt = now, updatedAt = now)
        )
        
        defaultRules.forEach { ruleDao.insertRule(it) }
    }

}
