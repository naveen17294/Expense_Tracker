package com.nk.expensetracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.time.TimeUtils
import com.nk.expensetracker.data.db.entity.TransactionEntity
import com.nk.expensetracker.domain.model.TransactionSource
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.model.TransactionType

/**
 * The row used everywhere a transaction is listed.
 *
 * Reads as one sentence for accessibility: "Swiggy, Food & Dining, expense,
 * minus ₹250.00, Today". Individual pieces are merged so a screen reader does
 * not walk six separate nodes.
 */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    categoryName: String?,
    modifier: Modifier = Modifier,
    showCategory: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val direction = transaction.type.direction()
    val accent = when (transaction.type) {
        TransactionType.TRANSFER -> AppTheme.money.transfer
        else -> when (direction) {
            MoneyDirection.OUTFLOW -> AppTheme.money.expense
            MoneyDirection.INFLOW -> AppTheme.money.income
            MoneyDirection.NEUTRAL -> AppTheme.money.transfer
        }
    }

    val title = transaction.counterpartyName?.takeIf { it.isNotBlank() }
        ?: transaction.description?.takeIf { it.isNotBlank() }
        ?: "Unknown payee"

    val subtitle = buildList {
        if (showCategory && categoryName != null) add(categoryName)
        add(transaction.type.displayLabel())
    }.joinToString(" · ")

    val statusSuffix = when (transaction.status) {
        TransactionStatus.NEEDS_REVIEW -> " · needs review"
        TransactionStatus.DRAFT -> " · draft"
        TransactionStatus.EXCLUDED -> " · excluded"
        TransactionStatus.DELETED -> " · deleted"
        TransactionStatus.CONFIRMED -> ""
    }

    val amountLabel = when (direction) {
        MoneyDirection.OUTFLOW -> "minus ${com.nk.expensetracker.core.money.Money.formatPaise(transaction.amountPaise)}"
        MoneyDirection.INFLOW -> "plus ${com.nk.expensetracker.core.money.Money.formatPaise(transaction.amountPaise)}"
        MoneyDirection.NEUTRAL -> com.nk.expensetracker.core.money.Money.formatPaise(transaction.amountPaise)
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $subtitle, $amountLabel,$statusSuffix"
            }
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = CategoryVisuals.iconFor(transaction.categoryId, categoryName),
                tint = accent
            )

            Spacer(Modifier.width(AppTheme.spacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (transaction.status == TransactionStatus.NEEDS_REVIEW) {
                    StatusPill(
                        text = "Needs review",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.width(AppTheme.spacing.sm))

            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountPaise = transaction.amountPaise,
                    direction = direction,
                    showSign = transaction.type != TransactionType.TRANSFER,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.padding(top = AppTheme.spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = transaction.sourceIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(12.dp).padding(end = 2.dp)
                    )
                    Text(
                        text = TimeUtils.formatShortDate(transaction.occurredAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.REFUND -> "Refund"
}

private fun TransactionEntity.sourceIcon() = when (source) {
    TransactionSource.SMS -> Icons.Rounded.Sms
    TransactionSource.CSV -> Icons.Rounded.ArrowForward
    TransactionSource.MANUAL -> Icons.Rounded.Edit
    TransactionSource.NOTIFICATION -> Icons.Rounded.WarningAmber
    TransactionSource.RESTORED_BACKUP -> Icons.Rounded.CloudOff
}

/**
 * Backwards-compatible entry point. Existing screens keep compiling while they
 * are migrated to [TransactionRow] with a resolved category name.
 */
@Deprecated(
    message = "Use TransactionRow with a resolved category name",
    replaceWith = ReplaceWith("TransactionRow(transaction, categoryName = null)")
)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TransactionRow(
        transaction = transaction,
        categoryName = null,
        onClick = onClick,
        modifier = modifier
    )
}