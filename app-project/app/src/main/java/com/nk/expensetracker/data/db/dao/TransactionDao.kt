package com.nk.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY occurredAt DESC")
    fun getTransactionsForAccount(accountId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY occurredAt DESC")
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :startDate AND occurredAt <= :endDate ORDER BY occurredAt DESC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :startDate AND occurredAt <= :endDate")
    suspend fun getTransactionsInRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT sourceHash FROM transactions WHERE occurredAt >= :startDate AND occurredAt <= :endDate AND sourceHash IS NOT NULL")
    suspend fun getSourceHashesInRange(startDate: Long, endDate: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
