package com.example.logbook.ui.main

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.AppSettingsEntity
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.settings.FlightsSyncDialog
import com.example.logbook.ui.settings.SyncConflictDialog
import com.example.logbook.ui.settings.SyncStatus
import com.example.logbook.ui.settings.SyncViewModel
import kotlinx.coroutines.delay
import com.example.logbook.ui.tabs.chast1.Chast1Tab
import com.example.logbook.ui.tabs.flights.FlightsTabWithViewModel
import com.example.logbook.ui.tabs.flights.FlightsTopBarActions
import com.example.logbook.ui.tabs.flights.FlightsViewModel
import com.example.logbook.ui.tabs.maintenance.MaintenanceScreen
import com.example.logbook.ui.tabs.resources.ResourcesTab
import com.example.logbook.ui.tabs.resources.ResourcesViewModel
import com.example.logbook.ui.tabs.resources.SetupFlowDialog
import com.example.logbook.ui.tabs.ta14.Ta14Tab
import com.example.logbook.ui.tabs.vk250001.Vk250001Tab
import com.example.logbook.ui.tabs.vk250002.Vk250002Tab
import com.example.logbook.ui.tabs.vr80.Vr80Tab
import com.example.logbook.data.model.ResourceWarnings
import com.example.logbook.ui.tabs.resources.ResourceWarningsDialog
import com.example.logbook.ui.tabs.resources.rememberResourceToday
import com.example.logbook.ui.util.LocalInformationActions
import com.example.logbook.ui.util.CompactTopBar
import com.example.logbook.ui.util.LocalFormularZoomState
import com.example.logbook.ui.util.PullToSyncContainer
import com.example.logbook.ui.util.ZoomControls
import com.example.logbook.ui.util.rememberPersistentZoomState

private const val NO_TAB_SELECTED = -1

private data class MainTab(
    val title: String,
    val index: Int,
    val icon: ImageVector
)

private val MAIN_TABS get() = listOf(
    MainTab(tr(R.string.msg_flights_48b357c), 0, Icons.Default.Flight),
    MainTab(com.example.logbook.localization.I18n.display("Часть 1"), 1, Icons.AutoMirrored.Filled.MenuBook),
    MainTab(com.example.logbook.localization.I18n.display("ВР-80"), 2, Icons.Default.Build),
    MainTab(com.example.logbook.localization.I18n.display("Та-14"), 3, Icons.Default.Settings),
    MainTab(com.example.logbook.localization.I18n.display("ВК-2500-01"), 4, Icons.Default.Memory),
    MainTab(com.example.logbook.localization.I18n.display("ВК-2500-02"), 5, Icons.Default.Memory),
    MainTab(tr(R.string.msg_resources_3e559b8), 6, Icons.Default.BarChart),
    MainTab(tr(R.string.journal_title), 7, Icons.Default.Build)
)

// ============================================================
// MainScreen с ViewModel
// ============================================================

@Composable
fun MainScreen(
    profileId: Long,
    showSetupAggregatesOnStart: Boolean = false,
    onBack: () -> Unit,
    onNavigateToAggregates: (Long) -> Unit = {},
    onNavigateToOils: (Long) -> Unit = {},
    onNavigateToAggregatesOnBoard: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        key = "main_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val flightsViewModel: FlightsViewModel = viewModel(
        key = "flights_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (profile != null) {
            hasLoaded = true
        } else if (hasLoaded) {
            onBack()
        }
    }

    if (!hasLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(tr(R.string.msg_loading_b6819e9))
        }
        return
    }

    val currentProfile = profile ?: return
    com.example.logbook.ui.tabs.resources.ProfileEntryWarnings(currentProfile)

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    MainScreenContent(
        profile = currentProfile,
        profileId = profileId,
        flightsViewModel = flightsViewModel,
        appSettings = appSettings,
        showSetupAggregatesOnStart = showSetupAggregatesOnStart,
        onBack = onBack,
        onNavigateToAggregates = onNavigateToAggregates,
        onNavigateToOils = onNavigateToOils,
        onNavigateToAggregatesOnBoard = onNavigateToAggregatesOnBoard,
        onOpenSettings = onOpenSettings,
    )
}

