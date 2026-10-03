package com.nk.expensetracker.domain.repository

import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: String): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): String
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
