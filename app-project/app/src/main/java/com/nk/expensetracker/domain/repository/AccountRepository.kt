package com.nk.expensetracker.domain.repository

import com.nk.expensetracker.data.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    fun getActiveAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: String): AccountEntity?
    suspend fun insertAccount(account: AccountEntity)
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
}
