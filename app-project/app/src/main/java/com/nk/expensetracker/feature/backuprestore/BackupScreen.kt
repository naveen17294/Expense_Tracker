package com.nk.expensetracker.feature.backuprestore

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nk.expensetracker.core.designsystem.components.IconBadge
import com.nk.expensetracker.core.designsystem.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.createBackup(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(AppTheme.spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            icon = Icons.Rounded.Lock,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            background = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            size = 48.dp
                        )
                        Spacer(Modifier.width(AppTheme.spacing.md))
                        Column {
                            Text(
                                text = "Your data stays yours",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Exports are plain text. Store them somewhere you trust.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spacing.xxl),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else {
                item {
                    ActionCard(
                        icon = Icons.Rounded.CloudUpload,
                        title = "Export a backup",
                        description = "Save transactions, accounts, and categories as a JSON file.",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        buttonLabel = "Export",
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            createBackupLauncher.launch("expense_tracker_backup_$stamp.json")
                        }
                    )
                }

                item {
                    ActionCard(
                        icon = Icons.Rounded.Restore,
                        title = "Restore from backup",
                        description = "Replace the data on this device with a previous backup file.",
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onContainer = MaterialTheme.colorScheme.onSurface,
                        buttonLabel = "Choose file",
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain")) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(AppTheme.spacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(AppTheme.spacing.sm))
                    Text(
                        text = "Restoring replaces everything currently in the app. " +
                            "Export a fresh backup first if you are unsure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = icon,
                    tint = onContainer,
                    background = onContainer.copy(alpha = 0.10f)
                )
                Spacer(Modifier.width(AppTheme.spacing.md))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer.copy(alpha = 0.85f)
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (icon == Icons.Rounded.Restore) {
                        Icons.Rounded.Download
                    } else {
                        Icons.Rounded.CloudUpload
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(buttonLabel)
            }
        }
    }
}