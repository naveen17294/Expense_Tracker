package com.nk.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nk.expensetracker.BuildConfig
import com.nk.expensetracker.core.designsystem.theme.ExpenseTrackerTheme
import com.nk.expensetracker.data.db.DatabaseSeeder
import com.nk.expensetracker.domain.model.TransactionStatus
import com.nk.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Seed database on launch
        lifecycleScope.launch {
            databaseSeeder.seedDatabaseIfNeeded()
        }

        // Request SMS permissions if we are in the personalSms flavor
        if (BuildConfig.FLAVOR == "personalSms") {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_SMS)
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }

        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val reviewCountFlow = remember(transactionRepository) {
                        transactionRepository
                            .getTransactionsByStatus(TransactionStatus.NEEDS_REVIEW)
                            .map { it.size }
                            .distinctUntilChanged()
                    }
                    val reviewCount by reviewCountFlow
                        .collectAsStateWithLifecycle(initialValue = 0)

                    ExpenseTrackerNavGraph(reviewCount = reviewCount)
                }
            }
        }
    }
}
