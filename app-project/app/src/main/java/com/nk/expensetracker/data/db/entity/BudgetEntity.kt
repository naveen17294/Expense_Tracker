package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val yearMonth: String, // e.g. "2026-10"
    val categoryId: String?, // null = overall budget
    val limitPaise: Long,
    val rolloverPreference: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
