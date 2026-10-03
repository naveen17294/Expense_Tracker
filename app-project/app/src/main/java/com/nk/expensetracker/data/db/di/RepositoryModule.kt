package com.nk.expensetracker.data.db.di

import com.nk.expensetracker.data.repository.AccountRepositoryImpl
import com.nk.expensetracker.data.repository.CategorizationRuleRepositoryImpl
import com.nk.expensetracker.data.repository.CategoryRepositoryImpl
import com.nk.expensetracker.data.repository.TransactionRepositoryImpl
import com.nk.expensetracker.domain.repository.AccountRepository
import com.nk.expensetracker.domain.repository.CategorizationRuleRepository
import com.nk.expensetracker.domain.repository.CategoryRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCategorizationRuleRepository(
        ruleRepositoryImpl: CategorizationRuleRepositoryImpl
    ): CategorizationRuleRepository
}
