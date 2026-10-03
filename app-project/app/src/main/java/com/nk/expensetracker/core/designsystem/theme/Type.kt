package com.nk.expensetracker.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * MD3 type scale tuned for a data-dense finance app:
 * headings are emphasised (heavier weight), body copy stays at regular weight,
 * and labels are tightened for compact metadata rows.
 *
 * Roboto is the correct MD3 typeface, so the platform default family is used.
 */
private val Alignment = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = Alignment
)

val AppTypography = Typography(
    displayLarge = style(57, 64, FontWeight.Normal, -0.25),
    displayMedium = style(45, 52, FontWeight.Normal),
    displaySmall = style(36, 44, FontWeight.Normal),

    headlineLarge = style(32, 40, FontWeight.Bold, -0.2),
    headlineMedium = style(28, 36, FontWeight.Bold),
    headlineSmall = style(24, 32, FontWeight.Bold),

    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(16, 24, FontWeight.SemiBold, 0.15),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),

    bodyLarge = style(16, 24, FontWeight.Normal, 0.5),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.25),
    bodySmall = style(12, 16, FontWeight.Normal, 0.4),

    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.5),
    labelSmall = style(11, 16, FontWeight.Medium, 0.5)
)

/**
 * Tabular figures stop columns of money from jittering as digits change.
 * Applied on top of an existing style rather than replacing it.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")