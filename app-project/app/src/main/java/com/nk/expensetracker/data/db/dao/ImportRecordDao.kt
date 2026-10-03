package com.nk.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nk.expensetracker.data.db.entity.ImportRecordEntity

/**
 * Audit trail for every attempted import.
 *
 * Rows are written whether or not a transaction was created, so "why did my
 * statement not import?" is answerable after the fact.
 */
@Dao
interface ImportRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportRecord(record: ImportRecordEntity)

    @Query("SELECT * FROM import_records ORDER BY createdAt DESC")
    suspend fun getAllImportRecords(): List<ImportRecordEntity>

    @Query("SELECT COUNT(*) FROM import_records WHERE sourceHash = :sourceHash AND outcome != 'DUPLICATE'")
    suspend fun countSuccessfulImports(sourceHash: String): Int

    @Query("SELECT * FROM import_records WHERE outcome = 'DUPLICATE' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentDuplicates(limit: Int = 50): List<ImportRecordEntity>

    @Query("SELECT COUNT(*) FROM import_records")
    suspend fun count(): Int
}