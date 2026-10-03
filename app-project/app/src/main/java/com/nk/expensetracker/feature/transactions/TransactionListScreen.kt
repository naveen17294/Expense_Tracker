package com.nk.expensetracker.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.AppSearchField
import com.nk.expensetracker.core.designsystem.components.EmptyState
import com.nk.expensetracker.core.designsystem.components.QuickChoice
import com.nk.expensetracker.core.designsystem.components.TransactionRow
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    if (uiState.isFiltered) {
                        TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
            ) {
                AppSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    placeholder = "Search payee, note, or amount"
                )

                TypeFilterRow(
                    selected = uiState.typeFilter,
                    onSelect = viewModel::onTypeFilterChanged
                )

                if (!uiState.isLoading && uiState.matchedCount > 0) {
                    ResultSummary(
                        count = uiState.matchedCount,
                        netOutflowPaise = uiState.netOutflowPaise
                    )
                }
            }

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.isEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isFiltered) {
                        EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = "No matches",
                            message = "Nothing matches your search and filters.",
                            action = {
                                TextButton(onClick = viewModel::clearFilters) {
                                    Text("Clear filters")
                                }
                            }
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Rounded.ReceiptLong,
                            title = "No transactions yet",
                            message = "Tap Add to record your first expense, or import from SMS."
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.gutter,
                        end = AppTheme.spacing.gutter,
                        top = AppTheme.spacing.md,
                        bottom = AppTheme.spacing.xxxl * 2
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
                ) {
                    uiState.groups.forEach { group ->
                        item(key = "header-${group.dateMillis}") {
                            DayHeader(
                                label = group.label,
                                spentPaise = group.spentPaise,
                                count = group.transactions.size
                            )
                        }

                        items(
                            items = group.transactions,
                            key = { "tx-${it.id}" },
                            contentType = { "transaction" }
                        ) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                categoryName = uiState.categoryNames[transaction.categoryId],
                                showCategory = true,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(
    selected: TransactionType?,
    onSelect: (TransactionType?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        contentPadding = PaddingValues(vertical = AppTheme.spacing.xxs)
    ) {
        item {
            QuickChoice(
                label = "All",
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(TransactionType.entries.toList()) { type ->
            QuickChoice(
                label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = selected == type,
                onClick = { onSelect(type) }
            )
        }
    }
}

@Composable
private fun ResultSummary(count: Int, netOutflowPaise: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$count ${if (count == 1) "result" else "results"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (netOutflowPaise > 0) {
            Text(
                text = "${Money.formatPaiseTrimmed(netOutflowPaise)} out",
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.money.expense
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, spentPaise: Long, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spacing.md, bottom = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (count == 1) "1 transaction" else "$count transactions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (spentPaise > 0) {
            Text(
                text = Money.formatPaiseTrimmed(spentPaise),
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.money.expense
            )
        }
    }
}