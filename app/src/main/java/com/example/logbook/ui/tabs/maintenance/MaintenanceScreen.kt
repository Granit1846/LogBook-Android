package com.example.logbook.ui.tabs.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logbook.R
import com.example.logbook.localization.I18n.tr
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.model.DefectTask
import com.example.logbook.data.model.MaintenanceJournal
import com.example.logbook.data.model.MaintenanceRepository
import com.example.logbook.data.model.MaintenanceWork
import com.example.logbook.data.model.RemainingWindow
import com.example.logbook.data.model.WorkDue
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

@Composable
fun MaintenanceScreen(
    profileId: Long,
    onSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getInstance(context) }
    val repository = remember(context, db) { MaintenanceRepository(context.applicationContext, db) }
    val profile by remember(db, profileId) { db.profileDao().observeById(profileId) }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var addWork by remember { mutableStateOf(false) }
    var addTaskFor by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (true) { delay(5_000); now = System.currentTimeMillis() } }

    val current = profile ?: return
    val works = try { MaintenanceJournal.works(current) } catch (e: Exception) {
        Text(tr(R.string.journal_read_error), modifier = Modifier.padding(16.dp)); return
    }
    val tasks = try { MaintenanceJournal.tasks(current) } catch (e: Exception) {
        Text(tr(R.string.journal_read_error), modifier = Modifier.padding(16.dp)); return
    }
    val visible = MaintenanceJournal.visibleTasks(current, now)
    val due = MaintenanceJournal.due(works, current.fuselageSne, LocalDate.now())
    val saveError = tr(R.string.journal_save_error)

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 680

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (isTabletLandscape) 1100.dp else 680.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }

            if (isTabletLandscape) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Левая колонка — регламентные работы + история + синхронизация
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(tr(R.string.journal_scheduled_works), style = MaterialTheme.typography.titleMedium)
                                    due.forEach { WorkDueRow(it, current.fuselageSne) }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { showHistory = true },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text(tr(R.string.journal_history))
                                }
                                OutlinedButton(
                                    onClick = onSync
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = tr(R.string.msg_synchronization_7ceddda),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tr(R.string.msg_synchronization_7ceddda))
                                }
                            }
                        }

                        // Правая колонка — неисправности
                        Card(modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr(R.string.journal_defects), style = MaterialTheme.typography.titleMedium)
                                if (visible.isEmpty()) Text(tr(R.string.journal_empty_defects))
                                visible.forEachIndexed { index, task ->
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    DefectCard(task, works.firstOrNull { it.id == task.workId }) {
                                        scope.launch {
                                            runCatching { repository.completeTask(profileId, task.id) }
                                                .onFailure { error = saveError }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr(R.string.journal_scheduled_works), style = MaterialTheme.typography.titleMedium)
                            due.forEach { WorkDueRow(it, current.fuselageSne) }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { showHistory = true },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(tr(R.string.journal_history))
                        }
                        OutlinedButton(
                            onClick = onSync
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = tr(R.string.msg_synchronization_7ceddda),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(R.string.msg_synchronization_7ceddda))
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr(R.string.journal_defects), style = MaterialTheme.typography.titleMedium)
                            if (visible.isEmpty()) Text(tr(R.string.journal_empty_defects))
                            visible.forEachIndexed { index, task ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DefectCard(task, works.firstOrNull { it.id == task.workId }) {
                                    scope.launch {
                                        runCatching { repository.completeTask(profileId, task.id) }
                                            .onFailure { error = saveError }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            OutlinedFloatingActionButton(
                onClick = { showAddMenu = true },
                modifier = Modifier.semantics {
                    contentDescription = tr(R.string.journal_add)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = tr(R.string.journal_add))
            }
            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                DropdownMenuItem(text = { Text(tr(R.string.journal_add_work)) }, onClick = {
                    showAddMenu = false; addWork = true
                })
                DropdownMenuItem(text = { Text(tr(R.string.journal_add_defect)) }, onClick = {
                    showAddMenu = false; addTaskFor = ""
                })
            }
        }
    }
    if (showHistory) HistoryDialog(
        works = works,
        completed = tasks.filter { it.completedAt > 0L },
        onClearHistory = {
            scope.launch {
                runCatching { repository.clearHistory(profileId) }
                    .onFailure { error = saveError }
            }
        },
        onDismiss = { showHistory = false }
    )
    if (addWork) AddWorkDialog(current.fuselageSne, onDismiss = { addWork = false }, onSave = { kind, date, counter ->
        scope.launch {
            runCatching { repository.addWork(profileId, kind, date, counter, "") }
                .onSuccess { addWork = false; error = "" }
                .onFailure { error = saveError }
        }
    })
    if (addTaskFor != null) AddTaskDialog(onDismiss = { addTaskFor = null }, onSave = { description ->
        val linked = addTaskFor.orEmpty()
        scope.launch {
            runCatching { repository.addTask(profileId, description, linked) }
                .onSuccess { addTaskFor = null; error = "" }
                .onFailure { error = saveError }
        }
    })
}

