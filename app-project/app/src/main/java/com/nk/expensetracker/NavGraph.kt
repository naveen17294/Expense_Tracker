package com.nk.expensetracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nk.expensetracker.feature.addedit.AddEditTransactionScreen
import com.nk.expensetracker.feature.auth.PinScreen
import com.nk.expensetracker.feature.backuprestore.BackupScreen
import com.nk.expensetracker.feature.csvimport.CsvImportScreen
import com.nk.expensetracker.feature.dashboard.DashboardScreen
import com.nk.expensetracker.feature.review.ReviewScreen
import com.nk.expensetracker.feature.transactions.TransactionListScreen

object Destinations {
    const val PIN = "pin"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val REVIEW = "review"
    const val BACKUP = "backup"
    const val IMPORT = "import"
    const val NOTEPAD_IMPORT = "notepad_import"
    const val ADD_EDIT_TRANSACTION = "add_edit_transaction?transactionId={transactionId}"

    fun createAddEditRoute(transactionId: String? = null): String =
        if (transactionId != null) "add_edit_transaction?transactionId=$transactionId" else "add_edit_transaction"
}

private data class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabDestination(Destinations.DASHBOARD, "Home", Icons.Rounded.Home),
    TabDestination(Destinations.TRANSACTIONS, "Transactions", Icons.Rounded.ReceiptLong),
    TabDestination(Destinations.REVIEW, "Review", Icons.Rounded.RateReview),
    TabDestination(Destinations.IMPORT, "Import", Icons.Rounded.UploadFile)
)

@Composable
fun ExpenseTrackerNavGraph(
    navController: NavHostController = rememberNavController(),
    reviewCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute in tabs.map { it.route }
    val showFab = currentRoute == Destinations.DASHBOARD || currentRoute == Destinations.TRANSACTIONS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab.route == Destinations.REVIEW && reviewCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) { Text(reviewCount.coerceAtMost(99).toString()) }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Destinations.createAddEditRoute()) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.PIN,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Destinations.PIN) {
                PinScreen(
                    onAuthenticated = {
                        navController.navigate(Destinations.DASHBOARD) {
                            popUpTo(Destinations.PIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.DASHBOARD) {
                DashboardScreen(
                    onAddTransactionClick = {
                        navController.navigate(Destinations.createAddEditRoute())
                    },
                    onReviewClick = { navController.navigate(Destinations.REVIEW) },
                    onSeeAllClick = { navController.navigate(Destinations.TRANSACTIONS) },
                    onBackupClick = { navController.navigate(Destinations.BACKUP) }
                )
            }

            composable(Destinations.BACKUP) {
                BackupScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Destinations.REVIEW) {
                ReviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditTransaction = { txId ->
                        navController.navigate(Destinations.createAddEditRoute(txId))
                    }
                )
            }

            composable(Destinations.TRANSACTIONS) {
                TransactionListScreen(
                    onAddTransactionClick = {
                        navController.navigate(Destinations.createAddEditRoute())
                    },
                    onTransactionClick = { txId ->
                        navController.navigate(Destinations.createAddEditRoute(txId))
                    }
                )
            }

            composable(Destinations.ADD_EDIT_TRANSACTION) {
                AddEditTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Destinations.NOTEPAD_IMPORT) {
                com.nk.expensetracker.feature.notepadimport.NotepadImportScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Destinations.IMPORT) {
                CsvImportScreen(onNavigateToNotepad = { navController.navigate(Destinations.NOTEPAD_IMPORT) })
            }
        }
    }
}