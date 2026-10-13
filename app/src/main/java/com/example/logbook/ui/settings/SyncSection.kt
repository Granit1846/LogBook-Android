package com.example.logbook.ui.settings

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logbook.data.exchange.*
import com.example.logbook.data.local.AppDatabase
import com.google.gson.JsonParser
import com.example.logbook.ui.util.bringIntoViewOnFocus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    WARNING,
    ERROR
}

data class SyncUiState(
    val busy: Boolean = false,
    val email: String = "",
    val lastSuccess: Long = 0,
    val message: String = "",
    val conflict: SyncConflict? = null,
    val downloaded: Boolean = false,
    val localNetwork: Boolean = false,
    val pairedPc: String = "",
    val history: List<SyncHistoryEntry> = emptyList(),
    val status: SyncStatus = SyncStatus.IDLE,
    val syncSeq: Long = 0L,
    val sharedSpace: Boolean = false,
    val sharedCatalog: List<SharedProfileInfo> = emptyList(),
    val showSharedPicker: Boolean = false
)

class SyncViewModel(context: Context): ViewModel() {
    private val context=context.applicationContext
    private val db=AppDatabase.getInstance(this.context)
    private val auth=SyncAuth(this.context)
    private val store=SyncStore(this.context,db)
    private val history=SyncHistory(this.context)
    private val mutable=MutableStateFlow(SyncUiState(email=auth.read()?.email.orEmpty()))
    val state=mutable.asStateFlow()
    private var pairing: LanPairing? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val last=currentLastSuccess()
            val entries=history.read()
            mutable.update { it.copy(lastSuccess=last,history=entries) }
        }
    }

    private suspend fun currentLastSuccess(): Long {
        if (mutable.value.localNetwork) return pairing?.let { store.checkpoint("lan:${it.peer}", it.peer).lastSuccess } ?: 0
        val session = auth.read() ?: return 0
        val account = if (mutable.value.sharedSpace) "shared" else session.user
        return store.checkpoint(session.endpoint, account).lastSuccess
    }

    private fun refreshLastSuccess() {
        viewModelScope.launch(Dispatchers.IO) {
            val last = currentLastSuccess()
            mutable.update { it.copy(lastSuccess = last) }
        }
    }

    private suspend fun client(shared: Boolean = mutable.value.sharedSpace): SupabaseSync {
        val settings=db.appSettingsDao().get()
        return SupabaseSync(
            settings?.supabaseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: SyncDefaults.URL,
            settings?.supabaseKey?.trim()?.takeIf { it.isNotBlank() } ?: SyncDefaults.KEY,
            auth,
            sharedSpace = shared
        )
    }

    private suspend fun engine(): SyncEngine {
        if(mutable.value.localNetwork) {
            val pc=pairing ?: error(tr(R.string.msg_scan_the_qr_code_in_the_pc_synchronization_win_af94f21))
            return SyncEngine(store,LanSync(pc),"lan:${pc.peer}",pc.peer)
        }
        val client=client(false)
        val session=auth.read() ?: error(tr(R.string.msg_sign_in_to_your_account_4eb58e1))
        return SyncEngine(store,client,session.endpoint,session.user)
    }

    fun signIn(email: String,password: String,register: Boolean) = operation {
        require(email.isNotBlank() && password.isNotBlank()) { tr(R.string.msg_enter_your_email_and_password_3c9c951) }
        val ready=client(false).signIn(email,password,register)
        refreshLastSuccess()
        mutable.update { it.copy(email=auth.read()?.email.orEmpty(),message=if(ready) tr(R.string.msg_signed_in_you_can_synchronize_data_now_ee8bbcb) else tr(R.string.msg_confirm_your_email_using_the_message_then_sign_7117ebd)) }
    }

    fun selectSharedSpace(shared: Boolean) {
        if (!mutable.value.busy) {
            mutable.update { it.copy(sharedSpace = shared, message = "") }
            refreshLastSuccess()
        }
    }

    fun dismissSharedPicker() {
        mutable.update { it.copy(showSharedPicker = false) }
    }

    fun downloadSharedProfiles(selectedIds: Set<String>) = operation {
        if (selectedIds.isEmpty()) {
            mutable.update { it.copy(showSharedPicker = false) }
            return@operation
        }
        val payloads = client(true).pullSharedProfiles(selectedIds)
        val msg = store.importSharedProfiles(payloads)
        history.add(SyncHistoryEntry(System.currentTimeMillis(), tr(R.string.msg_shared_space), tr(R.string.msg_received_shared_profiles), msg, ""))
        refreshLastSuccess()
        mutable.update { it.copy(message = msg, downloaded = true, showSharedPicker = false, status = SyncStatus.SUCCESS, syncSeq = it.syncSeq + 1) }
    }

    fun sync() = operation {
        if (!mutable.value.localNetwork && mutable.value.sharedSpace) {
            val catalog = client(true).sharedProfileCatalog()
            if (catalog.isEmpty()) {
                mutable.update { it.copy(message = tr(R.string.msg_no_shared_profiles), status = SyncStatus.IDLE) }
                return@operation
            }
            mutable.update { it.copy(sharedCatalog = catalog, showSharedPicker = true) }
            return@operation
        }
        exchange { engine().synchronize() }
        if (!mutable.value.localNetwork) {
            try {
                val packets = store.sharedProfilePackets()
                client(true).publishSharedProfiles(packets)
            } catch (_: Exception) {}
        }
    }

    fun selectLocal(local: Boolean) {
        if(!mutable.value.busy) {
            mutable.update { it.copy(localNetwork=local,message="",conflict=null,lastSuccess=0) }
            refreshLastSuccess()
        }
    }

    fun pair(text: String)=operation {
        pairing=LanPairing.parse(text)
        refreshLastSuccess()
        mutable.update { it.copy(pairedPc=pairing!!.host,message=tr(R.string.msg_pc_paired_tap_synchronize_over_wi_fi_e345dbc)) }
    }

    fun permissionDenied() {
        mutable.update { it.copy(message=tr(R.string.msg_to_exchange_data_over_wi_fi_allow_local_networ_d8d2f1d)) }
    }

    fun resolve(useLocal: Boolean) {
        val conflict=mutable.value.conflict ?: return
        operation { exchange { engine().resolve(conflict,useLocal) } }
    }

    private suspend fun exchange(block: suspend () -> SyncResult) {
        val method=if(mutable.value.localNetwork) "Wi-Fi" else tr(R.string.msg_internet_60a6800)
        try {
            val result=block()
            if(mutable.value.localNetwork) pairing?.let { pc ->
                val kind=if(result.downloaded) "received" else if(result.conflict!=null) "conflict" else if(result.direction=="Без изменений") "matched" else ""
                if(kind.isNotEmpty()) runCatching { LanSync(pc).report(kind) }
            }
            history.add(SyncHistoryEntry(System.currentTimeMillis(),method,
                if(result.conflict!=null) tr(R.string.msg_choose_version_175657a) else result.direction,result.message,result.backup))
            show(result)
        } catch(error: CancellationException) { throw error }
        catch(error: Exception) {
            history.add(SyncHistoryEntry(System.currentTimeMillis(),method,tr(R.string.msg_error_72aecd9),if(error is java.io.IOException) tr(R.string.msg_no_connection_exchange_was_not_completed_retry_9ae4d4f) else error.message ?: tr(R.string.msg_exchange_not_completed_cb9b202)))
            throw error
        } finally { mutable.update { it.copy(history=history.read()) } }
    }

    fun exportHistoryBackup(entry: SyncHistoryEntry, uri: android.net.Uri)=operation {
        require(entry.backup.matches(Regex("before-sync-[0-9]+-[0-9a-f-]+\\.json"))) { tr(R.string.msg_backup_unavailable_bde1f78) }
        val file=java.io.File(context.filesDir,"backups/sync/${entry.backup}")
        file.inputStream().use { input -> checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use { input.copyTo(it) } }
        mutable.update { it.copy(message=tr(R.string.msg_backup_saved_to_restore_it_select_it_in_the_ba_4f4c18a)) }
    }

    private suspend fun show(result: SyncResult) {
        val last = currentLastSuccess()
        val isWarn = result.conflict != null
        val st = if (isWarn) SyncStatus.WARNING else SyncStatus.SUCCESS
        mutable.update { it.copy(message=result.message,conflict=result.conflict,downloaded=result.downloaded,lastSuccess=last,status=st,syncSeq=it.syncSeq+1) }
    }

    fun resetStatus() {
        mutable.update { it.copy(status = SyncStatus.IDLE) }
    }

    fun dismiss() {
        if(!mutable.value.busy) {
            if(mutable.value.conflict!=null) {
                val local=mutable.value.localNetwork
                viewModelScope.launch(Dispatchers.IO) {
                    history.add(SyncHistoryEntry(System.currentTimeMillis(),if(local) "Wi-Fi" else tr(R.string.msg_internet_60a6800),tr(R.string.msg_cancelled_5ebee19),tr(R.string.msg_version_selection_cancelled_both_books_were_pr_404c161)))
                    if(local) pairing?.let { runCatching { LanSync(it).report("cancelled") } }
                    mutable.update { it.copy(history=history.read()) }
                }
            }
            mutable.update { it.copy(conflict=null,downloaded=false) }
        }
    }

    fun logout() {
        if(!mutable.value.busy) {
            auth.clear()
            mutable.value=SyncUiState(message=tr(R.string.msg_signed_out_8e4bf12),history=mutable.value.history)
        }
    }

    private fun operation(block: suspend () -> Unit) {
        if(mutable.value.busy) return
        mutable.update { it.copy(busy=true,status=SyncStatus.SYNCING,message="") }
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch(error: CancellationException) { throw error }
            catch(error: Exception) {
                val msg = if(error is java.io.IOException) tr(R.string.msg_no_server_connection_retry_synchronization_the_293fa8a)
                          else error.message ?: tr(R.string.msg_could_not_synchronize_data_e748a7f)
                mutable.update { it.copy(message=msg,status=SyncStatus.ERROR,syncSeq=it.syncSeq+1) }
            }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }

    class Factory(private val context: Context): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = SyncViewModel(context) as T
    }
}

