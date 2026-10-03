package com.nk.expensetracker.data.repository

import com.nk.expensetracker.data.db.dao.CategoryDao
import com.nk.expensetracker.data.db.entity.CategoryEntity
import com.nk.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    override suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)
    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)
    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)
    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}
