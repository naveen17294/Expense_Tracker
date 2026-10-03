package com.nk.expensetracker.feature.csvimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.AmountText
import com.nk.expensetracker.core.designsystem.components.DropdownSelector
import com.nk.expensetracker.core.designsystem.components.EmptyState
import com.nk.expensetracker.core.designsystem.components.IconBadge
import com.nk.expensetracker.core.designsystem.components.MoneyDirection
import com.nk.expensetracker.core.designsystem.components.SectionHeader
import com.nk.expensetracker.core.designsystem.components.StatusPill
import com.nk.expensetracker.core.designsystem.components.direction
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.domain.csv.SkipReason
import com.nk.expensetracker.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    viewModel: CsvImportViewModel = hiltViewModel(),
    onNavigateToNotepad: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
        viewModel.onFileSelected(uri, name ?: "Statement")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Import statement")
                        if (uiState.fileName != null) {
                            Text(
                                text = uiState.fileName!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.preview != null || uiState.outcome != null) {
                        TextButton(onClick = viewModel::reset) { Text("Start over") }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
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
            if (uiState.error != null) {
                item { ErrorCard(message = uiState.error!!, onDismiss = viewModel::dismissError) }
            }

            when {
                uiState.isBusy -> item { BusyBlock() }

                uiState.outcome != null -> item { OutcomeCard(uiState) }

                uiState.preview == null -> {
                    item { PickerBlock(onPick = { picker.launch(STATEMENT_MIME_TYPES) }) }
                    item {
                        OutlinedButton(
                            onClick = onNavigateToNotepad,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg)
                        ) {
                            Text("Or Paste Notepad Text")
                        }
                    }
                }

                else -> {
                    item { PreviewBlock(uiState) }
                    item { AccountBlock(uiState, viewModel::onAccountSelected) }
                    item { PreviewList(uiState) }
                    item {
                        CommitBlock(
                            enabled = uiState.canCommit,
                            newCount = uiState.preview!!.newCount,
                            onCommit = viewModel::commit
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(AppTheme.spacing.xl)) }
        }
    }
}

/** Statement exports arrive as CSV, TSV, or fixed-width text. */
private val STATEMENT_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/tab-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
    "application/octet-stream"
)

@Composable
private fun PickerBlock(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
    ) {
        EmptyState(
            icon = Icons.Rounded.UploadFile,
            title = "Choose a bank statement",
            message = "Pick the CSV or text export from your bank's app or email. " +
                "HDFC savings and credit card statements both work."
        )
        Button(onClick = onPick) {
            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppTheme.spacing.sm))
            Text("Select file")
        }
    }
}

@Composable
private fun BusyBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Reading statement…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(
                    text = "Could not import",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(AppTheme.spacing.sm))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(AppTheme.spacing.xs))
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun PreviewBlock(uiState: CsvImportUiState) {
    val preview = uiState.preview ?: return
    val parse = preview.parse

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
                text = "Ready to import",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            parse.statementTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                Metric(
                    value = preview.newCount.toString(),
                    label = "new",
                    modifier = Modifier.weight(1f)
                )
                Metric(
                    value = preview.duplicateCount.toString(),
                    label = "duplicates",
                    modifier = Modifier.weight(1f)
                )
                Metric(
                    value = parse.skippedCount.toString(),
                    label = "skipped",
                    modifier = Modifier.weight(1f)
                )
            }

            if (preview.newDebitPaise > 0 || preview.newCreditPaise > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                    AmountText(
                        amountPaise = preview.newDebitPaise,
                        direction = MoneyDirection.OUTFLOW,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    AmountText(
                        amountPaise = preview.newCreditPaise,
                        direction = MoneyDirection.INFLOW,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (parse.skipped.isNotEmpty()) {
                Text(
                    text = parse.skipped.entries.joinToString(" · ") { (reason, count) ->
                        "${count} ${reason.label()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            parse.issues.forEach { issue ->
                Text(
                    text = issue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun SkipReason.label(): String = when (this) {
    SkipReason.NO_DATE -> "unreadable dates"
    SkipReason.NO_AMOUNT -> "no amount"
    SkipReason.ZERO_AMOUNT -> "zero amount"
    SkipReason.SUMMARY_ROW -> "summary rows"
    SkipReason.MALFORMED -> "malformed"
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccountBlock(uiState: CsvImportUiState, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        SectionHeader(title = "Import into")
        DropdownSelector(
            label = "Account",
            items = uiState.accounts,
            selectedItem = uiState.accounts.find { it.id == uiState.selectedAccountId },
            onItemSelected = { onSelect(it.id) },
            itemLabel = { account ->
                account.maskedIdentifier?.let { "${account.name} •$it" } ?: account.name
            }
        )
    }
}

@Composable
private fun PreviewList(uiState: CsvImportUiState) {
    val preview = uiState.preview ?: return
    val candidates = preview.candidates.take(PREVIEW_ROWS)

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        SectionHeader(
            title = "Preview",
            subtitle = "Showing first $PREVIEW_ROWS of ${preview.candidates.size} rows"
        )

        candidates.forEach { candidate ->
            val draft = candidate.draft
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (candidate.isLikelyDuplicate) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.counterparty ?: "Unknown payee",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
                        ) {
                            Text(
                                text = TimeUtils.formatShortDate(draft.occurredAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusPill(text = draft.type.name.lowercase().replaceFirstChar { it.uppercase() })
                            if (candidate.isLikelyDuplicate) {
                                StatusPill(
                                    text = "already imported",
                                    container = MaterialTheme.colorScheme.tertiaryContainer,
                                    content = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(AppTheme.spacing.sm))
                    AmountText(
                        amountPaise = draft.amountPaise,
                        direction = draft.type.direction(),
                        showSign = draft.type != TransactionType.TRANSFER,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

private const val PREVIEW_ROWS = 25

@Composable
private fun CommitBlock(enabled: Boolean, newCount: Int, onCommit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Button(
            onClick = onCommit,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Import $newCount transactions")
        }
        Text(
            text = "Duplicates are skipped automatically. You can undo individual rows afterwards.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutcomeCard(uiState: CsvImportUiState) {
    val outcome = uiState.outcome ?: return

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Rounded.CheckCircle,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                )
                Spacer(Modifier.width(AppTheme.spacing.md))
                Text(
                    text = "Import complete",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "${outcome.inserted} added · ${outcome.duplicates} skipped as duplicates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            if (outcome.totalDebitPaise > 0 || outcome.totalCreditPaise > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                    Text(
                        text = "Out ${Money.formatPaiseTrimmed(outcome.totalDebitPaise)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "In ${Money.formatPaiseTrimmed(outcome.totalCreditPaise)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}