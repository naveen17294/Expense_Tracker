package com.nk.expensetracker.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.nk.expensetracker.core.designsystem.theme.AppTheme

private const val PIN_LENGTH = 4

@Composable
fun PinScreen(
    viewModel: PinViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthenticated()
    }

    // Shakes the dots sideways when the PIN is rejected.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    0f at 0
                    -18f at 40
                    18f at 90
                    -12f at 140
                    12f at 190
                    -6f at 240
                    0f at 320
                }
            )
        }
    }

    val isSetup = uiState.mode == PinMode.SETUP

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Shake is applied as an offset so layout is never disturbed.
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .padding(horizontal = AppTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSetup) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(AppTheme.spacing.xl))

            Text(
                text = if (isSetup) "Create your PIN" else "Enter your PIN",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppTheme.spacing.sm))

            Text(
                text = if (isSetup) {
                    "Choose a 4-digit PIN to protect your financial data."
                } else {
                    "Your data stays on this device and is never uploaded."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppTheme.spacing.xxl))

            PinDots(
                filled = uiState.currentPin.length,
                isError = uiState.error != null
            )

            uiState.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = AppTheme.spacing.md)
                )
            }

            Spacer(Modifier.height(AppTheme.spacing.xl))

            PinKeypad(
                onDigit = { digit ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onPinChanged(uiState.currentPin + digit)
                },
                onDelete = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onPinChanged(uiState.currentPin.dropLast(1))
                }
            )

            if (isSetup) {
                Spacer(Modifier.height(AppTheme.spacing.lg))
                androidx.compose.material3.TextButton(onClick = { viewModel.skipSetup() }) {
                    Text("Skip for now")
                }
            }
        }
    }
}

@Composable
private fun PinDots(filled: Int, isError: Boolean, modifier: Modifier = Modifier) {
    val activeColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier.semantics {
            contentDescription = "$filled of $PIN_LENGTH digits entered"
        },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)
    ) {
        repeat(PIN_LENGTH) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(if (isFilled) 18.dp else 14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) activeColor else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))

                        "DEL" -> KeypadButton(
                            onClick = onDelete,
                            modifier = Modifier.clearAndSetSemantics { }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Delete last digit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> KeypadButton(onClick = { onDigit(key) }) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}