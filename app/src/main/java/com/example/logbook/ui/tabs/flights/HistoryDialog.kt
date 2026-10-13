package com.example.logbook.ui.tabs.flights

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.relation.DayWithLaunches
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.util.CompactTopBar

// ============================================================
// Диалог с ViewModel
// ============================================================

@Composable
fun HistoryDialog(
    profileId: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        key = "history_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val days by viewModel.days.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HistoryDialogContent(
            days = days,
            onDismiss = onDismiss
        )
    }
}

// ============================================================
// Содержимое — БЕЗ Dialog, чтобы работал Preview
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDialogContent(
    days: List<DayWithLaunches>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            CompactTopBar(
                title = tr(R.string.msg_history_d5fec74),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onDismiss
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(days.filter { it.launches.isNotEmpty() }, key = { _, it -> it.day.id }) { index, dayWithLaunches ->
                    val launches = dayWithLaunches.launches.sortedBy { it.order }
                    Column {
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                        DayBlock(
                            date = dayWithLaunches.day.date,
                            crew = dayWithLaunches.day.crew,
                            launches = launches
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ============================================================
// Блок дня
// ============================================================

@Composable
private fun DayBlock(
    date: String,
    crew: String,
    launches: List<LaunchEntity>
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatShortDate(date),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize.value.plus(2).sp),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (crew.isNotBlank()) {
                Text(
                    text = crew,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize.value.plus(2).sp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        launches.forEach { launch ->
            LaunchHistoryRow(launch)
        }
    }
}

@Composable
private fun LaunchHistoryRow(launch: LaunchEntity) {
    Column(modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)) {

        val topLine = formatLaunchTop(launch)
        if (topLine.isNotEmpty()) {
            Text(
                text = topLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize.value.plus(2).sp),
                fontFamily = FontFamily.Monospace
            )
        }

        if (launch.zapravkaLiters > 0) {
            Text(
                text = formatLaunchFuel(launch),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize.value.plus(2).sp),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

// ============================================================
// Форматирование
// ============================================================

private fun formatShortDate(date: String): String {
    val parts = date.split(".")
    if (parts.size == 3) {
        val year = parts[2]
        if (year.length == 4) {
            return "${parts[0]}.${parts[1]}.${year.substring(2)}"
        }
    }
    return date
}

private fun formatLaunchTop(launch: LaunchEntity): String {
    return when (launch.type) {
        "полет" -> tr(R.string.msg_flight_1_s_2_s_3_s_6d66a4f, launch.vsu, launch.zemlya, launch.vozduh)
        "ТП" -> tr(R.string.msg_td_1_s_2_s_3242f46, launch.vsu, launch.tp)
        "проба" -> tr(R.string.msg_test_1_s_3ac4e0f, launch.vsu)
        "запуск" -> tr(R.string.msg_test_1_s_3ac4e0f, launch.vsu)
        "дозаправка" -> ""
        else -> ""
    }
}

private fun formatLaunchFuel(launch: LaunchEntity): String {
    val liters = launch.zapravkaLiters
    val density = launch.zapravkaDensity
    val ved = launch.vedCek

    val densityStr = if (density == density.toLong().toDouble()) {
        density.toLong().toString()
    } else {
        density.toString().replace('.', ',')
    }

    return "$liters * $densityStr / $ved"
}

// ============================================================
// @Preview — БЕЗ private
// ============================================================

@Preview(
    name = "История — пустая",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
fun HistoryPreviewEmpty() {
    MaterialTheme {
        HistoryDialogContent(
            days = emptyList(),
            onDismiss = {}
        )
    }
}

@Preview(
    name = "История — несколько дней",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
fun HistoryPreviewFilled() {
    MaterialTheme {
        HistoryDialogContent(
            days = listOf(
                DayWithLaunches(
                    day = DayEntity(
                        id = 1, profileId = 1,
                        date = "02.09.2026",
                        crew = "Иванов И.И."
                    ),
                    launches = listOf(
                        LaunchEntity(
                            id = 1, dayId = 1, order = 0,
                            vsu = 5, zemlya = 12, vozduh = 46,
                            zapravkaLiters = 500, zapravkaDensity = 0.798,
                            zapravkaKg = 399.0, vedCek = "163"
                        )
                    )
                ),
                DayWithLaunches(
                    day = DayEntity(
                        id = 2, profileId = 1,
                        date = "03.09.2026",
                        crew = "Петров П.П."
                    ),
                    launches = listOf(
                        LaunchEntity(
                            id = 2, dayId = 2, order = 0,
                            vsu = 4, zemlya = 11, vozduh = 100
                        ),
                        LaunchEntity(
                            id = 3, dayId = 2, order = 1,
                            vsu = 3, zemlya = 9, vozduh = 38
                        )
                    )
                ),
                DayWithLaunches(
                    day = DayEntity(
                        id = 3, profileId = 1,
                        date = "06.09.2026"
                    ),
                    launches = listOf(
                        LaunchEntity(
                            id = 4, dayId = 3, order = 0,
                            vsu = 5, zemlya = 0, vozduh = 0
                        )
                    )
                ),
                DayWithLaunches(
                    day = DayEntity(
                        id = 4, profileId = 1,
                        date = "07.09.2026",
                        crew = "Сидоров С.С."
                    ),
                    launches = listOf(
                        LaunchEntity(
                            id = 5, dayId = 4, order = 0,
                            vsu = 0, zemlya = 0, vozduh = 0,
                            zapravkaLiters = 1000, zapravkaDensity = 0.789,
                            zapravkaKg = 789.0, vedCek = "42"
                        )
                    )
                )
            ),
            onDismiss = {}
        )
    }
}
