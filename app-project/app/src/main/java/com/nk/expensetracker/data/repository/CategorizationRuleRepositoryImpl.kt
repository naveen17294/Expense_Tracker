package com.nk.expensetracker.data.repository

import com.nk.expensetracker.data.db.dao.CategorizationRuleDao
import com.nk.expensetracker.data.db.entity.CategorizationRuleEntity
import com.nk.expensetracker.domain.repository.CategorizationRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategorizationRuleRepositoryImpl @Inject constructor(
    private val ruleDao: CategorizationRuleDao
) : CategorizationRuleRepository {
    override fun getAllRules(): Flow<List<CategorizationRuleEntity>> = ruleDao.getAllRules()
    override suspend fun getAllRulesSync(): List<CategorizationRuleEntity> = ruleDao.getAllRulesSync()
    override suspend fun getRuleById(id: String): CategorizationRuleEntity? = ruleDao.getRuleById(id)
    override suspend fun insertRule(rule: CategorizationRuleEntity) = ruleDao.insertRule(rule)
    override suspend fun updateRule(rule: CategorizationRuleEntity) = ruleDao.updateRule(rule)
    override suspend fun deleteRule(rule: CategorizationRuleEntity) = ruleDao.deleteRule(rule)
}
