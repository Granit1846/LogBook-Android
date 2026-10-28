package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.BuildConfig
import com.example.logbook.localization.I18n.tr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.updates.AppUpdates

class InformationActions(val show: () -> Unit, val check: () -> Unit, val version: String = "—")
val LocalInformationActions = staticCompositionLocalOf { InformationActions({}, {}) }

@Composable
fun AppInformationHost(autoCheck: Boolean?, content: @Composable () -> Unit) {
    val updates: AppUpdates = viewModel()
    val state by updates.state.collectAsStateWithLifecycle()
    var showInformation by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    fun openLink(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(context, tr(R.string.msg_could_not_open_the_link_228c903), Toast.LENGTH_LONG).show() }
    }
    val checkUpdates = {
        showInformation = false
        if (BuildConfig.IN_APP_UPDATES) updates.check()
        else openLink("https://www.rustore.ru/catalog/app/com.skeorz.lab.logbook")
    }
    LaunchedEffect(autoCheck) { if (BuildConfig.IN_APP_UPDATES && autoCheck == true) updates.check(automatic = true) }
    CompositionLocalProvider(LocalInformationActions provides InformationActions({ showInformation = true }, checkUpdates, updates.version)) { content() }
    if (showInformation) AlertDialog(onDismissRequest = { showInformation = false },
        title = { Text(tr(R.string.msg_about_logbook_b93832a)) },
        text = { Column(Modifier.heightIn(max = scrollHeight).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr(R.string.msg_android_version_1_s_build_2_s_bd1d8fb, updates.version, updates.versionCode), style = MaterialTheme.typography.titleSmall)
            Text(tr(R.string.msg_flights_and_five_logbooks_part_1_vr_80_ta_14_v_d6e9d9d))
            Text(tr(R.string.msg_component_and_oil_resources_replacement_histor_a511c04))
            Text(tr(R.string.msg_data_is_stored_on_your_device_exchange_with_th_49849c8))
            Text(tr(R.string.msg_save_a_backup_before_restoring_or_replacing_th_76c8305))
            TextButton(onClick = { openLink(AppUpdates.WEBSITE) }) { Text(tr(R.string.msg_website_and_guide_05b1cea)) }
        } },
        confirmButton = { TextButton(onClick = checkUpdates, enabled = !state.busy) { Text(tr(R.string.msg_check_for_updates_fa7bfc5)) } },
        dismissButton = { TextButton(onClick = { showInformation = false }) { Text(tr(R.string.msg_close_4ae50d3)) } })
    if (state.visible && !state.downloadDialogVisible) AlertDialog(onDismissRequest = updates::dismiss,
        title = { Text(tr(R.string.msg_logbook_updates_139c358)) },
        text = { Column(Modifier.heightIn(max = scrollHeight).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr(R.string.msg_installed_version_1_s_2_s_5111881, updates.version, updates.versionCode))
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(com.example.logbook.localization.I18n.display(state.message))
            state.release?.takeIf { state.isNewer }?.let { release ->
                if (release.localizedNotes.isNotBlank()) Text(release.localizedNotes)
                if (release.url.isBlank()) Text(tr(R.string.msg_the_apk_link_has_not_been_published_yet_7f0923a))
                else Text(tr(R.string.msg_download_the_apk_and_install_it_over_the_curre_5af14a4))
            }
        } },
        confirmButton = {
            val release = state.release
            if (release != null && state.isNewer && release.url.isNotBlank()) {
                Button(
                    onClick = {
                        try {
                            updates.startDownload(context)
                        } catch (_: Exception) {
                            openLink(release.url)
                        }
                    }
                ) {
                    Text(tr(R.string.msg_download_apk_31352fb))
                }
            } else if (!state.busy && release == null) {
                TextButton(onClick = { updates.check() }) { Text(tr(R.string.msg_retry_9e506ac)) }
            }
        },
        dismissButton = {
            TextButton(onClick = updates::dismiss) {
                Text(tr(R.string.msg_close_4ae50d3))
            }
        })

    if (state.downloadDialogVisible) AlertDialog(
        onDismissRequest = {
            if (!state.isDownloading) updates.dismissDownloadDialog()
        },
        title = {
            Text(
                if (state.downloadedFile != null) tr(R.string.msg_install_update)
                else tr(R.string.msg_downloading_update_prefix).removeSuffix("…")
            )
        },
        text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isDownloading) {
                val progressVal = (state.downloadProgress ?: 0f).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progressVal }, modifier = Modifier.fillMaxWidth())
                Text(state.downloadDetail, style = MaterialTheme.typography.bodyMedium)
            } else if (state.downloadedFile != null) {
                LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                Text(
                    com.example.logbook.localization.I18n.display("Обновление успешно загружено. Нажмите «Установить» для продолжения."),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.downloadError != null) {
                Text(state.downloadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { openLink(state.release?.url ?: AppUpdates.GITHUB_RELEASES_URL) }) {
                    Text(com.example.logbook.localization.I18n.display("Скачать через браузер"))
                }
            }
        } },
        confirmButton = {
            if (state.downloadedFile != null) {
                Button(
                    onClick = { updates.installDownloadedApk(context) }
                ) {
                    Text(tr(R.string.msg_install_update))
                }
            } else if (state.downloadError != null) {
                Button(
                    onClick = { updates.startDownload(context) }
                ) {
                    Text(tr(R.string.msg_retry_9e506ac))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (state.isDownloading) updates.cancelDownload()
                    else updates.dismissDownloadDialog()
                }
            ) {
                Text(tr(R.string.msg_cancel_0ec753b))
            }
        })
}
