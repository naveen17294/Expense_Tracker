package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "import_records")
data class ImportRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sourceHash: String, // SHA-256 of the raw message/row
    val sender: String,
    val receivedTime: Long,
    val parserVersion: Int,
    val outcome: String, // PARSED, REJECTED, DUPLICATE
    val redactedText: String?, // For debugging only
    val createdAt: Long
)