// ============================================================
// MainScreenContent — без ViewModel (для Preview)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    profile: ProfileEntity,
    profileId: Long,
    flightsViewModel: FlightsViewModel?,
    appSettings: AppSettingsEntity? = null,
    showSetupAggregatesOnStart: Boolean = false,
    onBack: () -> Unit,
    onNavigateToAggregates: (Long) -> Unit = {},
    onNavigateToOils: (Long) -> Unit = {},
    onNavigateToAggregatesOnBoard: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    // Восстанавливаем вкладку из профиля.
    // -1 (NO_TAB_SELECTED) → сетка вкладок.
    var selectedTab by rememberSaveable {
        mutableIntStateOf(NO_TAB_SELECTED)
    }
    val tabStateHolder = rememberSaveableStateHolder()
    val formularZoomStates = listOf(
        rememberPersistentZoomState(profileId, "chast1"),
        rememberPersistentZoomState(profileId, "vr80"),
        rememberPersistentZoomState(profileId, "ta14"),
        rememberPersistentZoomState(profileId, "vk250001"),
        rememberPersistentZoomState(profileId, "vk250002")
    )
    val activeZoomState = formularZoomStates.getOrNull(selectedTab - 1)
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showPrintDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val syncViewModel: SyncViewModel = viewModel(
        key = "flights_sync",
        factory = SyncViewModel.Factory(context)
    )
    val syncState by syncViewModel.state.collectAsStateWithLifecycle()
    var showSyncWarningDialog by remember { mutableStateOf(false) }
    var showSyncErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncState.status, syncState.syncSeq) {
        if (syncState.status == SyncStatus.SUCCESS) {
            delay(500)
            syncViewModel.resetStatus()
        }
    }

    var hasAutoSyncedOnEntry by rememberSaveable(profileId) { mutableStateOf(false) }

    LaunchedEffect(appSettings?.syncOnFinishDay) {
        if (appSettings?.syncOnFinishDay == true && !hasAutoSyncedOnEntry) {
            hasAutoSyncedOnEntry = true
            syncViewModel.sync()
        }
    }

    val triggerAutoSync by flightsViewModel?.triggerAutoSync
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    LaunchedEffect(triggerAutoSync) {
        if (triggerAutoSync) {
            flightsViewModel?.consumeAutoSync()
            syncViewModel.sync()
        }
    }

    var showSetupAggregates by rememberSaveable {
        mutableStateOf(showSetupAggregatesOnStart)
    }

    // ============================================================
    // Системная кнопка «Назад»:
    //   - если открыта вкладка → вернуться к сетке вкладок
    //   - если открыта сетка → стандартный back (→ выход к профилям)
    // ============================================================
    BackHandler(enabled = selectedTab != NO_TAB_SELECTED) {
        selectedTab = NO_TAB_SELECTED
    }

    val resourcesViewModel: ResourcesViewModel? = if (showSetupAggregates) {
        viewModel(
            key = "resources_$profileId",
            factory = LogbookViewModelFactory(context, profileId)
        )
    } else null

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val profileName = profile.name.ifBlank { tr(R.string.msg_profile_1_s_b3ebad7, profileId) }

    val currentTabTitle = MAIN_TABS
        .firstOrNull { it.index == selectedTab }
        ?.title

    val title = if (selectedTab == NO_TAB_SELECTED || currentTabTitle == null) {
        profileName
    } else {
        "$profileName / $currentTabTitle"
    }

    val showFlightsToolbar = !(isLandscape && selectedTab == 0)

    val isFinishing = flightsViewModel?.isFinishing
        ?.collectAsStateWithLifecycle()?.value ?: false

    val information = LocalInformationActions.current
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            CompactTopBar(
                title = title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = {
                    if (selectedTab == NO_TAB_SELECTED) {
                        onBack()
                    } else {
                        selectedTab = NO_TAB_SELECTED
                    }
                },
                actions = {
                    SyncTopBarIndicator(
                        status = syncState.status,
                        onWarningClick = { showSyncWarningDialog = true },
                        onErrorClick = { showSyncErrorDialog = true }
                    )
                    if (selectedTab == 0 && isLandscape && flightsViewModel != null) {
                        FlightsTopBarActions(
                            onFinishDay = { flightsViewModel.finishDay() },
                            onHistory = { showHistoryDialog = true },
                            onSync = { syncViewModel.sync() },
                            finishDayEnabled = !isFinishing
                        )
                    }
                    if (activeZoomState != null) {
                        ZoomControls(activeZoomState)
                    }
                    if (selectedTab == NO_TAB_SELECTED || selectedTab in 1..5) {
                        IconButton(onClick = { showPrintDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Print, contentDescription = tr(R.string.msg_print_pdf_73a93ea), modifier = Modifier.size(20.dp))
                        }
                    }
                    if (selectedTab == 7 && syncState.status == SyncStatus.IDLE) {
                        IconButton(onClick = { syncViewModel.sync() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = tr(R.string.msg_synchronization_7ceddda),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = tr(R.string.msg_settings_7f17c7c),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = information.show,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = tr(R.string.msg_information_e2a0109),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == NO_TAB_SELECTED) {
                PullToSyncContainer(
                    onSync = { syncViewModel.sync() }
                ) {
                    TabsGrid(
                        tabs = MAIN_TABS,
                        isLandscape = isLandscape,
                        onTabSelected = { index -> selectedTab = index }
                    )
                }
            } else {
                if (flightsViewModel == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tr(R.string.msg_tab_1_s_6acc099, currentTabTitle ?: ""))
                    }
                } else {
                    tabStateHolder.SaveableStateProvider("$profileId:$selectedTab") {
                        CompositionLocalProvider(LocalFormularZoomState provides activeZoomState) {
                        when (selectedTab) {
                            0 -> FlightsTabWithViewModel(
                                profileId = profileId,
                                viewModel = flightsViewModel,
                                showToolbar = showFlightsToolbar,
                                onSync = { syncViewModel.sync() }
                            )
                            1 -> Chast1Tab(profileId = profileId)
                            2 -> Vr80Tab(profileId = profileId)
                            3 -> Ta14Tab(profileId = profileId)
                            4 -> Vk250001Tab(profileId = profileId)
                            5 -> Vk250002Tab(profileId = profileId)
                            6 -> ResourcesTab(
                                profileId = profileId,
                                onNavigateToAggregates = {
                                    onNavigateToAggregates(profileId)
                                },
                                onNavigateToOils = {
                                    onNavigateToOils(profileId)
                                },
                                onNavigateToAggregatesOnBoard = {         // ← НОВОЕ
                                    onNavigateToAggregatesOnBoard(profileId)
                                }
                            )
                            7 -> MaintenanceScreen(
                                profileId = profileId,
                                onSync = { syncViewModel.sync() }
                            )
                        }
                        }
                    }
                }
            }
        }
    }

    if (showSyncWarningDialog) {
        if (syncState.conflict != null) {
            SyncConflictDialog(
                conflict = syncState.conflict!!,
                busy = syncState.busy,
                message = com.example.logbook.localization.I18n.display(syncState.message),
                onDismiss = {
                    showSyncWarningDialog = false
                    syncViewModel.dismiss()
                    syncViewModel.resetStatus()
                },
                onChoose = { useLocal ->
                    syncViewModel.resolve(useLocal)
                    showSyncWarningDialog = false
                    syncViewModel.resetStatus()
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = {
                    showSyncWarningDialog = false
                    syncViewModel.resetStatus()
                },
                title = { Text(tr(R.string.msg_synchronization_7ceddda)) },
                text = { Text(com.example.logbook.localization.I18n.display(syncState.message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showSyncWarningDialog = false
                        syncViewModel.resetStatus()
                    }) {
                        Text(tr(R.string.msg_ok_e40fc0b))
                    }
                }
            )
        }
    }

    if (showSyncErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showSyncErrorDialog = false
                syncViewModel.resetStatus()
            },
            title = { Text(tr(R.string.msg_error_72aecd9)) },
            text = { Text(com.example.logbook.localization.I18n.display(syncState.message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSyncErrorDialog = false
                    syncViewModel.resetStatus()
                }) {
                    Text(tr(R.string.msg_ok_e40fc0b))
                }
            }
        )
    }

    if (showPrintDialog) {
        com.example.logbook.printing.FormularPrintDialog(profileId,
            com.example.logbook.printing.FORMULAR_TYPES.getOrNull(selectedTab - 1),
            onDismiss = { showPrintDialog = false })
    }

    if (showHistoryDialog && flightsViewModel != null) {
        com.example.logbook.ui.tabs.flights.HistoryDialog(
            profileId = profileId,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSetupAggregates && resourcesViewModel != null) {
        SetupFlowDialog(
            viewModel = resourcesViewModel,
            onDismiss = { showSetupAggregates = false }
        )
    }
}

