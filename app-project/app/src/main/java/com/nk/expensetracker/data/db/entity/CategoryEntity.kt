package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String?,
    val colorHex: String?,
    val parentCategoryId: String?,
    val isExpense: Boolean,
    val isIncome: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
