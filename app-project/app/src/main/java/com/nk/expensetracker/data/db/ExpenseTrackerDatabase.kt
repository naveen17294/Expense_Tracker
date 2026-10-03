package com.nk.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nk.expensetracker.data.db.dao.AccountDao
import com.nk.expensetracker.data.db.dao.CategorizationRuleDao
import com.nk.expensetracker.data.db.dao.CategoryDao
import com.nk.expensetracker.data.db.dao.ImportRecordDao
import com.nk.expensetracker.data.db.dao.MerchantDao
import com.nk.expensetracker.data.db.dao.TransactionDao
import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.data.db.entity.BudgetEntity
import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.data.db.entity.CategoryEntity
import com.nk.expensetracker.data.db.entity.ImportRecordEntity
import com.nk.expensetracker.data.db.entity.MerchantEntity
import com.nk.expensetracker.data.db.entity.TagEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.data.db.entity.TransactionTagCrossRef

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        CategorizationRuleEntity::class,
        ImportRecordEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ExpenseTrackerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun categorizationRuleDao(): CategorizationRuleDao
    abstract fun importRecordDao(): ImportRecordDao
}
