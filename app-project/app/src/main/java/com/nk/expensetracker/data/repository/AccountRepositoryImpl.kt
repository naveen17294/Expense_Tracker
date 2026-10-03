package com.nk.expensetracker.data.repository

import com.nk.expensetracker.data.db.dao.AccountDao
import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    override fun getActiveAccounts(): Flow<List<AccountEntity>> = accountDao.getActiveAccounts()
    override suspend fun getAccountById(id: String): AccountEntity? = accountDao.getAccountById(id)
    override suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    override suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    override suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
}
