package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "categorization_rules")
data class CategorizationRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val priority: Int,
    val matchField: String, // e.g., "merchant", "description", "upiReference"
    val pattern: String,
    val targetCategoryId: String?,
    val targetMerchantId: String?,
    val targetAccountId: String?, // For transfers
    val createdAt: Long,
    val updatedAt: Long
)
