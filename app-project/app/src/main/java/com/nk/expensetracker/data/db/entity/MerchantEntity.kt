package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val canonicalName: String,
    val aliases: String, // Comma separated aliases
    val defaultCategoryId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
