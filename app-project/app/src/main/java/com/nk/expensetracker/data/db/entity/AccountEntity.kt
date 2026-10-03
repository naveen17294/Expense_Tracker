package com.nk.expensetracker.data.db.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey
import java.util.UUID

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // bank, credit card, wallet, cash
    val institutionName: String?,
    val maskedIdentifier: String?, // last four digits only
    val openingBalancePaise: Long,
    val currencyCode: String,
    val isOwned: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
