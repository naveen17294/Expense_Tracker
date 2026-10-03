package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long
)

@Serializable
@Entity(
    tableName = "transaction_tag_cross_ref",
    primaryKeys = ["transactionId", "tagId"]
)
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String
)
