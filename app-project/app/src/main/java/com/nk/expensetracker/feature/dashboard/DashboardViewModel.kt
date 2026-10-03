package com.nk.expensetracker.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import com.nk.expensetracker.domain.repository.CategoryRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One row of the "where did the money go" breakdown. */
data class CategorySpend(
    val categoryId: String?,
    val categoryName: String,
    val amountPaise: Long,
    val fraction: Float
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val currentMonthExpenses: Long = 0L,
    val currentMonthIncome: Long = 0L,
    val netCashFlow: Long = 0L,
    val transactionCount: Int = 0,
    val reviewCount: Int = 0,
    val topCategories: List<CategorySpend> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList()
) {
    val hasSpending: Boolean get() = currentMonthExpenses > 0
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val month = TimeUtils.currentMonth()
    private val startOfMonth = TimeUtils.startOfMonth(month)
    private val endOfMonth = TimeUtils.endOfMonth(month)

    private val currentMonthTransactions = transactionRepository.getTransactionsBetweenDates(
        startOfMonth,
        endOfMonth
    )

    private val reviewTransactions = transactionRepository.getTransactionsByStatus(
        TransactionStatus.NEEDS_REVIEW
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        currentMonthTransactions,
        reviewTransactions,
        categoryRepository.getAllCategories()
    ) { monthTx, reviewTx, categories ->
        val categoryNames = categories.associate { it.id to it.name }

        var expenses = 0L
        var income = 0L
        val spendByCategory = mutableMapOf<String?, Long>()

        monthTx.forEach { tx ->
            // Only confirmed rows move the totals. Refunds reduce the category
            // they came back from instead of being counted as fresh income.
            if (tx.status == TransactionStatus.CONFIRMED) {
                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        expenses += tx.amountPaise
                        spendByCategory.merge(tx.categoryId, tx.amountPaise, Long::plus)
                    }

                    TransactionType.INCOME -> income += tx.amountPaise
                    TransactionType.REFUND -> {
                        expenses -= tx.amountPaise
                        spendByCategory.merge(tx.categoryId, -tx.amountPaise, Long::plus)
                    }

                    TransactionType.TRANSFER -> Unit
                }
            }
        }

        val topCategories = spendByCategory
            .filterValues { it > 0 }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { (id, amount) ->
                CategorySpend(
                    categoryId = id,
                    categoryName = categoryNames[id] ?: "Uncategorized",
                    amountPaise = amount,
                    fraction = if (expenses > 0) amount.toFloat() / expenses else 0f
                )
            }

        DashboardUiState(
            isLoading = false,
            monthLabel = TimeUtils.formatMonth(startOfMonth),
            currentMonthExpenses = expenses,
            currentMonthIncome = income,
            netCashFlow = income - expenses,
            transactionCount = monthTx.count { it.status == TransactionStatus.CONFIRMED },
            reviewCount = reviewTx.size,
            topCategories = topCategories,
            recentTransactions = monthTx
                .filter { it.status == TransactionStatus.CONFIRMED }
                .sortedByDescending { it.occurredAt }
                .take(6)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}