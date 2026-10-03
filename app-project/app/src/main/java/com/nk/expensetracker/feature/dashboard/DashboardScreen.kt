package com.nk.expensetracker.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.AmountText
import com.nk.expensetracker.core.designsystem.components.CategoryVisuals
import com.nk.expensetracker.core.designsystem.components.EmptyState
import com.nk.expensetracker.core.designsystem.components.IconBadge
import com.nk.expensetracker.core.designsystem.components.MoneyDirection
import com.nk.expensetracker.core.designsystem.components.SectionHeader
import com.nk.expensetracker.core.designsystem.components.TransactionRow
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.money.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddTransactionClick: () -> Unit,
    onReviewClick: () -> Unit,
    onSeeAllClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.monthLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.IconButton(onClick = onBackupClick) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = "Backup and restore")
                    }
                },
                scrollBehavior = null
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = AppTheme.spacing.gutter,
                end = AppTheme.spacing.gutter,
                top = AppTheme.spacing.sm,
                bottom = AppTheme.spacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)
        ) {
            item(key = "summary") {
                MonthSummaryCard(
                    netCashFlow = uiState.netCashFlow,
                    expenses = uiState.currentMonthExpenses,
                    income = uiState.currentMonthIncome,
                    transactionCount = uiState.transactionCount
                )
            }

            item(key = "review") {
                ReviewCallout(count = uiState.reviewCount, onClick = onReviewClick)
            }

            if (uiState.topCategories.isNotEmpty()) {
                item(key = "breakdown") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        SectionHeader(
                            title = "Spending breakdown",
                            subtitle = "Top categories this month"
                        )
                        uiState.topCategories.forEach { spend ->
                            CategorySpendBar(spend)
                        }
                    }
                }
            }

            item(key = "recent_header") {
                SectionHeader(
                    title = "Recent activity",
                    actionLabel = if (uiState.recentTransactions.isNotEmpty()) "See all" else null,
                    onActionClick = onSeeAllClick
                )
            }

            if (uiState.recentTransactions.isEmpty()) {
                item(key = "recent_empty") {
                    EmptyState(
                        icon = Icons.Rounded.Inbox,
                        title = "Nothing recorded yet",
                        message = "Add a transaction manually or import one from an SMS to get started."
                    )
                }
            } else {
                items(uiState.recentTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        categoryName = null,
                        showCategory = false,
                        onClick = onSeeAllClick
                    )
                }
            }
        }
    }
}

/**
 * Hero card for the month. Net flow is the headline because that is the number
 * people actually check; income and spend sit underneath as supporting detail.
 */
@Composable
private fun MonthSummaryCard(
    netCashFlow: Long,
    expenses: Long,
    income: Long,
    transactionCount: Int
) {
    val palette = AppTheme.money
    val isPositive = netCashFlow >= 0
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(container, MaterialTheme.colorScheme.tertiaryContainer)
                    )
                )
                .padding(AppTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Net cash flow",
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                StatusTrendPill(isPositive = isPositive)
            }

            Text(
                text = Money.formatPaiseTrimmed(netCashFlow),
                style = MaterialTheme.typography.displaySmall,
                color = onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$transactionCount confirmed ${if (transactionCount == 1) "transaction" else "transactions"}",
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.75f)
            )

            HorizontalDivider(color = onContainer.copy(alpha = 0.16f))

            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                FlowStat(
                    label = "Spent",
                    amountPaise = expenses,
                    direction = MoneyDirection.OUTFLOW,
                    onContainer = onContainer,
                    modifier = Modifier.weight(1f)
                )
                FlowStat(
                    label = "Received",
                    amountPaise = income,
                    direction = MoneyDirection.INFLOW,
                    onContainer = onContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusTrendPill(isPositive: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isPositive) {
            AppTheme.money.income.copy(alpha = 0.18f)
        } else {
            AppTheme.money.expense.copy(alpha = 0.18f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                contentDescription = null,
                tint = if (isPositive) AppTheme.money.income else AppTheme.money.expense,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(AppTheme.spacing.xxs))
            Text(
                text = if (isPositive) "Positive" else "Negative",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPositive) AppTheme.money.income else AppTheme.money.expense
            )
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    amountPaise: Long,
    direction: MoneyDirection,
    onContainer: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val accent = if (direction == MoneyDirection.OUTFLOW) {
        AppTheme.money.expense
    } else {
        AppTheme.money.income
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(AppTheme.spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = onContainer.copy(alpha = 0.8f)
            )
        }
        Text(
            text = Money.formatPaiseTrimmed(amountPaise),
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Only shown when there is something to review; otherwise it collapses away. */
@Composable
private fun ReviewCallout(count: Int, onClick: () -> Unit) {
    if (count == 0) return

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Rounded.RateReview,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                background = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
            )
            Spacer(Modifier.width(AppTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$count ${if (count == 1) "transaction" else "transactions"} to review",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Confirm or edit what the parser detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(AppTheme.spacing.sm))
            FilledTonalButton(onClick = onClick) { Text("Review") }
        }
    }
}

@Composable
private fun CategorySpendBar(spend: CategorySpend) {
    val tint = CategoryVisuals.colorFor(spend.categoryId, spend.categoryName, null)
    val progress by animateFloatAsState(
        targetValue = spend.fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "categoryProgress"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(tint)
                )
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(
                    text = spend.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                AmountText(
                    amountPaise = spend.amountPaise,
                    direction = MoneyDirection.OUTFLOW,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = tint,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}