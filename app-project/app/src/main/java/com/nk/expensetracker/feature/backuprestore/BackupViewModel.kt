package com.nk.expensetracker.feature.backuprestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createBackup(uri: Uri) {
        _uiState.update { it.copy(isLoading = true, message = "Creating backup...") }
        viewModelScope.launch {
            val result = backupManager.createBackup(uri)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, message = "Backup successful!", isError = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, message = "Backup failed: ${result.exceptionOrNull()?.message}", isError = true) }
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        _uiState.update { it.copy(isLoading = true, message = "Restoring backup...") }
        viewModelScope.launch {
            val result = backupManager.restoreBackup(uri)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, message = "Restore successful!", isError = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, message = "Restore failed: ${result.exceptionOrNull()?.message}", isError = true) }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null, isError = false) }
    }
}
