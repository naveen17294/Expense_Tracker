package com.nk.expensetracker.feature.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.AmountField
import com.nk.expensetracker.core.designsystem.components.DatePickerField
import com.nk.expensetracker.core.designsystem.components.DropdownSelector
import com.nk.expensetracker.core.designsystem.components.QuickChoice
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.domain.model.TransactionType

private fun TransactionType.label(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.REFUND -> "Refund"
}

private fun TransactionType.icon(): ImageVector = when (this) {
    TransactionType.EXPENSE -> Icons.Rounded.ArrowUpward
    TransactionType.INCOME -> Icons.Rounded.ArrowDownward
    TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
    TransactionType.REFUND -> Icons.Rounded.CallReceived
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Held as text so a partially typed amount ("12.", "12.4") is not truncated.
    var amountText by remember(uiState.amountPaise) {
        mutableStateOf(
            if (uiState.amountPaise > 0) {
                Money.paiseToRupees(uiState.amountPaise).toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var showValidation by remember { mutableStateOf(false) }

    val parsedAmount = Money.parseRupeesToPaise(amountText)
    val amountIsValid = parsedAmount != null && parsedAmount > 0
    val canSave = amountIsValid && uiState.accountId != null

    LaunchedEffect(parsedAmount) {
        if (parsedAmount != null) viewModel.onAmountChanged(parsedAmount)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing()) "Edit transaction" else "Add transaction")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            showValidation = true
                            if (canSave) viewModel.saveTransaction()
                        }
                    ) { Text("Save") }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = AppTheme.spacing.gutter,
                vertical = AppTheme.spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)
        ) {
            item(key = "type") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    items(TransactionType.entries.toList()) { type ->
                        QuickChoice(
                            label = type.label(),
                            selected = uiState.type == type,
                            onClick = { viewModel.onTypeChanged(type) }
                        )
                    }
                }
            }

            item(key = "amount") {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    AmountField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        isError = showValidation && !amountIsValid
                    )
                    if (showValidation && !amountIsValid) {
                        Text(
                            text = "Enter an amount greater than zero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item(key = "payee") {
                FormSection(title = "Details") {
                    OutlinedTextField(
                        value = uiState.merchantName,
                        onValueChange = viewModel::onMerchantNameChanged,
                        label = {
                            Text(
                                if (uiState.type == TransactionType.INCOME) "Received from"
                                else if (uiState.type == TransactionType.TRANSFER) "Transferred to"
                                else "Paid to"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.type == TransactionType.INCOME) {
                                    Icons.Rounded.CallMade
                                } else {
                                    Icons.Rounded.CallReceived
                                },
                                contentDescription = null
                            )
                        }
                    )

                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChanged,
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            item(key = "classification") {
                FormSection(title = "Category & account") {
                    DropdownSelector(
                        label = "Category",
                        items = uiState.categories,
                        selectedItem = uiState.categories.find { it.id == uiState.categoryId },
                        onItemSelected = { viewModel.onCategoryChanged(it.id) },
                        itemLabel = { it.name }
                    )

                    DropdownSelector(
                        label = if (uiState.type == TransactionType.INCOME) "Deposited into" else "Paid from",
                        items = uiState.accounts,
                        selectedItem = uiState.accounts.find { it.id == uiState.accountId },
                        onItemSelected = { viewModel.onAccountChanged(it.id) },
                        itemLabel = { account ->
                            account.maskedIdentifier?.let { "${account.name} •$it" } ?: account.name
                        }
                    )

                    if (uiState.type == TransactionType.TRANSFER) {
                        DropdownSelector(
                            label = "Transfer to",
                            items = uiState.accounts.filter { it.id != uiState.accountId },
                            selectedItem = uiState.accounts.find { it.id == uiState.destinationAccountId },
                            onItemSelected = { viewModel.onDestinationAccountChanged(it.id) },
                            itemLabel = { account ->
                                account.maskedIdentifier?.let { "${account.name} •$it" } ?: account.name
                            }
                        )
                    }
                }
            }

            item(key = "when") {
                FormSection(title = "When") {
                    DatePickerField(
                        dateMillis = uiState.dateMillis,
                        onDateSelected = viewModel::onDateChanged
                    )
                }
            }

            item(key = "save") {
                Button(
                    onClick = {
                        showValidation = true
                        if (canSave) viewModel.saveTransaction()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = if (uiState.isEditing()) "Save changes" else "Save transaction",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(AppTheme.spacing.xl)) }
        }
    }
}

/** Groups related inputs under a small heading inside a tonal container. */
@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

private fun AddEditTransactionUiState.isEditing(): Boolean =
    amountPaise > 0 || note.isNotBlank() || merchantName.isNotBlank()