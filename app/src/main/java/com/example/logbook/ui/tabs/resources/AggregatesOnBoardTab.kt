package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logbook.data.model.AggregateReplacement
import com.example.logbook.ui.util.CompactTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.logbook.data.model.ResourceDates
import com.example.logbook.ui.util.formatDateInput
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ============================================================
// Диалог «Агрегаты на борту» с ViewModel
// ============================================================

@Composable
fun AggregatesOnBoardDialog(
    viewModel: ResourcesViewModel,
    onDismiss: () -> Unit
) {
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var showSetupFlow by remember { mutableStateOf(false) }
    var replaceType by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    if (!showSetupFlow) AggregateOperationFeedback(viewModel)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AggregatesOnBoardContent(
            assignments = assignments,
            onReplace = { type -> replaceType = type },
            onSetup = { showSetupFlow = true },
            onHistory = { showHistory = true },
            onDismiss = onDismiss
        )
    }

    if (showSetupFlow) {
        SetupFlowDialog(
            viewModel = viewModel,
            onDismiss = { showSetupFlow = false }
        )
    }

    replaceType?.let { type ->
        ReplaceAggregateDialog(
            type = type,
            currentNumber = assignments[type] ?: "",
            onConfirm = { newNumber, reason, installationDate ->
                viewModel.replaceAggregate(type, newNumber, reason, installationDate) { replaceType = null }
            },
            onDismiss = { replaceType = null }
        )
    }

    if (showHistory) {
        HistoryDialog(
            history = history,
            onDismiss = { showHistory = false }
        )
    }
}

// ============================================================
// Содержимое диалога
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AggregatesOnBoardContent(
    assignments: Map<String, String>,
    onReplace: (String) -> Unit,
    onSetup: () -> Unit,
    onHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactTopBar(
                title = tr(R.string.msg_installed_components_0378de4),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onDismiss
            )

            // Строки — со скроллом
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                AggregateRow(
                    title = tr(R.string.msg_part_1_aircraft_ce83e52),
                    number = assignments["Chast1"] ?: "",
                    canReplace = false,
                    onReplace = { }
                )
                AggregateRow(
                    title = "ВР-80",
                    number = assignments["Vr80"] ?: "",
                    canReplace = true,
                    onReplace = { onReplace("Vr80") }
                )
                AggregateRow(
                    title = "Та-14",
                    number = assignments["Ta14"] ?: "",
                    canReplace = true,
                    onReplace = { onReplace("Ta14") }
                )
                AggregateRow(
                    title = "ВК-2500-01",
                    number = assignments["Vk250001"] ?: "",
                    canReplace = true,
                    onReplace = { onReplace("Vk250001") }
                )
                AggregateRow(
                    title = "ВК-2500-02",
                    number = assignments["Vk250002"] ?: "",
                    canReplace = true,
                    onReplace = { onReplace("Vk250002") }
                )
            }

            // Кнопки — закреплены внизу
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr(R.string.msg_replacement_history_2be0040))
                }
                Button(
                    onClick = onSetup,
                    enabled = listOf("Chast1", "Vr80", "Ta14", "Vk250001", "Vk250002")
                        .any { assignments[it].isNullOrBlank() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr(R.string.msg_set_up_components_1959ea4), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AggregateRow(
    title: String,
    number: String,
    canReplace: Boolean,
    onReplace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        Text(
            text = number.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (number.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = tr(R.string.msg_component_assigned_f5ae699),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = tr(R.string.msg_new_0f9ccee),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (canReplace && number.isNotBlank()) {
            IconButton(onClick = onReplace, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = tr(R.string.msg_replace_d772cc5),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
    HorizontalDivider()
}

// ============================================================
// Диалог 1: Ввод серийного номера вертолёта
// ============================================================

@Composable
fun SetupFlowDialog(
    viewModel: ResourcesViewModel,
    onDismiss: () -> Unit
) {
    AggregateOperationFeedback(viewModel)
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    var boardNumber by remember { mutableStateOf(assignments["Chast1"].orEmpty()) }
    var showConfirm by remember { mutableStateOf(false) }

    if (!showConfirm) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = true)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = tr(R.string.msg_enter_the_helicopter_serial_number_90369e1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = boardNumber,
                        onValueChange = { text ->
                            boardNumber = text.filter { it.isDigit() }.take(6)
                        },
                        label = { Text(tr(R.string.msg_helicopter_9b42e3b)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showConfirm = true },
                            enabled = boardNumber.length in 5..6,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr(R.string.msg_next_9c714cf))
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr(R.string.msg_cancel_0ec753b))
                        }
                    }
                }
            }
        }
    }

    if (showConfirm) {
        ConfirmAggregatesDialog(
            initialBoardNumber = boardNumber,
            initialAssignments = assignments,
            onConfirm = { map ->
                viewModel.saveAssignments(map, onSuccess = onDismiss)
            },
            onDismiss = onDismiss
        )
    }
}