@Composable
fun SyncSection(vm: SyncViewModel, enabled: Boolean, collapseInformation: Boolean = false) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context=androidx.compose.ui.platform.LocalContext.current
    val scanner=androidx.activity.compose.rememberLauncherForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
        result.contents?.let(vm::pair)
    }
    fun scan() { scanner.launch(com.journeyapps.barcodescanner.ScanOptions()
        .setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
        .setPrompt(tr(R.string.msg_scan_the_qr_code_in_the_pc_logbook_synchroniza_29b37b3))
        .setBeepEnabled(false).setOrientationLocked(true)) }
    val localPermission=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
        if(it) scan() else vm.permissionDenied()
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var informationExpanded by rememberSaveable { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if (collapseInformation) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .semantics { stateDescription = if (informationExpanded) tr(R.string.msg_expanded_dc997d3) else tr(R.string.msg_collapsed_d2f1503) }
                    .clickable(role = Role.Button) { informationExpanded = !informationExpanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(R.string.msg_synchronization_7ceddda), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (informationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null)
            }
            HorizontalDivider()
            AnimatedVisibility(visible = informationExpanded) { SyncInformation() }
        } else {
            Text(tr(R.string.msg_pc_android_synchronization_c086220),style=MaterialTheme.typography.titleMedium)
            SyncInformation()
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=!state.localNetwork,onClick={vm.selectLocal(false)},label={Text(tr(R.string.msg_over_the_internet_756318c))},enabled=enabled && !state.busy)
            FilterChip(selected=state.localNetwork,onClick={vm.selectLocal(true)},label={Text(tr(R.string.msg_over_wi_fi_39376e9))},enabled=enabled && !state.busy)
        }
        if(state.localNetwork) {
            Text(tr(R.string.msg_on_the_pc_open_synchronization_pair_android_ov_b1ce8bc))
            OutlinedButton(onClick={
                if(android.os.Build.VERSION.SDK_INT>=37 && androidx.core.content.ContextCompat.checkSelfPermission(context,"android.permission.ACCESS_LOCAL_NETWORK")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                    localPermission.launch("android.permission.ACCESS_LOCAL_NETWORK")
                else scan()
            },enabled=enabled && !state.busy) { Text(tr(R.string.msg_scan_pc_qr_code_2a41a30)) }
            if(state.pairedPc.isNotBlank()) {
                Text(tr(R.string.msg_pc_1_s_e7c13c1, state.pairedPc))
                Button(onClick=vm::sync,enabled=enabled && !state.busy) { Text(tr(R.string.msg_synchronize_over_wi_fi_56751d4)) }
            }
        } else if(state.email.isBlank()) {
            OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,enabled=enabled && !state.busy,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth().bringIntoViewOnFocus())
            OutlinedTextField(password,{password=it},label={Text(tr(R.string.msg_password_14f7c63))},singleLine=true,enabled=enabled && !state.busy,
                visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth().bringIntoViewOnFocus())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={vm.signIn(email,password,false);password=""},enabled=enabled && !state.busy) { Text(tr(R.string.msg_sign_in_939e95a)) }
                OutlinedButton(onClick={vm.signIn(email,password,true);password=""},enabled=enabled && !state.busy) { Text(tr(R.string.msg_register_7224765)) }
            }
        } else {
            Text(tr(R.string.msg_account_1_s_e20a047, state.email))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                FilterChip(
                    selected = !state.sharedSpace,
                    onClick = { vm.selectSharedSpace(false) },
                    label = { Text(tr(R.string.msg_personal_space)) },
                    enabled = enabled && !state.busy
                )
                FilterChip(
                    selected = state.sharedSpace,
                    onClick = { vm.selectSharedSpace(true) },
                    label = { Text(tr(R.string.msg_shared_space)) },
                    enabled = enabled && !state.busy
                )
            }
            if (state.sharedSpace) {
                Text(
                    tr(R.string.msg_shared_space_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if(state.lastSuccess>0) {
                Text(tr(R.string.msg_last_exchange_fc33be1)+java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",java.util.Locale.getDefault()).format(java.util.Date(state.lastSuccess)))
            }
            Button(onClick=vm::sync,enabled=enabled && !state.busy) {
                Text(if (state.sharedSpace) tr(R.string.msg_choose_shared_profiles) else tr(R.string.msg_synchronize_now_5822fb7))
            }
            TextButton(onClick=vm::logout,enabled=enabled && !state.busy) { Text(tr(R.string.msg_sign_out_026abb1)) }
        }
        if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(com.example.logbook.localization.I18n.display(state.message).isNotBlank()) Text(com.example.logbook.localization.I18n.display(state.message))
        OutlinedButton(onClick={showHistory=true},enabled=enabled && !state.busy) { Text(tr(R.string.msg_synchronization_history_236ecb4)) }
    }
    if(showHistory) SyncHistoryDialog(vm,onDismiss={showHistory=false})
}

@Composable
private fun SyncHistoryDialog(vm: SyncViewModel,onDismiss: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<SyncHistoryEntry?>(null) }
    val export=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) {
        uri -> if(uri!=null) selected?.let { vm.exportHistoryBackup(it,uri) }
    }
    Dialog(onDismissRequest={if(!state.busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!state.busy,dismissOnClickOutside=!state.busy)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),contentAlignment=Alignment.Center) {
            Surface(Modifier.widthIn(max=640.dp).fillMaxWidth(),shape=MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr(R.string.msg_synchronization_history_236ecb4),style=MaterialTheme.typography.titleLarge)
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f,false)) {
                        if(state.history.isEmpty()) item { Text(tr(R.string.msg_no_exchanges_yet_9375d00),Modifier.padding(vertical=16.dp)) }
                        items(state.history.size) { index ->
                            val entry=state.history[index]
                            Column(Modifier.padding(vertical=8.dp)) {
                                Text(java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date(entry.time))+" · ${com.example.logbook.localization.I18n.display(entry.method)}")
                                Text(com.example.logbook.localization.I18n.display(entry.direction),style=MaterialTheme.typography.titleSmall)
                                Text(com.example.logbook.localization.I18n.display(entry.result))
                                if(entry.backup.isNotBlank()) TextButton(onClick={selected=entry;export.launch(entry.backup)},enabled=!state.busy) { Text(tr(R.string.msg_save_backup_123ed53)) }
                            }
                            HorizontalDivider()
                        }
                    }
                    if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(com.example.logbook.localization.I18n.display(state.message).isNotBlank()) Text(com.example.logbook.localization.I18n.display(state.message))
                    TextButton(onClick=onDismiss,enabled=!state.busy,modifier=Modifier.align(Alignment.End)) { Text(tr(R.string.msg_close_4ae50d3)) }
                }
            }
        }
    }
}

