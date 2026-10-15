package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.CrmRepository
import com.example.security.BiometricSecurityManager
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.BottomCrmNav
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.SyncBanner
import com.example.ui.components.TopCrmBar
import com.example.ui.screens.AdminTeamScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FollowUpsScreen
import com.example.ui.screens.LeadsScreen
import com.example.ui.screens.ProjectsTasksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CrmViewModel

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainCrmApp()
            }
        }
    }
}

class CrmViewModelFactory(
    private val repository: CrmRepository,
    private val securityManager: BiometricSecurityManager,
    private val permissionManager: com.example.security.PermissionManager = com.example.security.PermissionManager()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrmViewModel::class.java)) {
            return CrmViewModel(repository, securityManager, permissionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainCrmApp() {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Initialize Database, Security Manager, and Repository
    val database = remember { AppDatabase.getDatabase(context, coroutineScope) }
    val securityManager = remember { BiometricSecurityManager(context) }
    val repository = remember { CrmRepository(database.crmDao()) }

    val viewModel: CrmViewModel = viewModel(
        factory = remember { CrmViewModelFactory(repository, securityManager) }
    )

    val strings by viewModel.strings.collectAsState()
    val isAppLocked by viewModel.securityManager.isAppLocked.collectAsState()
    val unreadNotifsCount by viewModel.unreadNotificationsCount.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var openAddLeadDialogDirectly by remember { mutableStateOf(false) }

    // Android BackHandler: if on any sub-tab, pressing back navigates to Dashboard tab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopCrmBar(
                    viewModel = viewModel,
                    unreadNotificationsCount = unreadNotifsCount,
                    isOffline = isOffline,
                    isSyncing = isSyncing,
                    pendingSyncCount = pendingSyncCount,
                    onNotificationsClick = { showNotificationsDialog = true },
                    onRoleClick = { selectedTab = 4 } // Jump to Team & Admin
                )
            },
            bottomBar = {
                BottomCrmNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    strings = strings
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Global Sync Status Banner
                SyncBanner(
                    isOffline = isOffline,
                    isSyncing = isSyncing,
                    pendingCount = pendingSyncCount,
                    strings = strings,
                    onSyncClick = { viewModel.triggerCloudSync() }
                )

                // Current Active Tab
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onNavigateToLeads = { selectedTab = 1 },
                        onNavigateToFollowUps = { selectedTab = 2 },
                        onNavigateToAdmin = { selectedTab = 4 },
                        onAddNewLeadClick = {
                            openAddLeadDialogDirectly = true
                            selectedTab = 1
                        }
                    )
                    1 -> {
                        LeadsScreen(
                            viewModel = viewModel,
                            strings = strings,
                            showAddDialogInitially = openAddLeadDialogDirectly
                        )
                        openAddLeadDialogDirectly = false
                    }
                    2 -> FollowUpsScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                    3 -> ProjectsTasksScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                    4 -> AdminTeamScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                    5 -> SettingsScreen(
                        viewModel = viewModel,
                        strings = strings
                    )
                }
            }
        }

        // Fullscreen Biometric Lock Screen Overlay
        AnimatedVisibility(
            visible = isAppLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BiometricLockScreen(
                securityManager = viewModel.securityManager,
                strings = strings
            )
        }

        // Real-time Notifications Dialog
        if (showNotificationsDialog) {
            NotificationsDialog(
                notifications = notifications,
                strings = strings,
                onDismiss = { showNotificationsDialog = false },
                onNotificationClick = { notifId ->
                    viewModel.markNotificationRead(notifId)
                },
                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                onClearAll = { viewModel.clearNotifications() }
            )
        }
    }
}