// ============================================================
// Диалог 2: Проверьте и подтвердите агрегаты
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmAggregatesDialog(
    initialBoardNumber: String,
    initialAssignments: Map<String, String>,
    onConfirm: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val types = listOf(
        "Chast1" to tr(R.string.msg_helicopter_9b42e3b),
        "Vr80" to com.example.logbook.localization.I18n.display("ВР-80"),
        "Ta14" to com.example.logbook.localization.I18n.display("Та-14"),
        "Vk250001" to com.example.logbook.localization.I18n.display("ВК-2500-01"),
        "Vk250002" to com.example.logbook.localization.I18n.display("ВК-2500-02")
    )

    var values by remember {
        mutableStateOf(
            types.associate { (type, _) ->
                type to (initialAssignments[type] ?: if (type == "Chast1") initialBoardNumber else "")
            }
        )
    }

    var editing by remember { mutableStateOf(false) }
    val allFilled = values.values.all { it.isNotBlank() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CompactTopBar(
                    title = tr(R.string.msg_installed_components_0378de4),
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr(R.string.msg_check_and_confirm_components_21f745a),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    types.forEach { (type, title) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.width(140.dp)
                            )
                            OutlinedTextField(
                                value = values[type] ?: "",
                                onValueChange = { text ->
                                    if (editing || type == "Chast1") {
                                        values = values.toMutableMap().apply {
                                            this[type] = if (type == "Chast1") text.filter { it.isDigit() } else text.take(64)
                                        }
                                    }
                                },
                                enabled = editing || type == "Chast1",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = if (type == "Chast1") KeyboardType.Number else KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (initialAssignments[type] == values[type] && !values[type].isNullOrBlank()) tr(R.string.msg_assigned_4d9f828) else tr(R.string.msg_new_3697bef),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editing = !editing },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr(R.string.msg_edit_901beb5))
                        }
                        Button(
                            onClick = { onConfirm(values) },
                            enabled = allFilled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr(R.string.msg_confirm_0467ae4))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Диалог замены агрегата
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplaceAggregateDialog(
    type: String,
    currentNumber: String,
    onConfirm: (String, String, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val typeName = when (type) {
        "Vr80" -> "ВР-80"
        "Ta14" -> "Та-14"
        "Vk250001" -> "ВК-2500-01"
        "Vk250002" -> "ВК-2500-02"
        else -> type
    }

    var newNumber by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yy") }
    var installationText by rememberSaveable { mutableStateOf(LocalDate.now().format(dateFormat)) }
    val installationDate = ResourceDates.parse(installationText)
    var showInstallationPicker by remember { mutableStateOf(false) }
    if (showInstallationPicker) {
        ScrollableDatePickerDialog(initialDate = installationDate ?: LocalDate.now(),
            onSelect = { installationText = it.format(dateFormat); showInstallationPicker = false },
            onDismiss = { showInstallationPicker = false })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr(R.string.msg_replace_component_1_s_eaa59d5, typeName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(R.string.msg_current_serial_number_ac03108),
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        text = currentNumber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(R.string.msg_new_serial_number_dcb9338),
                        modifier = Modifier.width(120.dp)
                    )
                    OutlinedTextField(
                        value = newNumber,
                        onValueChange = { text ->
                            newNumber = text.take(64)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = tr(R.string.msg_reason_4a9ea52),
                        modifier = Modifier
                            .width(120.dp)
                            .padding(top = 12.dp)
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        minLines = 3,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = installationText,
                    onValueChange = { installationText = formatDateInput(it) },
                    label = { Text(tr(R.string.msg_installation_date_e9ca0fa)) },
                    singleLine = true,
                    isError = installationDate == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { showInstallationPicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = tr(R.string.msg_choose_installation_date_d7c2aaa))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr(R.string.msg_the_removed_component_s_logbook_will_be_saved__d864e25),
                    style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr(R.string.msg_cancel_0ec753b))
                    }
                    Button(
                        onClick = { installationDate?.let { onConfirm(newNumber, reason, it) } },
                        enabled = newNumber.isNotBlank() && newNumber.trim() != currentNumber.trim() && installationDate != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr(R.string.msg_replace_d772cc5))
                    }
                }
            }
        }
    }
}

