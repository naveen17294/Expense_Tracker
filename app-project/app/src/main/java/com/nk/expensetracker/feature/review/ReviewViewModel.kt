package com.nk.expensetracker.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = true,
    val pendingTransactions: List<TransactionEntity> = emptyList()
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<ReviewUiState> = transactionRepository
        .getTransactionsByStatus(TransactionStatus.NEEDS_REVIEW)
        .map { transactions ->
            ReviewUiState(
                isLoading = false,
                pendingTransactions = transactions
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReviewUiState(isLoading = true)
        )

    fun approveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val updated = transaction.copy(
                status = TransactionStatus.CONFIRMED,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.updateTransaction(updated)
        }
    }

    fun rejectTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val updated = transaction.copy(
                status = TransactionStatus.EXCLUDED,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.updateTransaction(updated)
        }
    }
}
