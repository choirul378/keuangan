package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.IconButton
import com.example.ui.FinanceUiState
import com.example.ui.MainViewModel
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.BackupRestoreDialog
import com.example.ui.screens.BudgetDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme

import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.example.security.SecurityManager
import com.example.ui.screens.LockScreen
import com.example.ui.screens.SecuritySettingsDialog

enum class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Beranda", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    REPORT("Laporan Grafik", Icons.Default.BarChart, Icons.Outlined.BarChart),
    HISTORY("Riwayat", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong)
}

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val securityManager = remember { SecurityManager(this@MainActivity) }
            var isAppUnlocked by rememberSaveable { mutableStateOf(!securityManager.isPinEnabled) }
            var showSecurityDialog by remember { mutableStateOf(false) }
            val securitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            MyApplicationTheme {
                if (securityManager.isPinEnabled && !isAppUnlocked) {
                    LockScreen(
                        isBiometricAvailable = securityManager.isBiometricEnabled && securityManager.canUseBiometric(this@MainActivity),
                        onTriggerBiometric = {
                            securityManager.authenticateWithBiometric(
                                activity = this@MainActivity,
                                onSuccess = { isAppUnlocked = true },
                                onError = { /* fallback to pin */ }
                            )
                        },
                        onVerifyPin = { securityManager.verifyPin(it) },
                        onUnlockSuccess = { isAppUnlocked = true },
                        onResetPinConfirmed = {
                            securityManager.disableSecurity()
                            isAppUnlocked = true
                        }
                    )
                } else {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    var currentNavIndex by rememberSaveable { mutableIntStateOf(0) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentNavIndex) {
                                        0 -> "Catatan Keuangan"
                                        1 -> "Laporan & Grafik Bulanan"
                                        2 -> "Riwayat Transaksi"
                                        else -> "Catatan Keuangan"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showSecurityDialog = true },
                                    modifier = Modifier.testTag("security_settings_top_action")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Pengaturan PIN & Sidik Jari",
                                        tint = if (securityManager.isPinEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.openBackupRestoreDialog() },
                                    modifier = Modifier.testTag("backup_restore_top_action")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBackupRestore,
                                        contentDescription = "Cadangkan & Pulihkan Database",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavDestination.entries.forEachIndexed { index, destination ->
                                val selected = currentNavIndex == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentNavIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldPrimary,
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_item_$index")
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { viewModel.openAddTransaction() },
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.testTag("add_transaction_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Catatan Baru",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentNavIndex) {
                            0 -> HomeScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.selectPreviousMonth() },
                                onNextMonth = { viewModel.selectNextMonth() },
                                onCurrentMonth = { viewModel.selectCurrentMonth() },
                                onFilterTypeChange = { viewModel.setFilterType(it) },
                                onEditTransaction = { viewModel.openEditTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onEditBudgetClick = { viewModel.openBudgetDialog() }
                            )
                            1 -> ReportScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.selectPreviousMonth() },
                                onNextMonth = { viewModel.selectNextMonth() }
                            )
                            2 -> HistoryScreen(
                                uiState = uiState,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onFilterTypeChange = { viewModel.setFilterType(it) },
                                onEditTransaction = { viewModel.openEditTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onOpenBackupRestore = { viewModel.openBackupRestoreDialog() }
                            )
                        }
                    }

                    // Add/Edit Transaction Bottom Sheet
                    if (uiState.showAddEditSheet) {
                        AddEditTransactionSheet(
                            sheetState = sheetState,
                            initialTransaction = uiState.editingTransaction,
                            onDismiss = { viewModel.dismissAddEditSheet() },
                            onSave = { title, amount, type, categoryId, wallet, dateMillis, note ->
                                viewModel.saveTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    categoryId = categoryId,
                                    wallet = wallet,
                                    dateMillis = dateMillis,
                                    note = note
                                )
                            }
                        )
                    }

                    // Monthly Budget Dialog
                    if (uiState.showBudgetDialog) {
                        BudgetDialog(
                            currentBudget = uiState.monthlyBudget,
                            onDismiss = { viewModel.dismissBudgetDialog() },
                            onSaveBudget = { viewModel.setMonthlyBudget(it) }
                        )
                    }

                    // Backup & Restore Dialog
                    if (uiState.showBackupRestoreDialog) {
                        val backupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        BackupRestoreDialog(
                            sheetState = backupSheetState,
                            totalCurrentTransactions = uiState.allTransactions.size,
                            onDismiss = { viewModel.dismissBackupRestoreDialog() },
                            onGetBackupJson = { viewModel.getBackupJson() },
                            onRestoreBackup = { json, replace -> viewModel.restoreFromJson(json, replace) }
                        )
                    }

                    // Security Settings Dialog
                    if (showSecurityDialog) {
                        SecuritySettingsDialog(
                            sheetState = securitySheetState,
                            securityManager = securityManager,
                            onDismiss = { showSecurityDialog = false },
                            onSecurityChanged = {
                                isAppUnlocked = !securityManager.isPinEnabled
                            }
                        )
                    }
                }
            }
        }
    }
}
}