@Composable
private fun SyncInformation() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr(R.string.msg_internet_manual_exchange_through_supabase_sign_d01de59))
        Text(tr(R.string.msg_wi_fi_connect_both_devices_to_the_same_network_c4e67fb))
        Text(tr(R.string.msg_tap_synchronize_now_on_the_device_with_new_dat_28161d5))
        Text(tr(R.string.msg_the_exchange_includes_all_profiles_if_both_dev_9698633))
    }
}

@Composable
fun FlightsSyncDialog(
    onDismiss: () -> Unit,
    onDownloaded: () -> Unit,
    autoStart: Boolean = false,
    silent: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: SyncViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "flights_sync", factory = SyncViewModel.Factory(context)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var hasAutoStarted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(autoStart) {
        if (autoStart && !hasAutoStarted && !state.busy) {
            hasAutoStarted = true
            vm.sync()
        }
    }

    if (silent) {
        LaunchedEffect(state.busy, hasAutoStarted, state.conflict, state.downloaded) {
            if (hasAutoStarted && !state.busy && state.conflict == null && !state.downloaded) {
                onDismiss()
            }
        }
    } else {
        Dialog(
            onDismissRequest = { if (!state.busy) onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false,
                dismissOnBackPress = !state.busy, dismissOnClickOutside = !state.busy)
        ) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(8.dp),
                contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.padding(16.dp)) {
                        Column(Modifier.weight(1f, false).verticalScroll(rememberScrollState())) {
                            SyncSection(vm, enabled = true)
                        }
                        TextButton(onClick = onDismiss, enabled = !state.busy,
                            modifier = Modifier.align(Alignment.End)) { Text(tr(R.string.msg_close_4ae50d3)) }
                    }
                }
            }
        }
    }
    SyncDialogs(vm, onDownloaded, silent = silent)
}

