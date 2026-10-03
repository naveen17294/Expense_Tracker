package com.nk.expensetracker.domain.model

import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.data.db.entity.CategoryEntity
import com.nk.expensetracker.data.db.entity.MerchantEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val merchants: List<MerchantEntity>,
    val rules: List<CategorizationRuleEntity>,
    val transactions: List<TransactionEntity>
)
