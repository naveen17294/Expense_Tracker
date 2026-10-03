package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import java.util.UUID

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId", "occurredAt"]),
        Index(value = ["categoryId", "occurredAt"]),
        Index(value = ["occurredAt"]),
        Index(value = ["status"]),
        Index(value = ["sourceHash"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val status: TransactionStatus,
    val amountPaise: Long,
    val currencyCode: String,
    val occurredAt: Long,
    val accountId: String,
    val destinationAccountId: String?, // for transfers
    val merchantId: String?,
    val categoryId: String?,
    val counterpartyName: String?,
    val description: String?,
    val note: String?,
    val paymentMethod: String?, // UPI, card, wallet, cash, bank transfer
    val upiReference: String?,
    val bankReference: String?,
    val parentTransactionId: String?, // for refunds
    val source: TransactionSource,
    val sourceRecordId: String?,
    val sourceHash: String?, // For deduplication
    val confidenceScore: Double?,
    val isUserModified: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
