package com.nk.expensetracker.data.repository

import com.nk.expensetracker.data.db.dao.TransactionDao
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }

    override fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    override fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByStatus(status)
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): String {
        transactionDao.insertTransaction(transaction)
        return transaction.id
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
}
