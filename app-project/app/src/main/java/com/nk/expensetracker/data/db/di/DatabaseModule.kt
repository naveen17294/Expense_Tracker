package com.nk.expensetracker.data.db.di

import android.content.Context
import androidx.room.Room
import com.nk.expensetracker.core.common.Constants
import com.nk.expensetracker.data.db.ExpenseTrackerDatabase
import com.nk.expensetracker.data.db.dao.AccountDao
import com.nk.expensetracker.data.db.dao.CategorizationRuleDao
import com.nk.expensetracker.data.db.dao.CategoryDao
import com.nk.expensetracker.data.db.dao.MerchantDao
import com.nk.expensetracker.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideExpenseTrackerDatabase(
        @ApplicationContext context: Context
    ): ExpenseTrackerDatabase {
        return Room.databaseBuilder(
            context,
            ExpenseTrackerDatabase::class.java,
            Constants.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideAccountDao(database: ExpenseTrackerDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideTransactionDao(database: ExpenseTrackerDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: ExpenseTrackerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideMerchantDao(database: ExpenseTrackerDatabase): MerchantDao = database.merchantDao()

    @Provides
    fun provideCategorizationRuleDao(database: ExpenseTrackerDatabase): CategorizationRuleDao = database.categorizationRuleDao()
}
