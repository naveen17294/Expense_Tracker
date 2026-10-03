package com.nk.expensetracker.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand palette: a deep teal seed that reads as "money / calm / trustworthy".
 * Tonal roles are filled in explicitly rather than relying on defaults so the
 * light and dark schemes stay recognisably the same brand.
 */

internal val TealPrimaryLight = Color(0xFF00696D)
internal val TealOnPrimaryLight = Color(0xFFFFFFFF)
internal val TealPrimaryContainerLight = Color(0xFF9CF1F3)
internal val TealOnPrimaryContainerLight = Color(0xFF002021)

internal val BlueSecondaryLight = Color(0xFF4A6365)
internal val BlueOnSecondaryLight = Color(0xFFFFFFFF)
internal val BlueSecondaryContainerLight = Color(0xFFCCE8EA)
internal val BlueOnSecondaryContainerLight = Color(0xFF051F21)

internal val IndigoTertiaryLight = Color(0xFF4B607C)
internal val IndigoOnTertiaryLight = Color(0xFFFFFFFF)
internal val IndigoTertiaryContainerLight = Color(0xFFD3E4FF)
internal val IndigoOnTertiaryContainerLight = Color(0xFF041C35)

internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)

internal val BackgroundLight = Color(0xFFF5FAF9)
internal val OnBackgroundLight = Color(0xFF171D1D)
internal val SurfaceLight = Color(0xFFF5FAF9)
internal val OnSurfaceLight = Color(0xFF171D1D)
internal val SurfaceVariantLight = Color(0xFFDAE4E5)
internal val OnSurfaceVariantLight = Color(0xFF3F4849)
internal val OutlineLight = Color(0xFF6F797A)
internal val OutlineVariantLight = Color(0xFFBEC8C9)

internal val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLowLight = Color(0xFFEFF4F4)
internal val SurfaceContainerLight = Color(0xFFE9EFEF)
internal val SurfaceContainerHighLight = Color(0xFFE3E9E9)
internal val SurfaceContainerHighestLight = Color(0xFFDDE4E4)

internal val InverseSurfaceLight = Color(0xFF2B3232)
internal val InverseOnSurfaceLight = Color(0xFFECF2F1)
internal val InversePrimaryLight = Color(0xFF80D4D7)

internal val TealPrimaryDark = Color(0xFF80D4D7)
internal val TealOnPrimaryDark = Color(0xFF003738)
internal val TealPrimaryContainerDark = Color(0xFF004F51)
internal val TealOnPrimaryContainerDark = Color(0xFF9CF1F3)

internal val BlueSecondaryDark = Color(0xFFB0CCCE)
internal val BlueOnSecondaryDark = Color(0xFF1B3436)
internal val BlueSecondaryContainerDark = Color(0xFF324B4D)
internal val BlueOnSecondaryContainerDark = Color(0xFFCCE8EA)

internal val IndigoTertiaryDark = Color(0xFFB3C8E8)
internal val IndigoOnTertiaryDark = Color(0xFF1C314B)
internal val IndigoTertiaryContainerDark = Color(0xFF334863)
internal val IndigoOnTertiaryContainerDark = Color(0xFFD3E4FF)

internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

internal val BackgroundDark = Color(0xFF0E1414)
internal val OnBackgroundDark = Color(0xFFDDE4E4)
internal val SurfaceDark = Color(0xFF0E1414)
internal val OnSurfaceDark = Color(0xFFDDE4E4)
internal val SurfaceVariantDark = Color(0xFF3F4849)
internal val OnSurfaceVariantDark = Color(0xFFBEC8C9)
internal val OutlineDark = Color(0xFF899393)
internal val OutlineVariantDark = Color(0xFF3F4849)

internal val SurfaceContainerLowestDark = Color(0xFF090F0F)
internal val SurfaceContainerLowDark = Color(0xFF171D1D)
internal val SurfaceContainerDark = Color(0xFF1B2121)
internal val SurfaceContainerHighDark = Color(0xFF252B2B)
internal val SurfaceContainerHighestDark = Color(0xFF303636)

internal val InverseSurfaceDark = Color(0xFFDDE4E4)
internal val InverseOnSurfaceDark = Color(0xFF2B3232)
internal val InversePrimaryDark = Color(0xFF00696D)

val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    tertiary = IndigoTertiaryLight,
    onTertiary = IndigoOnTertiaryLight,
    tertiaryContainer = IndigoTertiaryContainerLight,
    onTertiaryContainer = IndigoOnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    scrim = Color(0xFF000000)
)

val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    tertiary = IndigoTertiaryDark,
    onTertiary = IndigoOnTertiaryDark,
    tertiaryContainer = IndigoTertiaryContainerDark,
    onTertiaryContainer = IndigoOnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    scrim = Color(0xFF000000)
)