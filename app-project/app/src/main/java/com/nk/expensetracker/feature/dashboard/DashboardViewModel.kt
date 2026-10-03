package com.nk.expensetracker.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val currentMonthExpenses: Long = 0L,
    val currentMonthIncome: Long = 0L,
    val netCashFlow: Long = 0L,
    val reviewCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    private val currentMonth = TimeUtils.currentMonth()
    private val startOfMonth = TimeUtils.startOfMonth(currentMonth)
    private val endOfMonth = TimeUtils.endOfMonth(currentMonth)

    private val currentMonthTransactions = transactionRepository.getTransactionsBetweenDates(
        startOfMonth, 
        endOfMonth
    )

    private val reviewTransactions = transactionRepository.getTransactionsByStatus(
        TransactionStatus.NEEDS_REVIEW
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        currentMonthTransactions,
        reviewTransactions
    ) { monthTx, reviewTx ->
        
        var expenses = 0L
        var income = 0L

        monthTx.forEach { tx ->
            // Only count confirmed transactions for totals
            if (tx.status == TransactionStatus.CONFIRMED) {
                when (tx.type) {
                    TransactionType.EXPENSE -> expenses += tx.amountPaise
                    TransactionType.INCOME -> income += tx.amountPaise
                    TransactionType.REFUND -> expenses -= tx.amountPaise // Refund reduces expense
                    TransactionType.TRANSFER -> {} // Transfers don't affect net flow
                }
            }
        }

        DashboardUiState(
            isLoading = false,
            currentMonthExpenses = expenses,
            currentMonthIncome = income,
            netCashFlow = income - expenses,
            reviewCount = reviewTx.size,
            recentTransactions = monthTx.take(5) // Show top 5 recent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