@Composable
private fun SyncTopBarIndicator(
    status: SyncStatus,
    onWarningClick: () -> Unit,
    onErrorClick: () -> Unit
) {
    when (status) {
        SyncStatus.IDLE -> {}
        SyncStatus.SYNCING -> {
            val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = tr(R.string.msg_synchronization_7ceddda),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
        SyncStatus.SUCCESS -> {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        SyncStatus.WARNING -> {
            IconButton(
                onClick = onWarningClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFCC00),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        SyncStatus.ERROR -> {
            IconButton(
                onClick = onErrorClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================
// Сетка карточек вкладок
// ============================================================

@Composable
private fun TabsGrid(
    tabs: List<MainTab>,
    isLandscape: Boolean,
    onTabSelected: (Int) -> Unit
) {
    val columns = if (isLandscape) 4 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs, key = { it.index }) { tab ->
            TabCard(
                tab = tab,
                onClick = { onTabSelected(tab.index) }
            )
        }
    }
}

@Composable
private fun TabCard(
    tab: MainTab,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tab.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Главный экран — карточки (portrait)",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun MainScreenPreviewPortrait() {
    MaterialTheme {
        MainScreenContent(
            profile = ProfileEntity(
                id = 1,
                name = "Вертолёт 1",
                boardNumber = "12010"
            ),
            profileId = 1L,
            flightsViewModel = null,
            showSetupAggregatesOnStart = false,
            onBack = {}
        )
    }
}

@Preview(
    name = "Главный экран — карточки (landscape)",
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape"
)
@Composable
private fun MainScreenPreviewLandscape() {
    MaterialTheme {
        MainScreenContent(
            profile = ProfileEntity(
                id = 1,
                name = "Вертолёт 1",
                boardNumber = "12010"
            ),
            profileId = 1L,
            flightsViewModel = null,
            showSetupAggregatesOnStart = false,
            onBack = {}
        )
    }
}
