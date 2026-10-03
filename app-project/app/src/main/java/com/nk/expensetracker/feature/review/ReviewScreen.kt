package com.nk.expensetracker.feature.review

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.AmountText
import com.nk.expensetracker.core.designsystem.components.CategoryVisuals
import com.nk.expensetracker.core.designsystem.components.EmptyState
import com.nk.expensetracker.core.designsystem.components.IconBadge
import com.nk.expensetracker.core.designsystem.components.StatusPill
import com.nk.expensetracker.core.designsystem.components.direction
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review queue")
                        if (uiState.pendingTransactions.isNotEmpty()) {
                            Text(
                                text = "${uiState.pendingTransactions.size} waiting",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.pendingTransactions.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.CheckCircle,
                    title = "You're all caught up",
                    message = "Every imported transaction has been confirmed. New ones will appear here."
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = AppTheme.spacing.gutter,
                    end = AppTheme.spacing.gutter,
                    top = AppTheme.spacing.sm,
                    bottom = AppTheme.spacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
            ) {
                item(key = "intro") { QueueIntro(count = uiState.pendingTransactions.size) }

                items(uiState.pendingTransactions, key = { it.id }) { transaction ->
                    ReviewCard(
                        transaction = transaction,
                        onApprove = { viewModel.approveTransaction(transaction) },
                        onReject = { viewModel.rejectTransaction(transaction) },
                        onEdit = { onEditTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueIntro(count: Int) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.RateReview,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Text(
                text = "Check the amount, payee, and category before confirming. " +
                    "Anything you confirm teaches the rules engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewCard(
    transaction: TransactionEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit
) {
    val confidence = transaction.confidenceScore
    val confidencePercent = ((confidence ?: 0.0) * 100).roundToInt().coerceIn(0, 100)
    val barColor by animateColorAsState(
        targetValue = when {
            confidence == null -> MaterialTheme.colorScheme.outline
            confidence >= 0.9 -> AppTheme.money.income
            confidence >= 0.65 -> MaterialTheme.colorScheme.tertiary
            else -> AppTheme.money.expense
        },
        label = "confidenceColor"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = CategoryVisuals.iconFor(transaction.categoryId, null),
                    tint = when (transaction.type.direction()) {
                        com.nk.expensetracker.core.designsystem.components.MoneyDirection.OUTFLOW ->
                            AppTheme.money.expense

                        com.nk.expensetracker.core.designsystem.components.MoneyDirection.INFLOW ->
                            AppTheme.money.income

                        com.nk.expensetracker.core.designsystem.components.MoneyDirection.NEUTRAL ->
                            AppTheme.money.transfer
                    }
                )

                Spacer(Modifier.width(AppTheme.spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.counterpartyName?.takeIf { it.isNotBlank() }
                            ?: "Unknown payee",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = TimeUtils.formatDateTime(transaction.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AmountText(
                    amountPaise = transaction.amountPaise,
                    direction = transaction.type.direction(),
                    showSign = true,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    StatusPill(text = transaction.type.name.lowercase().replaceFirstChar { it.uppercase() })
                    if (transaction.upiReference != null) {
                        StatusPill(
                            text = "UPI ${transaction.upiReference.takeLast(6)}",
                            container = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                    if (transaction.source.name == "SMS") {
                        StatusPill(
                            text = "from SMS",
                            container = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                if (transaction.description?.isNotBlank() == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .padding(AppTheme.spacing.xs),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(AppTheme.spacing.xs))
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (confidence != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { (confidencePercent / 100f) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        Spacer(Modifier.width(AppTheme.spacing.sm))
                        Text(
                            text = "$confidencePercent% confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = AppTheme.spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReject) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppTheme.spacing.xs))
                    Text("Exclude")
                }

                Box(modifier = Modifier.weight(1f))

                FilledTonalButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppTheme.spacing.xs))
                    Text("Edit")
                }

                Button(onClick = onApprove) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppTheme.spacing.xs))
                    Text("Confirm")
                }
            }
        }
    }
}