package com.nk.expensetracker.feature.notepadimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.domain.parser.NotepadParser
import com.nk.expensetracker.domain.repository.AccountRepository
import com.nk.expensetracker.domain.repository.TransactionRepository
import com.nk.expensetracker.domain.categorization.CategorizationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotepadImportUiState(
    val isBusy: Boolean = false,
    val text: String = "",
    val error: String? = null,
    val successCount: Int? = null
)

@HiltViewModel
class NotepadImportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categorizationEngine: CategorizationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotepadImportUiState())
    val uiState: StateFlow<NotepadImportUiState> = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(text = text, error = null, successCount = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(error = null, successCount = null) }
    }

    fun importText() {
        val textToParse = _uiState.value.text
        if (textToParse.isBlank()) {
            _uiState.update { it.copy(error = "Please enter some text") }
            return
        }

        _uiState.update { it.copy(isBusy = true, error = null) }

        viewModelScope.launch {
            try {
                val parsed = NotepadParser.parse(textToParse)
                if (parsed.isEmpty()) {
                    _uiState.update { it.copy(isBusy = false, error = "No transactions found in text") }
                    return@launch
                }

                val accounts = accountRepository.getAllAccounts().first()
                val accountId = accounts.firstOrNull()?.id ?: return@launch

                var count = 0
                for (tx in parsed) {
                    val withAccount = tx.copy(accountId = accountId)
                    val categorized = categorizationEngine.categorize(withAccount)
                    transactionRepository.insertTransaction(categorized)
                    count++
                }

                _uiState.update { it.copy(isBusy = false, successCount = count, text = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, error = "Failed: " + e.message) }
            }
        }
    }
}
