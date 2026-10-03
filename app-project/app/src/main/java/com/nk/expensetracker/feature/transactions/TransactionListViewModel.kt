package com.nk.expensetracker.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import com.nk.expensetracker.domain.repository.CategoryRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

/** Transactions that happened on the same calendar day, newest day first. */
data class TransactionGroup(
    val dateMillis: Long,
    val label: String,
    val transactions: List<TransactionEntity>,
    val spentPaise: Long
)

data class TransactionListUiState(
    val isLoading: Boolean = true,
    val groups: List<TransactionGroup> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val categoryNames: Map<String, String> = emptyMap(),
    val matchedCount: Int = 0,
    val netOutflowPaise: Long = 0
) {
    val isFiltered: Boolean get() = searchQuery.isNotBlank() || typeFilter != null
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

/**
 * Owns the list, the search text, and the type filter, and pre-groups results by
 * day so the screen only has to render.
 */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.getAllTransactions(),
        searchQuery,
        typeFilter,
        categoryRepository.getAllCategories()
    ) { all, query, filter, categories ->
        val categoryNames = categories.associate { it.id to it.name }

        val visible = all.asSequence()
            .filter { it.status != TransactionStatus.DELETED }
            .filter { filter == null || it.type == filter }
            .filter { tx -> query.isBlank() || tx.matches(query) }
            .sortedByDescending { it.occurredAt }
            .toList()

        val today = LocalDate.now(TimeUtils.IST)
        val groups = visible
            .groupBy { TimeUtils.millisToLocalDate(it.occurredAt) }
            .toSortedMap(reverseOrder())
            .map { (date, transactions) ->
                TransactionGroup(
                    dateMillis = transactions.maxOf { it.occurredAt },
                    label = TimeUtils.formatDayHeader(transactions.maxOf { it.occurredAt }, today),
                    transactions = transactions,
                    spentPaise = transactions
                        .filter { it.status == TransactionStatus.CONFIRMED }
                        .sumOf { tx ->
                            when (tx.type) {
                                TransactionType.EXPENSE -> tx.amountPaise
                                TransactionType.REFUND -> -tx.amountPaise
                                else -> 0L
                            }
                        }
                )
            }

        TransactionListUiState(
            isLoading = false,
            groups = groups,
            searchQuery = query,
            typeFilter = filter,
            categoryNames = categoryNames,
            matchedCount = visible.size,
            netOutflowPaise = groups.sumOf { it.spentPaise }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.update { query }
    }

    fun onTypeFilterChanged(type: TransactionType?) {
        typeFilter.update { current -> if (current == type) null else type }
    }

    fun clearFilters() {
        searchQuery.value = ""
        typeFilter.value = null
    }
}

private fun TransactionEntity.matches(query: String): Boolean {
    val needle = query.trim()
    return counterpartyName?.contains(needle, ignoreCase = true) == true ||
        merchantId?.contains(needle, ignoreCase = true) == true ||
        note?.contains(needle, ignoreCase = true) == true ||
        description?.contains(needle, ignoreCase = true) == true ||
        upiReference?.contains(needle, ignoreCase = true) == true ||
        type.name.contains(needle, ignoreCase = true) ||
        com.nk.expensetracker.core.money.Money.formatPaiseTrimmed(amountPaise)
            .contains(needle, ignoreCase = true)
}