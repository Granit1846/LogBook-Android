package com.example.logbook.ui.tabs.flights

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.res.Configuration
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.SnkEntity
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.util.PullToSyncContainer
import com.example.logbook.ui.util.selectAllOnFocus
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ============================================================
// Главный экран (с ViewModel) — standalone
// ============================================================

@Composable
fun FlightsTab(profileId: Long) {
    val context = LocalContext.current
    var showSync by remember { mutableStateOf(false) }
    var autoSyncActive by remember { mutableStateOf(false) }
    val viewModel: FlightsViewModel = viewModel(
        key = "flights_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val triggerAutoSync by viewModel.triggerAutoSync.collectAsStateWithLifecycle()
    LaunchedEffect(triggerAutoSync) {
        if (triggerAutoSync) {
            viewModel.consumeAutoSync()
            autoSyncActive = true
            showSync = true
        }
    }

    FlightsTabWithViewModel(
        profileId = profileId,
        viewModel = viewModel,
        showToolbar = true,
        onSync = {
            autoSyncActive = true
            showSync = true
        }
    )
    if (showSync) com.example.logbook.ui.settings.FlightsSyncDialog(
        onDismiss = {
            showSync = false
            autoSyncActive = false
        },
        onDownloaded = {
            showSync = false
            autoSyncActive = false
        },
        autoStart = autoSyncActive,
        silent = autoSyncActive
    )
}

// ============================================================
// Экран с ViewModel извне (MainScreen)
// ============================================================

@Composable
fun FlightsTabWithViewModel(
    profileId: Long,
    viewModel: FlightsViewModel,
    showToolbar: Boolean,
    onSync: () -> Unit = {}
) {
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val loadedPage by viewModel.loadedPage.collectAsStateWithLifecycle()
    val finishDayErrors by viewModel.finishDayErrors.collectAsStateWithLifecycle()
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()

    FlightsTabContent(
        profileId = profileId,
        date = loadedPage?.date ?: date,
        launches = loadedPage?.details?.launches?.sortedBy { it.order }.orEmpty(),
        snk = loadedPage?.details?.snk,
        currentDay = loadedPage?.details?.day,
        showToolbar = showToolbar,
        finishDayErrors = finishDayErrors,
        isFinishing = isFinishing,
        onShiftDay = { viewModel.shiftDay(it) },
        onSelectDate = { viewModel.setDate(it) },
        onUpdateCrew = { viewModel.updateCrew(it) },
        onAddLaunch = { viewModel.addLaunch() },
        onUpdateLaunch = { viewModel.updateLaunch(it) },
        onDeleteLaunch = { viewModel.deleteLaunch(it) },
        onUpdateSnk = { viewModel.updateSnk(it) },
        onFinishDay = { viewModel.finishDay() },
        onDismissErrors = { viewModel.dismissFinishDayErrors() },
        interactionDate = date,
        onSync = onSync
    )
}

// ============================================================
// Содержимое — без ViewModel
// ============================================================

private data class FlightsPage(
    val date: String,
    val crew: String,
    val launches: List<LaunchEntity>,
    val snk: SnkEntity?
)

@Composable
fun FlightsTabContent(
    profileId: Long,
    date: String,
    launches: List<LaunchEntity>,
    snk: SnkEntity?,
    currentDay: DayEntity?,
    showToolbar: Boolean,
    finishDayErrors: List<DayWriter.LaunchError> = emptyList(),
    isFinishing: Boolean = false,
    onShiftDay: (Int) -> Unit,
    onUpdateCrew: (String) -> Unit,
    onAddLaunch: () -> Unit,
    onUpdateLaunch: (LaunchEntity) -> Unit,
    onDeleteLaunch: (LaunchEntity) -> Unit,
    onUpdateSnk: (SnkEntity) -> Unit,
    onFinishDay: () -> Unit = {},
    onDismissErrors: () -> Unit = {},
    onSelectDate: (String) -> Unit = {},
    interactionDate: String = date,
    onSync: () -> Unit = {}
) {
    val shiftDay by rememberUpdatedState(onShiftDay)
    val activeDate by rememberUpdatedState(interactionDate)
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    val pageDay = currentDay?.takeIf { it.date == date }
    val page = FlightsPage(
        date = date,
        crew = pageDay?.crew.orEmpty(),
        launches = launches.filter { it.dayId == pageDay?.id },
        snk = snk?.takeIf { it.dayId == pageDay?.id }
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            var showHistory by remember { mutableStateOf(false) }

            if (showToolbar) {
                FlightsToolbar(
                    onFinishDay = onFinishDay,
                    onHistory = { showHistory = true },
                    onSync = onSync,
                    finishDayEnabled = !isFinishing
                )
                HorizontalDivider()
            }

            if (showHistory) {
                HistoryDialog(
                    profileId = profileId,
                    onDismiss = { showHistory = false }
                )
            }

            AnimatedContent(
                targetState = page,
                contentKey = { it.date },
                transitionSpec = {
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    val forward = runCatching {
                        LocalDate.parse(targetState.date, formatter) >
                            LocalDate.parse(initialState.date, formatter)
                    }.getOrDefault(true)
                    val direction = if (forward) AnimatedContentTransitionScope.SlideDirection.Left
                        else AnimatedContentTransitionScope.SlideDirection.Right
                    slideIntoContainer(direction, tween(280)) togetherWith
                        slideOutOfContainer(direction, tween(280))
                },
                label = "FlightsDayTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(swipeThreshold) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onDragCancel = { horizontalDrag = 0f },
                            onDragEnd = {
                                if (horizontalDrag <= -swipeThreshold) shiftDay(1)
                                else if (horizontalDrag >= swipeThreshold) shiftDay(-1)
                                horizontalDrag = 0f
                            },
                            onHorizontalDrag = { _, amount -> horizontalDrag += amount }
                        )
                    }
            ) { displayedPage ->
                val isLandscape = LocalConfiguration.current.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE
                PullToSyncContainer(
                    onSync = onSync
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "date_crew", contentType = "header") {

                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DateHeaderInline(
                                    date = displayedPage.date,
                                    onShift = { if (displayedPage.date == activeDate) shiftDay(it) },
                                    onSelectDate = { if (displayedPage.date == activeDate) onSelectDate(it) }
                                )
                                CrewFieldInline(
                                    crew = displayedPage.crew,
                                    onCrewChange = { if (displayedPage.date == activeDate) onUpdateCrew(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            DateHeader(
                                date = displayedPage.date,
                                onShift = { if (displayedPage.date == activeDate) shiftDay(it) },
                                onSelectDate = { if (displayedPage.date == activeDate) onSelectDate(it) }
                            )
                            CrewField(
                                crew = displayedPage.crew,
                                onCrewChange = { if (displayedPage.date == activeDate) onUpdateCrew(it) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    }

                    if (displayedPage.launches.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tr(R.string.msg_no_starts_tap_to_add_one_00f2fa0),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        items(displayedPage.launches, key = { it.id }, contentType = { "launch" }) { launch ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                LaunchCard(
                                    launch = launch,
                                    onUpdate = { if (displayedPage.date == activeDate) onUpdateLaunch(it) },
                                    onDelete = { if (displayedPage.date == activeDate) onDeleteLaunch(launch) }
                                )
                            }
                        }
                    }

                    item(key = "snk_totals", contentType = "summary") {
                        Spacer(modifier = Modifier.height(16.dp))

                        // ============================================================
                        // СНК + Итого за день
                        //   portrait:  СНК сверху, Итого снизу
                        //   landscape: СНК (2/3) слева, Итого (1/3) справа
                        // ============================================================
                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(2f)) {
                                    SnkSection(
                                        snk = displayedPage.snk,
                                        onUpdate = { if (displayedPage.date == activeDate) onUpdateSnk(it) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    DailyTotals(launches = displayedPage.launches)
                                }
                            }
                        } else {
                            SnkSection(
                                snk = displayedPage.snk,
                                onUpdate = { if (displayedPage.date == activeDate) onUpdateSnk(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DailyTotals(launches = displayedPage.launches)
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
        }

        OutlinedFloatingActionButton(
            onClick = onAddLaunch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = tr(R.string.msg_add_start_7fef594))
        }
    }

    if (finishDayErrors.isNotEmpty()) {
        FinishDayErrorsDialog(
            errors = finishDayErrors,
            onDismiss = onDismissErrors
        )
    }
}

// ============================================================
// Диалог ошибок после «Закончить день»
// ============================================================

@Composable
internal fun FinishDayErrorsDialog(
    errors: List<DayWriter.LaunchError>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.msg_errors_while_closing_the_day_bd86550)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr(R.string.msg_invalid_data_combinations_were_found_024c919) +
                            tr(R.string.msg_entries_were_saved_anyway_turbine_drive_mode_t_7ad7486),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                errors.forEach { err ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = tr(R.string.msg_start_1_s_661f34d, err.launchOrder + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = err.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr(R.string.msg_got_it_f127f2a)) }
        }
    )
}

// ============================================================
// Верхняя панель (portrait)
// ============================================================

@Composable
private fun FlightsToolbar(
    onFinishDay: () -> Unit,
    onHistory: () -> Unit,
    onSync: () -> Unit,
    finishDayEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onFinishDay,
            enabled = finishDayEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Text(tr(R.string.msg_finish_day_0db486a))
        }
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier.weight(1f)
        ) {
            Text(tr(R.string.msg_history_d5fec74), fontSize = (MaterialTheme.typography.labelLarge.fontSize.value + 2).sp)
        }
        IconButton(onClick = onSync) {
            Icon(Icons.Default.Sync, contentDescription = tr(R.string.msg_synchronization_7ceddda))
        }
    }
}