@Composable
fun SharedProfilePickerDialog(
    catalog: List<SharedProfileInfo>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selected by remember(catalog) { mutableStateOf(catalog.map { it.profileId }.toSet()) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(tr(R.string.msg_choose_shared_profiles)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text(
                    tr(R.string.msg_only_selected_profiles_will_be_downloaded),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                catalog.forEach { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !busy) {
                                selected = if (info.profileId in selected) selected - info.profileId else selected + info.profileId
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = info.profileId in selected,
                            onCheckedChange = { isChecked ->
                                selected = if (isChecked) selected + info.profileId else selected - info.profileId
                            },
                            enabled = !busy
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(info.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (info.boardNumber.isNotBlank()) {
                                Text(tr(R.string.msg_board_number_value, info.boardNumber), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                enabled = !busy && selected.isNotEmpty()
            ) {
                Text(tr(R.string.msg_download_selected_profiles))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(tr(R.string.msg_cancel_0ec753b))
            }
        }
    )
}

@Composable
fun SyncDialogs(vm: SyncViewModel, onDownloaded: () -> Unit, silent: Boolean = false) {
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(state.busy) {}
    state.conflict?.let { SyncConflictDialog(it,state.busy,com.example.logbook.localization.I18n.display(state.message),vm::dismiss,vm::resolve) }
    if (state.showSharedPicker) {
        SharedProfilePickerDialog(
            catalog = state.sharedCatalog,
            busy = state.busy,
            onDismiss = vm::dismissSharedPicker,
            onConfirm = vm::downloadSharedProfiles
        )
    }
    if (state.downloaded) {
        if (silent) {
            LaunchedEffect(Unit) {
                vm.dismiss()
                onDownloaded()
            }
        } else {
            AlertDialog(
                onDismissRequest = { vm.dismiss() },
                title = { Text(tr(R.string.msg_synchronization_completed_200fc60)) },
                text = { Text(com.example.logbook.localization.I18n.display(state.message)) },
                confirmButton = {
                    TextButton(onClick = { vm.dismiss(); onDownloaded() }) {
                        Text(tr(R.string.msg_go_to_profiles_c6654e2))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vm.dismiss() }) {
                        Text(tr(R.string.msg_dialog_exit))
                    }
                }
            )
        }
    }
}

@Composable
fun SyncConflictDialog(conflict: SyncConflict,busy: Boolean,message: String,onDismiss: () -> Unit,onChoose: (Boolean) -> Unit) {
    val summary=remember(conflict, com.example.logbook.localization.I18n.language) { conflictSummary(conflict) }
    Dialog(onDismissRequest={if(!busy) onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!busy,dismissOnClickOutside=!busy)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),contentAlignment=Alignment.Center) {
            Surface(Modifier.widthIn(max=640.dp).fillMaxWidth(),shape=MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr(R.string.msg_synchronization_conflict_e7ad254),style=MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState())) {
                        Text(tr(R.string.msg_both_versions_have_changes_your_choice_applies_97a0363))
                        Text(tr(R.string.msg_server_revision_1_s_f88538f, conflict.revision))
                        Text(summary,Modifier.padding(vertical=12.dp))
                        if(message.isNotBlank()) Text(com.example.logbook.localization.I18n.display(message))
                    }
                    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={onChoose(true)},enabled=!busy,modifier=Modifier.weight(1f)) { Text(tr(R.string.msg_keep_android_version_0f1eb1b)) }
                        OutlinedButton(onClick={onChoose(false)},enabled=!busy,modifier=Modifier.weight(1f)) { Text(tr(R.string.msg_download_server_version_646a0ee)) }
                    }
                    TextButton(onClick=onDismiss,enabled=!busy,modifier=Modifier.align(Alignment.End)) { Text(tr(R.string.msg_cancel_0ec753b)) }
                }
            }
        }
    }
}

