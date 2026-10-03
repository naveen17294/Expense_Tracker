package com.nk.expensetracker

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nk.expensetracker.feature.addedit.AddEditTransactionScreen
import com.nk.expensetracker.feature.dashboard.DashboardScreen
import com.nk.expensetracker.feature.review.ReviewScreen
import com.nk.expensetracker.feature.transactions.TransactionListScreen

object Destinations {
    const val PIN = "pin"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val REVIEW = "review"
    const val BACKUP = "backup"
    const val ADD_EDIT_TRANSACTION = "add_edit_transaction?transactionId={transactionId}"
    
    fun createAddEditRoute(transactionId: String? = null): String {
        return if (transactionId != null) "add_edit_transaction?transactionId=$transactionId" else "add_edit_transaction"
    }
}

@Composable
fun ExpenseTrackerNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            // Only show bottom bar on top-level destinations
            val isTopLevel = currentDestination?.route in listOf(Destinations.DASHBOARD, Destinations.TRANSACTIONS)
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == Destinations.DASHBOARD } == true,
                        onClick = {
                            navController.navigate(Destinations.DASHBOARD) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Transactions") },
                        label = { Text("Transactions") },
                        selected = currentDestination?.hierarchy?.any { it.route == Destinations.TRANSACTIONS } == true,
                        onClick = {
                            navController.navigate(Destinations.TRANSACTIONS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destinations.PIN,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destinations.PIN) {
                com.nk.expensetracker.feature.auth.PinScreen(
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
                    onReviewClick = {
                        navController.navigate(Destinations.REVIEW)
                    },
                    onBackupClick = {
                        navController.navigate(Destinations.BACKUP)
                    }
                )
            }

            composable(Destinations.BACKUP) {
                com.nk.expensetracker.feature.backuprestore.BackupScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
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
                AddEditTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
