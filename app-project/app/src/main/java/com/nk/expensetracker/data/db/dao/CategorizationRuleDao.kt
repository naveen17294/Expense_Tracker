package com.nk.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {
    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRules(): Flow<List<CategorizationRuleEntity>>

    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, createdAt DESC")
    suspend fun getAllRulesSync(): List<CategorizationRuleEntity>

    @Query("SELECT * FROM categorization_rules WHERE id = :id")
    suspend fun getRuleById(id: String): CategorizationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRuleEntity)

    @Update
    suspend fun updateRule(rule: CategorizationRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CategorizationRuleEntity)
}
