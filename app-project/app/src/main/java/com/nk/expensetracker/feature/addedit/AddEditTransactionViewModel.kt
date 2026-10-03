package com.nk.expensetracker.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.data.db.entity.CategoryEntity
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType
import com.nk.expensetracker.domain.repository.AccountRepository
import com.nk.expensetracker.domain.repository.CategoryRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditTransactionUiState(
    val isLoading: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountPaise: Long = 0L,
    val dateMillis: Long = TimeUtils.nowMillis(),
    val accountId: String? = null,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val merchantName: String = "",
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isSaved: Boolean = false
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String? = savedStateHandle["transactionId"]

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())

    val uiState: StateFlow<AddEditTransactionUiState> = combine(
        _uiState,
        accountRepository.getActiveAccounts(),
        categoryRepository.getAllCategories()
    ) { state, accounts, categories ->
        state.copy(
            accounts = accounts,
            categories = categories,
            // Pre-select GPay by default, fallback to first account if not found
            accountId = state.accountId ?: accounts.find { it.name == "GPay" }?.id ?: accounts.firstOrNull()?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditTransactionUiState(isLoading = true)
    )

    init {
        if (transactionId != null) {
            loadTransaction(transactionId)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tx = transactionRepository.getTransactionById(id)
            if (tx != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        type = tx.type,
                        amountPaise = tx.amountPaise,
                        dateMillis = tx.occurredAt,
                        accountId = tx.accountId,
                        destinationAccountId = tx.destinationAccountId,
                        categoryId = tx.categoryId,
                        merchantName = tx.counterpartyName ?: tx.merchantId ?: "",
                        note = tx.note ?: ""
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTypeChanged(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onAmountChanged(amountPaise: Long) {
        _uiState.update { it.copy(amountPaise = amountPaise) }
    }

    fun onDateChanged(dateMillis: Long) {
        _uiState.update { it.copy(dateMillis = dateMillis) }
    }

    fun onAccountChanged(accountId: String) {
        _uiState.update { it.copy(accountId = accountId) }
    }

    fun onDestinationAccountChanged(accountId: String?) {
        _uiState.update { it.copy(destinationAccountId = accountId) }
    }

    fun onCategoryChanged(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onMerchantNameChanged(name: String) {
        _uiState.update { it.copy(merchantName = name) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        
        // Basic validation
        if (currentState.accountId == null || currentState.amountPaise <= 0L) {
            return // Show error in UI ideally
        }

        viewModelScope.launch {
            val tx = TransactionEntity(
                id = transactionId ?: UUID.randomUUID().toString(),
                type = currentState.type,
                status = TransactionStatus.CONFIRMED, // Manual entries are confirmed
                amountPaise = currentState.amountPaise,
                currencyCode = "INR", // Default
                occurredAt = currentState.dateMillis,
                accountId = currentState.accountId,
                destinationAccountId = if (currentState.type == TransactionType.TRANSFER) currentState.destinationAccountId else null,
                merchantId = null, // We'd ideally look this up or create one based on merchantName
                categoryId = currentState.categoryId,
                counterpartyName = currentState.merchantName.takeIf { it.isNotBlank() },
                description = null,
                note = currentState.note.takeIf { it.isNotBlank() },
                paymentMethod = null,
                upiReference = null,
                bankReference = null,
                parentTransactionId = null,
                source = TransactionSource.MANUAL,
                sourceRecordId = null,
                sourceHash = null,
                confidenceScore = null,
                isUserModified = true,
                createdAt = TimeUtils.nowMillis(),
                updatedAt = TimeUtils.nowMillis()
            )

            if (transactionId == null) {
                transactionRepository.insertTransaction(tx)
            } else {
                transactionRepository.updateTransaction(tx)
            }
            
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
