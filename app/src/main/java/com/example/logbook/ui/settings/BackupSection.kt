package com.example.logbook.ui.settings

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.example.logbook.data.backup.BackupFolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSection(viewModel: BackupViewModel, enabled: Boolean = true) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingSafety by rememberSaveable { mutableStateOf(false) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) viewModel.selectFolderAndExport(it, pendingSafety)
    }
    val restoreContract = remember(viewModel) {
        object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<String>): Intent =
                super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, viewModel.restoreInitialUri())
        }
    }
    val restore = rememberLauncherForActivityResult(restoreContract) {
        if (it != null) viewModel.prepare(it)
    }
    Column {
        Text(tr(R.string.msg_all_profiles_flights_logbooks_components_oils__bf23e05))
        Spacer(Modifier.height(8.dp))
        Text(tr(R.string.msg_folder_1_s_9a1f835, BackupFolder.PATH))
        Text(tr(R.string.msg_on_the_first_save_choose_documents_and_grant_a_d6679d9))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = enabled && !state.busy,
            onClick = {
                if (viewModel.hasFolderAccess()) viewModel.export()
                else { pendingSafety = false; folder.launch(BackupFolder.DOCUMENTS_URI) }
            }) {
            Text(tr(R.string.msg_save_backup_123ed53))
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = enabled && !state.busy,
            onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
            Text(tr(R.string.msg_restore_from_file_ed04dee))
        }
        if (state.hasSafetyCopy) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = enabled && !state.busy,
                onClick = {
                    if (viewModel.hasFolderAccess()) viewModel.export(safetyCopy = true)
                    else { pendingSafety = true; folder.launch(BackupFolder.DOCUMENTS_URI) }
                }) {
                Text(tr(R.string.msg_save_the_backup_made_before_the_last_restore_991942a))
            }
        }
        if (state.busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(tr(R.string.msg_processing_backup_9ea1abe))
        }
    }
}

@Composable
fun BackupDialogs(viewModel: BackupViewModel, onRestored: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.busy) { }
    state.prepared?.let { backup ->
        if (!state.busy) AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(tr(R.string.msg_restore_data_716fb7f)) },
            text = { Text(tr(R.string.msg_backup_from_1_s_eba5fc2, SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(Date(backup.createdAt))) +
                tr(R.string.msg_profiles_1_s_days_2_s_7822d5b, backup.profiles, backup.days) +
                tr(R.string.msg_current_data_for_all_profiles_will_be_replaced_08a8986)) },
            confirmButton = { TextButton(onClick = viewModel::restore) { Text(tr(R.string.msg_restore_29f3b29)) } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(tr(R.string.msg_cancel_0ec753b)) } }
        )
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = { if (state.restored) onRestored() else viewModel.dismiss() },
            title = { Text(if (state.restored) tr(R.string.msg_restore_completed_9947520) else tr(R.string.msg_backup_72f931b)) },
            text = { Text(com.example.logbook.localization.I18n.display(message)) },
            confirmButton = { TextButton(onClick = {
                if (state.restored) onRestored() else viewModel.dismiss()
            }) { Text(tr(R.string.msg_ok_e40fc0b)) } }
        )
    }
}