@Composable
private fun WorkDueRow(value: WorkDue, counterMinutes: Int) {
    val remaining = MaintenanceJournal.remaining(value, counterMinutes, LocalDate.now())
    val color = when (remaining?.third) {
        0 -> Color(0xFF2E7D32)
        1 -> Color(0xFFF9A825)
        2 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.outline
    }
    val detail = when {
        remaining == null -> tr(R.string.journal_no_baseline_triplet)
        else -> remainingLabel(remaining)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).background(color, CircleShape))
        Spacer(Modifier.width(9.dp))
        Text(kindLabel(value.kind), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun remainingLabel(value: RemainingWindow): String = if (value.hourly) {
    tr(R.string.journal_hours_triplet, value.lower.toString(), value.nominal.toString(),
        value.upper.toString(), MaintenanceJournal.roundHours(value.current).toString())
} else {
    tr(R.string.journal_days_triplet, value.lower.toString(), value.nominal.toString(),
        value.upper.toString(), value.current.toString())
}

@Composable
private fun HistoryDialog(
    works: List<MaintenanceWork>,
    completed: List<DefectTask>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val hasHistory = works.isNotEmpty() || completed.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(R.string.journal_history))
                if (hasHistory) {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = tr(R.string.msg_clear_history),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text(tr(R.string.msg_clear_history)) },
                    text = { Text(tr(R.string.msg_clear_history_confirm)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearConfirm = false
                            onClearHistory()
                        }) { Text(tr(R.string.msg_delete_86ea33a), color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text(tr(R.string.msg_cancel_0ec753b)) }
                    }
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(tr(R.string.journal_works), style = MaterialTheme.typography.titleMedium) }
                if (works.isEmpty()) item { Text(tr(R.string.journal_empty_works)) }
                items(works.sortedWith(compareByDescending<MaintenanceWork> { it.date }.thenByDescending { it.createdAt }), key = { "work_${it.id}" }) { work ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(kindLabel(work.kind), style = MaterialTheme.typography.titleSmall)
                            Text("${displayDate(work.date)} · ${MaintenanceJournal.formatHours(work.counterMinutes.toLong())}")
                            if (work.description.isNotBlank()) Text(work.description)
                        }
                    }
                }
                item { Text(tr(R.string.journal_completed_defects), style = MaterialTheme.typography.titleMedium) }
                if (completed.isEmpty()) item { Text(tr(R.string.journal_empty_history)) }
                items(completed.sortedByDescending { it.completedAt }, key = { "task_${it.id}" }) { task ->
                    DefectCard(task, works.firstOrNull { it.id == task.workId }, onComplete = {})
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.journal_close)) } }
    )
}

@Composable
private fun DefectCard(task: DefectTask, work: MaintenanceWork?, onComplete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.completedAt > 0L, enabled = task.completedAt == 0L, onCheckedChange = { if (it) onComplete() })
        Column(Modifier.padding(start = 4.dp)) {
            Text(task.description)
            if (work != null) Text("${kindLabel(work.kind)} · ${displayDate(work.date)}", style = MaterialTheme.typography.bodySmall)
            if (task.completedAt > 0L) Text(tr(R.string.journal_completed), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddWorkDialog(counterMinutes: Int, onDismiss: () -> Unit, onSave: (String, LocalDate, Int) -> Unit) {
    var kind by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var counter by remember { mutableStateOf(MaintenanceJournal.formatHours(counterMinutes.toLong())) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(tr(R.string.journal_add_work)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(kind?.let { kindLabel(it) } ?: tr(R.string.journal_choose_work))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    MaintenanceJournal.kinds.forEach { item ->
                        DropdownMenuItem(text = { Text(kindLabel(item)) }, onClick = { kind = item; menu = false })
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr(R.string.journal_date_value, date.format(dateFormat)))
                }
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                OutlinedTextField(counter, { counter = normalizeCounterInput(it) },
                    label = { Text(tr(R.string.journal_counter)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }))
                if (invalid) Text(tr(R.string.journal_invalid_input), color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { TextButton(onClick = {
            val parsedCounter = MaintenanceJournal.parseHours(counter)
            val selectedKind = kind
            if (selectedKind == null || parsedCounter == null) invalid = true
            else onSave(selectedKind, date, parsedCounter)
        }) { Text(tr(R.string.journal_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.journal_cancel)) } })
    if (showDatePicker) ScrollableDatePickerDialog(initialDate = date,
        onSelect = { date = it; showDatePicker = false }, onDismiss = { showDatePicker = false })
}

internal fun normalizeCounterInput(input: String): String = buildString {
    val punctuation = Regex("\\p{P}")
    input.forEach { char ->
        when {
            char in '0'..'9' -> append(char)
            punctuation.matches(char.toString()) && (isEmpty() || last() != ':') -> append(':')
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var description by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(tr(R.string.journal_add_defect)) },
        text = { OutlinedTextField(description, { description = it }, label = { Text(tr(R.string.journal_description)) }, maxLines = 5) },
        confirmButton = { TextButton(onClick = { if (description.isNotBlank()) onSave(description) }, enabled = description.isNotBlank()) { Text(tr(R.string.journal_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.journal_cancel)) } })
}

@Composable
private fun kindLabel(kind: String): String = tr(when (kind) {
    MaintenanceJournal.PERIODIC -> R.string.journal_periodic
    MaintenanceJournal.HOURS_50 -> R.string.journal_50
    MaintenanceJournal.HOURS_100 -> R.string.journal_100
    MaintenanceJournal.HOURS_300 -> R.string.journal_300
    MaintenanceJournal.REG_12 -> R.string.journal_12
    MaintenanceJournal.REG_24 -> R.string.journal_24
    else -> R.string.journal_other
})

private fun displayDate(iso: String): String = runCatching { LocalDate.parse(iso).format(dateFormat) }.getOrDefault(iso)
