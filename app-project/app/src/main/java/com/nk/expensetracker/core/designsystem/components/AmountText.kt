package com.nk.expensetracker.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import com.nk.expensetracker.core.designsystem.theme.tabular
import com.nk.expensetracker.core.money.Money
import com.nk.expensetracker.domain.model.TransactionType

/** Direction of money flow, used to pick a colour and a sign. */
enum class MoneyDirection { OUTFLOW, INFLOW, NEUTRAL }

/** Derives the visual direction from a transaction type. */
fun TransactionType.direction(): MoneyDirection = when (this) {
    TransactionType.EXPENSE -> MoneyDirection.OUTFLOW
    TransactionType.INCOME -> MoneyDirection.INFLOW
    TransactionType.REFUND -> MoneyDirection.INFLOW
    TransactionType.TRANSFER -> MoneyDirection.NEUTRAL
}

@Composable
fun directionColor(direction: MoneyDirection): Color = when (direction) {
    MoneyDirection.OUTFLOW -> AppTheme.money.expense
    MoneyDirection.INFLOW -> AppTheme.money.income
    MoneyDirection.NEUTRAL -> AppTheme.money.transfer
}

/**
 * Money in a tabular-figure style so columns of amounts stay aligned, coloured by
 * flow direction. The value is exposed as a single semantics node so TalkBack
 * reads "₹500" rather than "rupee, 5, 0, 0".
 */
@Composable
fun AmountText(
    amountPaise: Long,
    direction: MoneyDirection,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    showSign: Boolean = false,
    compact: Boolean = false,
    maxLines: Int = 1
) {
    val palette = AppTheme.money
    val color = when (direction) {
        MoneyDirection.OUTFLOW -> palette.expense
        MoneyDirection.INFLOW -> palette.income
        MoneyDirection.NEUTRAL -> palette.neutral
    }
    val text = when {
        compact -> Money.formatCompact(amountPaise)
        showSign && direction != MoneyDirection.NEUTRAL ->
            Money.formatSigned(amountPaise, isOutflow = direction == MoneyDirection.OUTFLOW)

        else -> Money.formatPaiseTrimmed(amountPaise)
    }

    Text(
        text = text,
        style = style.tabular(),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.clearAndSetSemantics { }
    )
}