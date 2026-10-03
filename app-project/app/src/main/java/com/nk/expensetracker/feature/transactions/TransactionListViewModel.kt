package com.nk.expensetracker.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TransactionListUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery
    ) { allTransactions, query ->
        val filtered = if (query.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter { tx ->
                (tx.counterpartyName?.contains(query, ignoreCase = true) == true) ||
                (tx.merchantId?.contains(query, ignoreCase = true) == true) ||
                (tx.note?.contains(query, ignoreCase = true) == true) ||
                (tx.amountPaise.toString().contains(query))
            }
        }

        TransactionListUiState(
            isLoading = false,
            transactions = filtered,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }
}