// ============================================================
// Диалог истории замен
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDialog(
    history: List<AggregateReplacement>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.ROOT)

    val typeOrder = listOf("Vr80", "Ta14", "Vk250001", "Vk250002")
    val typeNames = mapOf(
        "Vr80" to com.example.logbook.localization.I18n.display("ВР-80"),
        "Ta14" to com.example.logbook.localization.I18n.display("Та-14"),
        "Vk250001" to com.example.logbook.localization.I18n.display("ВК-2500-01"),
        "Vk250002" to com.example.logbook.localization.I18n.display("ВК-2500-02")
    )

    val grouped = typeOrder.associateWith { type ->
        history.filter { it.type == type }.sortedByDescending { it.date }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CompactTopBar(
                    title = tr(R.string.msg_installed_components_0378de4),
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onDismiss
                )

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tr(R.string.msg_history_is_empty_724b4cd))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        typeOrder.forEach { type ->
                            val list = grouped[type] ?: emptyList()
                            if (list.isNotEmpty()) {
                                item(key = "header_$type") {
                                    Text(
                                        text = typeNames[type] ?: type,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(
                                            top = 16.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                }
                                items(
                                    items = list,
                                    key = { "${type}_${it.date}_${it.newNumber}" }
                                ) { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${item.oldNumber} → ${item.newNumber}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.reason.isNotBlank()) {
                                            Text(
                                                text = tr(R.string.msg_reason_1_s_d2f849f, item.reason),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        if (!item.installationDate.isNullOrBlank()) {
                                            Text(tr(R.string.msg_installation_date_1_s_b7d6f60, LocalDate.parse(item.installationDate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
                                        }
                                        Text(
                                            text = dateFormat.format(Date(item.date)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Агрегаты на борту — пустая",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun AggregatesOnBoardPreviewEmpty() {
    MaterialTheme {
        AggregatesOnBoardContent(
            assignments = emptyMap(),
            onReplace = {},
            onSetup = {},
            onHistory = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Агрегаты на борту — заполнено",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun AggregatesOnBoardPreviewFilled() {
    MaterialTheme {
        AggregatesOnBoardContent(
            assignments = mapOf(
                "Chast1" to "12008",
                "Vr80" to "11",
                "Ta14" to "12",
                "Vk250001" to "13",
                "Vk250002" to "14"
            ),
            onReplace = {},
            onSetup = {},
            onHistory = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "История замен — пусто",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun HistoryPreviewEmpty() {
    MaterialTheme {
        HistoryPreviewContent(history = emptyList())
    }
}

@Preview(
    name = "История замен — заполнено",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun HistoryPreviewFilled() {
    val now = System.currentTimeMillis()
    MaterialTheme {
        HistoryPreviewContent(
            history = listOf(
                AggregateReplacement(
                    type = "Vr80",
                    oldNumber = "11",
                    newNumber = "18",
                    reason = "Износ",
                    date = now
                ),
                AggregateReplacement(
                    type = "Vr80",
                    oldNumber = "10",
                    newNumber = "11",
                    reason = "Плановая замена",
                    date = now - 86400000
                ),
                AggregateReplacement(
                    type = "Vk250001",
                    oldNumber = "13",
                    newNumber = "16",
                    reason = tr(R.string.msg_service_life_6af9238),
                    date = now - 3600000
                )
            )
        )
    }
}

// Обёртка — содержимое истории без Dialog (для Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryPreviewContent(
    history: List<AggregateReplacement>
) {
    val dateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.ROOT)

    val typeOrder = listOf("Vr80", "Ta14", "Vk250001", "Vk250002")
    val typeNames = mapOf(
        "Vr80" to com.example.logbook.localization.I18n.display("ВР-80"),
        "Ta14" to com.example.logbook.localization.I18n.display("Та-14"),
        "Vk250001" to com.example.logbook.localization.I18n.display("ВК-2500-01"),
        "Vk250002" to com.example.logbook.localization.I18n.display("ВК-2500-02")
    )
    val grouped = typeOrder.associateWith { type ->
        history.filter { it.type == type }.sortedByDescending { it.date }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(tr(R.string.msg_replacement_history_2be0040)) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr(R.string.msg_back_f6dab07)
                        )
                    }
                }
            )

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tr(R.string.msg_history_is_empty_724b4cd))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    typeOrder.forEach { type ->
                        val list = grouped[type] ?: emptyList()
                        if (list.isNotEmpty()) {
                            item(key = "header_$type") {
                                Text(
                                    text = typeNames[type] ?: type,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        top = 16.dp,
                                        bottom = 8.dp
                                    )
                                )
                            }
                            items(
                                items = list,
                                key = { "${type}_${it.date}_${it.newNumber}" }
                            ) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${item.oldNumber} → ${item.newNumber}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.reason.isNotBlank()) {
                                        Text(
                                            text = tr(R.string.msg_reason_1_s_d2f849f, item.reason),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = dateFormat.format(Date(item.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}
