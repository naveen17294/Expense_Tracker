package com.nk.expensetracker.feature.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.DatePickerField
import com.nk.expensetracker.core.designsystem.components.DropdownSelector
import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Convert paise to a mutable string state for the text field
    var amountText by remember(uiState.amountPaise) { 
        mutableStateOf(if (uiState.amountPaise > 0) (uiState.amountPaise / 100.0).toString() else "") 
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            viewModel.onAmountChanged(Money.rupeesToPaise(amount))
                            viewModel.saveTransaction()
                        },
                        enabled = amountText.isNotBlank() && uiState.accountId != null
                    ) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            // Loading state
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.values().forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChanged(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.values().size)
                    ) {
                        Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    // Basic validation for amount input
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                        amountText = newValue
                        val amount = newValue.toDoubleOrNull() ?: 0.0
                        viewModel.onAmountChanged(Money.rupeesToPaise(amount))
                    }
                },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium
            )

            // Merchant / Payee
            OutlinedTextField(
                value = uiState.merchantName,
                onValueChange = viewModel::onMerchantNameChanged,
                label = { Text(if (uiState.type == TransactionType.INCOME) "Sender" else "Merchant / Payee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Note
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Account Dropdown
            val selectedAccount = uiState.accounts.find { it.id == uiState.accountId }
            DropdownSelector(
                label = "Account",
                items = uiState.accounts,
                selectedItem = selectedAccount,
                onItemSelected = { viewModel.onAccountChanged(it.id) },
                itemLabel = { it.name }
            )

            // Destination Account (for Transfers)
            if (uiState.type == TransactionType.TRANSFER) {
                val selectedDestination = uiState.accounts.find { it.id == uiState.destinationAccountId }
                DropdownSelector(
                    label = "Destination Account",
                    items = uiState.accounts,
                    selectedItem = selectedDestination,
                    onItemSelected = { viewModel.onDestinationAccountChanged(it.id) },
                    itemLabel = { it.name }
                )
            }

            // Category Dropdown
            val selectedCategory = uiState.categories.find { it.id == uiState.categoryId }
            DropdownSelector(
                label = "Category",
                items = uiState.categories,
                selectedItem = selectedCategory,
                onItemSelected = { viewModel.onCategoryChanged(it.id) },
                itemLabel = { it.name }
            )

            // Date/Time Picker
            DatePickerField(
                dateMillis = uiState.dateMillis,
                onDateSelected = viewModel::onDateChanged
            )
        }
    }
}
