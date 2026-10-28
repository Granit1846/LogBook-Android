package com.example.logbook.ui.settings

import com.example.logbook.R
import com.example.logbook.BuildConfig
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.AppSettingsEntity
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.util.LocalInformationActions
import com.example.logbook.ui.util.CompactTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRestored: () -> Unit = onBack
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = LogbookViewModelFactory(context, 0L)
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupViewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory(context))
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val importViewModel: DesktopImportViewModel = viewModel(factory = DesktopImportViewModel.Factory(context))
    val importState by importViewModel.state.collectAsStateWithLifecycle()

    val syncViewModel: SyncViewModel = viewModel(factory = SyncViewModel.Factory(context))
    val syncState by syncViewModel.state.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CompactTopBar(
                title = tr(R.string.msg_settings_7f17c7c),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { if (!backupState.busy && !importState.busy && !syncState.busy) onBack() }
            )

            if (settings == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tr(R.string.msg_loading_b6819e9))
                }
            } else {
                com.example.logbook.ui.util.PullToSyncContainer(
                    onSync = { syncViewModel.sync() },
                    enabled = !backupState.busy && !importState.busy && !syncState.busy
                ) { SettingsContent(
                    settings = settings!!,
                    onThemeChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setTheme(it) },
                    onNotificationsChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setNotifications(it) },
                    onLanguageChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setLanguage(it) },
                    onAutoSaveChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setAutoSave(it) },
                    onAutoCheckUpdatesChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setAutoCheckUpdates(it) },
                    onSyncOnFinishDayChange = { if (!backupState.busy && !importState.busy && !syncState.busy) viewModel.setSyncOnFinishDay(it) },
                    backupContent = { BackupSection(backupViewModel, enabled = !importState.busy && !syncState.busy) },
                    importContent = { DesktopImportSection(importViewModel, !backupState.busy && !syncState.busy) },
                    syncContent = { SyncSection(syncViewModel, !backupState.busy && !importState.busy, collapseInformation = true) }
                ) }
            }
        }
    }
    BackupDialogs(backupViewModel, onRestored)
    DesktopImportDialogs(importViewModel, onRestored)
    SyncDialogs(syncViewModel, onRestored)
}

@Composable
internal fun SettingsContent(
    settings: AppSettingsEntity,
    onThemeChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onSyncOnFinishDayChange: (Boolean) -> Unit,
    backupContent: @Composable () -> Unit,
    importContent: @Composable () -> Unit,
    syncContent: @Composable () -> Unit
) {
    val information = LocalInformationActions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 16.dp)
    ) {
        // ============================================================
        // Интерфейс
        // ============================================================
        SectionHeader(tr(R.string.msg_interface_8d7038f))

        DropdownSetting(
            label = tr(R.string.msg_language_ed3f0e5),
            current = settings.language,
            options = listOf(
                "ru" to tr(R.string.msg__5537fc8),
                "en" to "English"
            ),
            onSelect = onLanguageChange
        )

        DropdownSetting(
            label = tr(R.string.msg_theme_553c45f),
            current = settings.theme,
            options = listOf(
                "System" to tr(R.string.msg_system_afeb194),
                "Light" to tr(R.string.msg_light_8080010),
                "Dark" to tr(R.string.msg_dark_bd16b23),
                "Nexus" to tr(R.string.msg_nexus_dark_grey_orange_2b3b28e)
            ),
            onSelect = onThemeChange
        )

        SwitchSetting(
            label = tr(R.string.msg_notifications_ee3c35f),
            value = settings.notifications,
            onValueChange = onNotificationsChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ============================================================
        // Дополнительно
        // ============================================================
        var backupExpanded by rememberSaveable { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth()
                .semantics { stateDescription = if (backupExpanded) tr(R.string.msg_expanded_dc997d3) else tr(R.string.msg_collapsed_d2f1503) }
                .clickable(role = Role.Button) { backupExpanded = !backupExpanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr(R.string.msg_backup_72f931b), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (backupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null)
        }
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        AnimatedVisibility(visible = backupExpanded) { backupContent() }
        Spacer(modifier = Modifier.height(24.dp))

        importContent()
        Spacer(modifier = Modifier.height(24.dp))
        syncContent()
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(tr(R.string.msg_advanced_d7907f4))

        SwitchSetting(
            label = tr(R.string.msg_autosave_8cc4d11),
            value = settings.autoSave,
            onValueChange = onAutoSaveChange
        )

        if (BuildConfig.IN_APP_UPDATES) {
            SwitchSetting(
                label = tr(R.string.msg_check_for_updates_automatically_97ae112),
                value = settings.autoCheckUpdates,
                onValueChange = onAutoCheckUpdatesChange
            )
        }

        SwitchSetting(
            label = tr(R.string.msg_sync_on_finish_day),
            value = settings.syncOnFinishDay,
            onValueChange = onSyncOnFinishDayChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ============================================================
        // О программе
        // ============================================================
        SectionHeader(tr(R.string.msg_about_3da0b9e))

        SettingRow(
            label = tr(R.string.msg_version_97c248c),
            value = information.version
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = information.show,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr(R.string.msg_help_6a45cef))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = information.check,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr(R.string.msg_check_for_updates_fa7bfc5))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ============================================================
// Вспомогательные компоненты
// ============================================================

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SwitchSetting(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChange
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == current }?.second ?: current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(currentLabel)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            onSelect(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

