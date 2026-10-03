package com.nk.expensetracker.feature.csvimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.data.csv.StatementImportManager
import com.nk.expensetracker.data.db.entity.AccountEntity
import com.nk.expensetracker.domain.csv.StatementImportOutcome
import com.nk.expensetracker.domain.csv.StatementImportPreview
import com.nk.expensetracker.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CsvImportUiState(
    val isBusy: Boolean = false,
    val fileName: String? = null,
    val preview: StatementImportPreview? = null,
    val outcome: StatementImportOutcome? = null,
    val error: String? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val selectedAccountId: String? = null,
    val countCreditsAsIncome: Boolean = true
) {
    val canCommit: Boolean
        get() = !isBusy && preview != null && preview.newCount > 0 && selectedAccountId != null
}

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val importManager: StatementImportManager,
    accountRepository: AccountRepository
) : ViewModel() {

    private val transient = MutableStateFlow(CsvImportUiState())

    val uiState: StateFlow<CsvImportUiState> = combine(
        transient,
        accountRepository.getActiveAccounts()
    ) { state, accounts ->
        state.copy(
            accounts = accounts,
            selectedAccountId = state.selectedAccountId ?: accounts.firstOrNull()?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CsvImportUiState()
    )

    fun onFileSelected(uri: Uri, displayName: String?) {
        val accountId = uiState.value.selectedAccountId ?: return
        transient.update {
            it.copy(
                isBusy = true,
                error = null,
                outcome = null,
                preview = null,
                fileName = displayName
            )
        }

        viewModelScope.launch {
            importManager
                .preview(uri, accountId, uiState.value.countCreditsAsIncome)
                .onSuccess { preview ->
                    transient.update {
                        it.copy(isBusy = false, preview = preview, fileName = displayName)
                    }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            isBusy = false,
                            preview = null,
                            error = error.message ?: "That file could not be read."
                        )
                    }
                }
        }
    }

    fun onAccountSelected(accountId: String) {
        transient.update { it.copy(selectedAccountId = accountId) }
    }

    fun commit() {
        val state = uiState.value
        val preview = state.preview ?: return
        val accountId = state.selectedAccountId ?: return

        transient.update { it.copy(isBusy = true, error = null) }

        viewModelScope.launch {
            importManager.commit(preview.copy(accountId = accountId))
                .onSuccess { outcome ->
                    transient.update { it.copy(isBusy = false, outcome = outcome, preview = null) }
                }
                .onFailure { error ->
                    transient.update {
                        it.copy(
                            isBusy = false,
                            error = error.message ?: "The import could not be saved."
                        )
                    }
                }
        }
    }

    fun reset() {
        transient.update { CsvImportUiState() }
    }

    fun dismissError() {
        transient.update { it.copy(error = null) }
    }
}