private fun conflictSummary(conflict: SyncConflict): String = runCatching {
    val local=JsonParser.parseString(conflict.local).asJsonObject.getAsJsonObject("tables")
    val remote=JsonParser.parseString(conflict.remote).asJsonObject.getAsJsonObject("tables")
    val names=mapOf("profiles" to tr(R.string.msg_profiles_81acc03),"days" to tr(R.string.msg_days_46a1378),"launches" to tr(R.string.msg_flights_48b357c),"snk_values" to tr(R.string.msg_snk_4087caf),
        "chast1_records" to com.example.logbook.localization.I18n.display("Часть 1"),"vr80_records" to com.example.logbook.localization.I18n.display("ВР-80"),"ta14_records" to com.example.logbook.localization.I18n.display("Та-14"),"vk250001_records" to com.example.logbook.localization.I18n.display("ВК-2500-01"),"vk250002_records" to com.example.logbook.localization.I18n.display("ВК-2500-02"))
    buildString {
        appendLine(tr(R.string.msg_count_android_server_4234fd9))
        names.forEach { (table,name) -> appendLine("$name: ${local.getAsJsonArray(table).size()} / ${remote.getAsJsonArray(table).size()}") }
        appendLine(tr(R.string.msg_changed_rows_75d393a))
        var shown=0;var total=0
        SyncStore.TABLES.forEach { table ->
            val a=local.getAsJsonArray(table).associate { it.asJsonObject.get("id").asString to it.asJsonObject }
            val b=remote.getAsJsonArray(table).associate { it.asJsonObject.get("id").asString to it.asJsonObject }
            (a.keys+b.keys).forEach { id -> if(a[id]!=b[id]) {
                total++
                if(shown++<30) {
                    val row=a[id] ?: b.getValue(id)
                    val label=row.get("date")?.asString ?: row.get("name")?.asString ?: id.take(8)
                    val fields=if(a[id]!=null && b[id]!=null) a.getValue(id).keySet().filter { a[id]?.get(it)!=b[id]?.get(it) }.joinToString(", ") else if(a[id]==null) tr(R.string.msg_only_on_server_004758a) else tr(R.string.msg_only_on_android_b95f329)
                    appendLine("${names[table] ?: table}, $label: $fields")
                    if(a[id]!=null && b[id]!=null) a.getValue(id).keySet().filter { a[id]?.get(it)!=b[id]?.get(it) }.take(5).forEach { field ->
                        val left=a[id]?.get(field)?.toString()?.take(120);val right=b[id]?.get(field)?.toString()?.take(120)
                        appendLine(tr(R.string.msg_1_s_2_s_3_s_android_server_5324951, field, left, right))
                    }
                }
            } }
        }
        if(total>30) appendLine(tr(R.string.msg_more_differences_1_s_80dd318, total-30))
    }
}.getOrDefault(tr(R.string.msg_packets_differ_check_data_on_both_devices_befo_82d9d04))
