package com.example.logbook.ui.settings

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logbook.data.exchange.*
import com.example.logbook.data.local.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DesktopImportState(val busy: Boolean = false, val prepared: PreparedDesktopImport? = null,
    val message: String? = null, val completed: Boolean = false)

class DesktopImportViewModel(context: Context) : ViewModel() {
    private val context = context.applicationContext
    private val importer = DesktopImporter(this.context,AppDatabase.getInstance(this.context))
    private val mutable = MutableStateFlow(DesktopImportState())
    val state = mutable.asStateFlow()
    fun prepare(uri: Uri) = operation {
        val prepared = checkNotNull(context.contentResolver.openInputStream(uri)) { tr(R.string.msg_could_not_open_the_file_651ac1a) }.use { importer.prepare(it) }
        mutable.value = DesktopImportState(prepared=prepared)
    }
    fun apply(keys: Set<String>) {
        val prepared = mutable.value.prepared ?: return
        operation {
            val count = importer.apply(prepared,keys)
            mutable.value = DesktopImportState(completed=true,message=tr(R.string.msg_profiles_imported_1_s_previous_data_was_backed_a22b7a8, count))
        }
    }
    fun dismiss() { if (!mutable.value.busy) mutable.value = DesktopImportState() }
    private fun operation(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy=true,message=null)
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutable.value = DesktopImportState(message=error.message ?: tr(R.string.msg_could_not_import_data_12a57a8)) }
            finally { mutable.value = mutable.value.copy(busy=false) }
        }
    }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = DesktopImportViewModel(context) as T
    }
}

@Composable
fun DesktopImportSection(vm: DesktopImportViewModel, enabled: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::prepare) }
    var informationExpanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .semantics { stateDescription = if (informationExpanded) tr(R.string.msg_expanded_dc997d3) else tr(R.string.msg_collapsed_d2f1503) }
                .clickable(role = Role.Button) { informationExpanded = !informationExpanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr(R.string.msg_import_3f8c551), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (informationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null)
        }
        HorizontalDivider()
        AnimatedVisibility(visible = informationExpanded) {
            Text(tr(R.string.msg_first_transfer_from_work_data_json_existing_pr_c5ba054))
        }
        OutlinedButton(onClick={picker.launch(arrayOf("application/json","text/plain","application/octet-stream"))},enabled=enabled && !state.busy) {
            Text(tr(R.string.msg_import_from_the_pc_app_932fcb0))
        }
        if (state.busy && state.prepared == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.prepared == null && !state.completed) state.message?.let { Text(it,Modifier.padding(vertical=8.dp)) }
    }
}

@Composable
fun DesktopImportDialogs(vm: DesktopImportViewModel, onImported: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(enabled=state.busy) {}
    state.prepared?.let { prepared ->
        DesktopImportPreview(prepared.preview,prepared.archiveCount,state.busy,vm::dismiss,vm::apply)
    }
    if (state.completed) AlertDialog(onDismissRequest={vm.dismiss();onImported()}, title={Text(tr(R.string.msg_import_completed_f5dc647))},
        text={Text(com.example.logbook.localization.I18n.display(state.message.orEmpty()))},confirmButton={TextButton(onClick={vm.dismiss();onImported()}) {Text(tr(R.string.msg_go_to_profiles_c6654e2))} })
}

@Composable
fun DesktopImportPreview(profiles: List<DesktopProfilePreview>, archives: Int, busy: Boolean,
    onDismiss: () -> Unit, onApply: (Set<String>) -> Unit) {
    var selected by rememberSaveable(profiles) { mutableStateOf(ArrayList(profiles.filter { !it.blocked }.map { it.key })) }
    Dialog(onDismissRequest={if(!busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!busy,dismissOnClickOutside=!busy)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),contentAlignment=Alignment.Center) {
            Surface(Modifier.widthIn(max=600.dp).fillMaxWidth(),shape=MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr(R.string.msg_import_from_pc_92d7070),style=MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState())) {
                        Text(tr(R.string.msg_new_profiles_will_be_added_current_data_is_bac_bb0ac1a))
                        profiles.forEach { p ->
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(p.key in selected, { checked -> selected=ArrayList(if(checked) selected+p.key else selected-p.key) },enabled=!busy && !p.blocked)
                                Text(tr(R.string.msg_1_s_aircraft_2_s_3383991, p.name, p.board.ifBlank { tr(R.string.msg_not_specified) }),Modifier.weight(1f))
                            }
                            Text(tr(R.string.msg_days_1_s_logbook_rows_2_s_4ebacf3, p.days, p.rows))
                            if (p.blocked) Text(tr(R.string.msg_already_exists_import_skipped_74b69bd),color=MaterialTheme.colorScheme.error)
                            p.warnings.forEach { Text("• $it",style=MaterialTheme.typography.bodySmall) }
                            HorizontalDivider(Modifier.padding(vertical=8.dp))
                        }
                        Text(tr(R.string.msg_component_archives_in_file_1_s_existing_archiv_34ca8f6, archives))
                    }
                    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                        TextButton(onClick=onDismiss,enabled=!busy) { Text(tr(R.string.msg_cancel_0ec753b)) }
                        Button(onClick={onApply(selected.toSet())},enabled=!busy && selected.isNotEmpty()) { Text(tr(R.string.msg_import_55fd5f8)) }
                    }
                }
            }
        }
    }
}
