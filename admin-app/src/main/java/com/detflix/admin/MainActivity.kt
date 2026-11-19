package com.detflix.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.detflix.admin.ui.AdminTab
import com.detflix.admin.ui.AdminViewModel
import com.detflix.admin.ui.screens.CatalogAndSettingsScreen
import com.detflix.admin.ui.screens.ClientsScreen
import com.detflix.admin.ui.screens.CodesScreen
import com.detflix.admin.ui.screens.DashboardScreen
import com.detflix.admin.ui.screens.PaymentsScreen
import com.detflix.admin.ui.screens.PinLockScreen
import com.detflix.admin.ui.screens.StatusPill
import com.detflix.admin.ui.screens.copyTextToClipboard
import com.detflix.admin.ui.screens.sharePlainText
import com.detflix.admin.ui.theme.DetflixAdminTheme
import com.detflix.admin.ui.theme.DetflixBlack
import com.detflix.admin.ui.theme.DetflixGold
import com.detflix.admin.ui.theme.DetflixRed
import com.detflix.admin.ui.theme.DetflixSurface
import com.detflix.admin.ui.theme.DetflixTextMuted
import com.detflix.admin.ui.theme.DetflixWhite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DetflixAdminTheme {
                DetflixAdminRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetflixAdminRoot(vm: AdminViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }
    var triggerAddClient by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }

    if (!state.isUnlocked) {
        PinLockScreen(errorText = state.pinError, onUnlock = vm::verifyPin)
        return
    }

    val navItems = remember {
        listOf(
            AdminTab.DASHBOARD to Icons.Default.Dashboard,
            AdminTab.CODES to Icons.Default.Key,
            AdminTab.CLIENTS to Icons.Default.People,
            AdminTab.PAYMENTS to Icons.Default.AttachMoney,
            AdminTab.CATALOG to Icons.Default.Movie
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(DetflixBlack)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = DetflixBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DetflixSurface,
                        titleContentColor = DetflixWhite
                    ),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("DET")
                                    withStyle(SpanStyle(color = DetflixRed)) { append("FLIX") }
                                    append(" ")
                                    withStyle(SpanStyle(color = DetflixGold)) { append("ADMIN") }
                                },
                                style = MaterialTheme.typography.headlineLarge
                            )
                            StatusPill("APK LOCAL", DetflixGold)
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = DetflixSurface) {
                        navItems.forEach { (tab, icon) ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DetflixWhite,
                                    selectedTextColor = DetflixGold,
                                    indicatorColor = DetflixRed,
                                    unselectedIconColor = DetflixTextMuted,
                                    unselectedTextColor = DetflixTextMuted
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        vm.generateQuickCode()
                        currentTab = AdminTab.CODES
                    },
                    containerColor = DetflixRed,
                    contentColor = DetflixWhite,
                    modifier = Modifier.testTag("fab_generate_code")
                ) {
                    Icon(Icons.Default.Key, contentDescription = "Generar código")
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail(
                        containerColor = DetflixSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.width(4.dp))
                        navItems.forEach { (tab, icon) ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DetflixWhite,
                                    selectedTextColor = DetflixGold,
                                    indicatorColor = DetflixRed,
                                    unselectedIconColor = DetflixTextMuted,
                                    unselectedTextColor = DetflixTextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        AdminTab.DASHBOARD -> DashboardScreen(
                            state = state,
                            onGenerateQuickCode = { vm.generateQuickCode() },
                            onOpenNewClientDialog = {
                                triggerAddClient = true
                                currentTab = AdminTab.CLIENTS
                            },
                            onExportCatalog = {
                                vm.exportCatalogJson { json ->
                                    copyTextToClipboard(context, "catalog.json", json)
                                    sharePlainText(context, "Exportar catalog.json", json)
                                    vm.notifyMessage("catalog.json copiado y listo para compartir")
                                }
                            },
                            onCopyCode = { code ->
                                copyTextToClipboard(context, "Código DETFLIX", code)
                                vm.notifyMessage("Código copiado: $code")
                            }
                        )

                        AdminTab.CODES -> CodesScreen(
                            state = state,
                            onGenerateCode = { name, recordPay ->
                                vm.generateQuickCode(clientName = name, recordPayment = recordPay)
                            },
                            onAssignCode = vm::assignCodeToClient,
                            onRevokeCode = vm::revokeCode,
                            onCopyCode = { code ->
                                copyTextToClipboard(context, "Código DETFLIX", code)
                                vm.notifyMessage("Código copiado: $code")
                            }
                        )

                        AdminTab.CLIENTS -> ClientsScreen(
                            state = state,
                            showAddDialogInitial = triggerAddClient,
                            onDismissInitialAdd = { triggerAddClient = false },
                            onRegisterOrRenew = vm::registerOrRenewClient,
                            onToggleStatus = vm::toggleClientStatus,
                            onDeleteClient = vm::deleteClient
                        )

                        AdminTab.PAYMENTS -> PaymentsScreen(
                            state = state,
                            onRecordPayment = vm::recordPayment
                        )

                        AdminTab.CATALOG -> CatalogAndSettingsScreen(
                            state = state,
                            onSaveContent = vm::saveContent,
                            onToggleContentAvailable = vm::toggleContentAvailable,
                            onDeleteContent = vm::deleteContent,
                            onExportCatalog = {
                                vm.exportCatalogJson { json ->
                                    copyTextToClipboard(context, "catalog.json", json)
                                    sharePlainText(context, "Exportar catalog.json", json)
                                    vm.notifyMessage("catalog.json copiado y listo para compartir")
                                }
                            },
                            onSaveSettings = vm::saveSettings
                        )
                    }
                }
            }
        }
    }
}