// ============================================================
// Действия для TopAppBar (landscape)
// ============================================================

@Composable
fun FlightsTopBarActions(
    onFinishDay: () -> Unit,
    onHistory: () -> Unit,
    onSync: () -> Unit,
    finishDayEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = onFinishDay,
            enabled = finishDayEnabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(tr(R.string.msg_finish_day_0db486a), fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onHistory,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(tr(R.string.msg_history_d5fec74), fontSize = 15.sp)
        }
        IconButton(onClick = onSync) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = tr(R.string.msg_synchronization_7ceddda)
            )
        }
    }
}

// ============================================================
// Дата / Экипаж
// ============================================================

@Composable
private fun DateHeader(
    date: String,
    onShift: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    if (showPicker) {
        ScrollableDatePickerDialog(initialDate = runCatching { LocalDate.parse(date, formatter) }.getOrDefault(LocalDate.now()),
            onSelect = { onSelectDate(it.format(formatter)); showPicker = false },
            onDismiss = { showPicker = false })
    }
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { onShift(-1) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr(R.string.msg_previous_day_0e26e83))
        }
        OutlinedButton(
            onClick = { showPicker = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(date, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        IconButton(onClick = { onShift(+1) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr(R.string.msg_next_day_9ee7544))
        }
    }
}

@Composable
private fun DateHeaderInline(
    date: String,
    onShift: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DateHeader(date = date, onShift = onShift, onSelectDate = onSelectDate, modifier = modifier)
}

@Composable
private fun CrewField(
    crew: String,
    onCrewChange: (String) -> Unit
) {
    var localText by remember { mutableStateOf(crew) }

    LaunchedEffect(crew) {
        if (crew != localText) {
            localText = crew
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = localText,
            onValueChange = { text ->
                localText = text
                onCrewChange(text)
            },
            label = { Text(tr(R.string.msg_crew_ce392cd)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CrewFieldInline(
    crew: String,
    onCrewChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localText by remember { mutableStateOf(crew) }

    LaunchedEffect(crew) {
        if (crew != localText) {
            localText = crew
        }
    }

    OutlinedTextField(
        value = localText,
        onValueChange = { text ->
            localText = text
            onCrewChange(text)
        },
        label = { Text(tr(R.string.msg_crew_ce392cd)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = modifier
    )
}

// ============================================================
// LaunchCard
// ============================================================

@Composable
private fun LaunchCard(
    launch: LaunchEntity,
    onUpdate: (LaunchEntity) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember(launch.id) {
        mutableStateOf(launch.tp > 0 || launch.samolet > 0)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.example.logbook.localization.I18n.display(launch.type).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.height(CompactRowHeight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = tr(R.string.msg_delete_start_0d6f6a0))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactIntField(
                    placeholder = tr(R.string.msg_apu_60c5faf),
                    value = launch.vsu,
                    onValueChange = { onUpdate(launch.copy(vsu = it)) },
                    modifier = Modifier.weight(1f)
                )
                CompactIntField(
                    placeholder = tr(R.string.msg_ground_42ff10b),
                    value = launch.zemlya,
                    onValueChange = { onUpdate(launch.copy(zemlya = it)) },
                    modifier = Modifier.weight(1f)
                )
                CompactIntField(
                    placeholder = tr(R.string.msg_flight_c8478a6),
                    value = launch.vozduh,
                    onValueChange = { onUpdate(launch.copy(vozduh = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ZapravkaRow(launch = launch, onUpdate = onUpdate)

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tr(R.string.msg_advanced_d7907f4),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactIntField(
                        placeholder = "ТП",
                        value = launch.tp,
                        onValueChange = { onUpdate(launch.copy(tp = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    CompactIntField(
                        placeholder = tr(R.string.msg_aeropl_d3cb65b),
                        value = launch.samolet,
                        onValueChange = { onUpdate(launch.copy(samolet = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZapravkaRow(
    launch: LaunchEntity,
    onUpdate: (LaunchEntity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactIntField(
            placeholder = tr(R.string.msg_litres_e5f290d),
            value = launch.zapravkaLiters,
            onValueChange = { onUpdate(launch.copy(zapravkaLiters = it)) },
            modifier = Modifier.weight(1f)
        )
        CompactDoubleField(
            placeholder = tr(R.string.msg_density_df6469a),
            value = launch.zapravkaDensity,
            onValueChange = { onUpdate(launch.copy(zapravkaDensity = it)) },
            modifier = Modifier.weight(1f)
        )
        CompactReadOnlyDoubleField(
            placeholder = tr(R.string.msg_kg_6f0e40b),
            value = launch.calculatedKg,
            modifier = Modifier.weight(1f)
        )
        CompactTextField(
            placeholder = tr(R.string.msg_keeper_d531d06),
            value = launch.vedCek,
            onValueChange = { onUpdate(launch.copy(vedCek = it)) },
            modifier = Modifier.weight(1.2f)
        )
    }
}

// ============================================================
// СНК
// ============================================================

@Composable
private fun SnkSection(
    snk: SnkEntity?,
    onUpdate: (SnkEntity) -> Unit
) {
    val current = snk ?: SnkEntity(dayId = 0)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = tr(R.string.msg_snk_4087caf),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnkHalf(
                title = tr(R.string.msg_left_28c0929),
                modifier = Modifier.weight(1f),
                a = current.aLeft,
                b = current.bLeft,
                e = current.eLeft,
                g = current.gLeft,
                h = current.hLeft,
                i = current.iLeft,
                onA = { onUpdate(current.copy(aLeft = it)) },
                onB = { onUpdate(current.copy(bLeft = it)) },
                onE = { onUpdate(current.copy(eLeft = it)) },
                onG = { onUpdate(current.copy(gLeft = it)) },
                onH = { onUpdate(current.copy(hLeft = it)) },
                onI = { onUpdate(current.copy(iLeft = it)) }
            )
            SnkHalf(
                title = tr(R.string.msg_right_49dd3c2),
                modifier = Modifier.weight(1f),
                a = current.aRight,
                b = current.bRight,
                e = current.eRight,
                g = current.gRight,
                h = current.hRight,
                i = current.iRight,
                onA = { onUpdate(current.copy(aRight = it)) },
                onB = { onUpdate(current.copy(bRight = it)) },
                onE = { onUpdate(current.copy(eRight = it)) },
                onG = { onUpdate(current.copy(gRight = it)) },
                onH = { onUpdate(current.copy(hRight = it)) },
                onI = { onUpdate(current.copy(iRight = it)) }
            )
        }
    }
}

@Composable
private fun SnkHalf(
    title: String,
    modifier: Modifier = Modifier,
    a: Double, b: Double, e: Double, g: Double, h: Double, i: Double,
    onA: (Double) -> Unit,
    onB: (Double) -> Unit,
    onE: (Double) -> Unit,
    onG: (Double) -> Unit,
    onH: (Double) -> Unit,
    onI: (Double) -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        HorizontalDivider()
        SnkDoubleRow("A", a, onA)
        SnkDoubleRow("B", b, onB)
        SnkDoubleRow("E", e, onE)
        SnkDoubleRow("G", g, onG)
        SnkDoubleRow("H", h, onH)
        SnkDoubleRow("I", i, onI)
    }
}

@Composable
private fun SnkDoubleRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    var tfv by remember(value) {
        val s = formatDouble(value)
        mutableStateOf(TextFieldValue(s, TextRange(s.length)))
    }

    LaunchedEffect(value) {
        val s = formatDouble(value)
        if (s != tfv.text) {
            tfv = TextFieldValue(s, TextRange(s.length))
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(20.dp),
            fontWeight = FontWeight.Bold
        )
        BasicTextField(
            value = tfv,
            onValueChange = { newValue ->
                var normalized = newValue.text.replace('.', ',')
                normalized = normalized.filter { it.isDigit() || it == ',' }
                tfv = TextFieldValue(
                    normalized,
                    selection = TextRange(normalized.length)
                )
                val parsed = normalized.replace(',', '.').toDoubleOrNull() ?: 0.0
                onValueChange(parsed)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier
                .weight(1f)
                .height(CompactRowHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CompactShape
                )
                .padding(
                    horizontal = CompactHorizontalPadding,
                    vertical = CompactVerticalPadding
                )
                .selectAllOnFocus(tfv) { tfv = it },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (tfv.text.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ============================================================
// Компактные поля
// ============================================================

private val CompactRowHeight = 38.dp
private val CompactShape = RoundedCornerShape(6.dp)
private val CompactHorizontalPadding = 6.dp
private val CompactVerticalPadding = 2.dp

@Composable
private fun CompactTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    var tfv by remember(value) {
        mutableStateOf(
            TextFieldValue(value, TextRange(value.length))
        )
    }

    LaunchedEffect(value) {
        if (value != tfv.text) {
            tfv = TextFieldValue(value, TextRange(value.length))
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = newValue.copy(selection = newValue.selection)
            onValueChange(newValue.text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = modifier
            .height(CompactRowHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CompactShape
            )
            .padding(
                horizontal = CompactHorizontalPadding,
                vertical = CompactVerticalPadding
            )
            .selectAllOnFocus(tfv) { tfv = it },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (tfv.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CompactIntField(
    placeholder: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    CompactTextField(
        placeholder = placeholder,
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val digits = text.filter { it.isDigit() }
            onValueChange(digits.toIntOrNull() ?: 0)
        },
        modifier = modifier,
        keyboardType = KeyboardType.Number,
        imeAction = imeAction
    )
}

@Composable
private fun CompactDoubleField(
    placeholder: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    var tfv by remember(value) {
        val s = formatDouble(value)
        mutableStateOf(TextFieldValue(s, TextRange(s.length)))
    }

    LaunchedEffect(value) {
        val s = formatDouble(value)
        if (s != tfv.text) {
            tfv = TextFieldValue(s, TextRange(s.length))
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            var normalized = newValue.text.replace('.', ',')
            normalized = normalized.filter { it.isDigit() || it == ',' }
            tfv = TextFieldValue(
                normalized,
                selection = TextRange(normalized.length)
            )
            val parsed = normalized.replace(',', '.').toDoubleOrNull() ?: 0.0
            onValueChange(parsed)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier
            .height(CompactRowHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CompactShape
            )
            .padding(
                horizontal = CompactHorizontalPadding,
                vertical = CompactVerticalPadding
            )
            .selectAllOnFocus(tfv) { tfv = it },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (tfv.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CompactReadOnlyDoubleField(
    placeholder: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(CompactRowHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CompactShape
            )
            .padding(
                horizontal = CompactHorizontalPadding,
                vertical = CompactVerticalPadding
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value == 0.0) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        } else {
            Text(
                text = formatDouble(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// ============================================================
// Итоги за день
// ============================================================

@Composable
private fun DailyTotals(launches: List<LaunchEntity>) {
    val totalVsu = launches.sumOf { it.vsu }
    val totalZemlya = launches.sumOf { it.zemlya }
    val totalVozduh = launches.sumOf { it.vozduh }
    val totalLiters = launches.sumOf { it.zapravkaLiters }
    val totalKg = launches.sumOf { it.calculatedKg }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tr(R.string.msg_daily_total_66589f6),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TotalRow(tr(R.string.msg_apu_60c5faf), tr(R.string.msg_1_s_min_2_s_b909d28, totalVsu, formatMinutes(totalVsu)))
            TotalRow(tr(R.string.msg_ground_42ff10b), tr(R.string.msg_1_s_min_2_s_937f92b, totalZemlya, formatMinutes(totalZemlya)))
            TotalRow(tr(R.string.msg_flight_c8478a6), tr(R.string.msg_1_s_min_2_s_e5b5fc2, totalVozduh, formatMinutes(totalVozduh)))
            TotalRow(tr(R.string.msg_refuelling_b773ac0), tr(R.string.msg_1_s_l_2_s_kg_c28a115, totalLiters, formatKg(totalKg)))
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================
// Утилиты
// ============================================================

private fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return "%02d:%02d".format(h, m)
}

private fun formatDouble(value: Double): String {
    if (value == 0.0) return ""
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString().replace('.', ',')
    }
}

private fun formatKg(value: Double): String {
    if (value == 0.0) return ""
    return value.roundToInt().toString()
}

// ============================================================
// @Preview — БЕЗ ИЗМЕНЕНИЙ
// ============================================================

@Preview(
    name = "Полёты — пустой день (portrait)",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun FlightsTabPreviewEmpty() {
    MaterialTheme {
        FlightsTabContent(
            profileId = 1L,
            date = "05.10.2026",
            launches = emptyList(),
            snk = SnkEntity(id = 1, dayId = 1),
            currentDay = DayEntity(id = 1, profileId = 1, date = "05.10.2026", crew = ""),
            showToolbar = true,
            onShiftDay = {},
            onUpdateCrew = {},
            onAddLaunch = {},
            onUpdateLaunch = {},
            onDeleteLaunch = {},
            onUpdateSnk = {}
        )
    }
}

@Preview(
    name = "Полёты — 2 запуска (portrait)",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun FlightsTabPreviewFilled() {
    MaterialTheme {
        FlightsTabContent(
            profileId = 1L,
            date = "05.10.2026",
            launches = listOf(
                LaunchEntity(
                    id = 1, dayId = 1, order = 0,
                    vsu = 5, zemlya = 12, vozduh = 46,
                    zapravkaLiters = 500, zapravkaDensity = 0.798,
                    zapravkaKg = 399.0, vedCek = "123"
                ),
                LaunchEntity(
                    id = 2, dayId = 1, order = 1,
                    vsu = 5, zemlya = 0, vozduh = 0
                )
            ),
            snk = SnkEntity(
                id = 1, dayId = 1,
                aLeft = 1.5, aRight = 1.5,
                bLeft = 0.2, bRight = 0.2,
                eLeft = 0.5, eRight = 0.5,
                gLeft = 0.15, gRight = 0.15,
                hLeft = 0.3, hRight = 0.3,
                iLeft = 5.0, iRight = 5.0
            ),
            currentDay = DayEntity(
                id = 1, profileId = 1,
                date = "05.10.2026", crew = "Иванов И.И."
            ),
            showToolbar = true,
            onShiftDay = {},
            onUpdateCrew = {},
            onAddLaunch = {},
            onUpdateLaunch = {},
            onDeleteLaunch = {},
            onUpdateSnk = {}
        )
    }
}

@Preview(
    name = "Полёты — landscape (без toolbar)",
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape"
)
@Composable
private fun FlightsTabPreviewLandscape() {
    MaterialTheme {
        FlightsTabContent(
            profileId = 1L,
            date = "05.10.2026",
            launches = listOf(
                LaunchEntity(id = 1, dayId = 1, order = 0, vsu = 5, zemlya = 12, vozduh = 46)
            ),
            snk = SnkEntity(id = 1, dayId = 1),
            currentDay = DayEntity(id = 1, profileId = 1, date = "05.10.2026", crew = "Иванов И.И."),
            showToolbar = false,
            onShiftDay = {},
            onUpdateCrew = {},
            onAddLaunch = {},
            onUpdateLaunch = {},
            onDeleteLaunch = {},
            onUpdateSnk = {}
        )
    }
}
