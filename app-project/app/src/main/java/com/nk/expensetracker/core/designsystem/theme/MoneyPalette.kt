package com.nk.expensetracker.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours for money direction.
 *
 * These are derived from the active [ColorScheme] rather than hard-coded so the
 * palette stays correctly paired (`onX` always sits on `X`) under dynamic
 * colour, light, and dark. If a scheme is swapped the mapping follows.
 */
@Immutable
data class MoneyPalette(
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val transfer: Color,
    val refund: Color,
    val neutral: Color
) {
    companion object {
        fun from(scheme: ColorScheme) = MoneyPalette(
            expense = scheme.error,
            onExpense = scheme.onError,
            expenseContainer = scheme.errorContainer,
            onExpenseContainer = scheme.onErrorContainer,
            income = scheme.primary,
            onIncome = scheme.onPrimary,
            incomeContainer = scheme.primaryContainer,
            onIncomeContainer = scheme.onPrimaryContainer,
            transfer = scheme.tertiary,
            refund = scheme.secondary,
            neutral = scheme.onSurfaceVariant
        )
    }
}

val LocalMoneyPalette = staticCompositionLocalOf { MoneyPalette.from(LightColorScheme) }

val LocalSpacing = staticCompositionLocalOf { Spacing() }

object AppTheme {
    val money: MoneyPalette
        @Composable @ReadOnlyComposable get() = LocalMoneyPalette.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}