package com.nk.expensetracker.domain.repository

import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import kotlinx.coroutines.flow.Flow

interface CategorizationRuleRepository {
    fun getAllRules(): Flow<List<CategorizationRuleEntity>>
    suspend fun getAllRulesSync(): List<CategorizationRuleEntity>
    suspend fun getRuleById(id: String): CategorizationRuleEntity?
    suspend fun insertRule(rule: CategorizationRuleEntity)
    suspend fun updateRule(rule: CategorizationRuleEntity)
    suspend fun deleteRule(rule: CategorizationRuleEntity)
}
