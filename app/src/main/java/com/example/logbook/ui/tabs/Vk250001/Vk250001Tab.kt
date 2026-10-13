package com.example.logbook.ui.tabs.vk250001

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.logbook.ui.theme.LocalFormularColors
import com.example.logbook.ui.theme.formularColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.Vk250001Entity
import com.example.logbook.data.local.entity.Vk250001StartEntity
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.util.LockLandscape
import com.example.logbook.ui.util.bringIntoViewOnFocus
import com.example.logbook.ui.util.ZoomableTable
import com.example.logbook.ui.util.FormularRow
import com.example.logbook.ui.util.DeleteDayCell
import com.example.logbook.ui.util.formularCellBounds
import com.example.logbook.ui.util.displayDate
import com.example.logbook.ui.util.formatDateInput
import com.example.logbook.ui.util.formatTimeInput
import com.example.logbook.ui.util.minutesToTime
import com.example.logbook.ui.util.parseDateInput
import com.example.logbook.ui.util.parseTimeInput
import java.math.BigDecimal
import java.math.RoundingMode

// ============================================================
// Основной экран (с ViewModel)
// ============================================================

@Composable
fun Vk250001Tab(profileId: Long) {
    LockLandscape()

    val context = LocalContext.current
    val viewModel: Vk250001ViewModel = viewModel(
        key = "vk250001_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val records by viewModel.records.collectAsStateWithLifecycle()
    val startRecord by viewModel.startRecord.collectAsStateWithLifecycle()

    LaunchedEffect(startRecord) {
        if (startRecord == null) viewModel.ensureStartExists()
    }

    Vk250001TabContent(
        startRecord = startRecord,
        records = records,
        onUpdateStart = { viewModel.updateStart(it) },
        onDeleteDay = { viewModel.deleteDay(it) }
    )
}

// ============================================================
// Содержимое (без ViewModel) — используется и в Preview
// ============================================================

@Composable
private fun Vk250001TabContent(
    startRecord: Vk250001StartEntity?,
    records: List<Vk250001Entity>,
    onUpdateStart: (Vk250001StartEntity) -> Unit,
    onDeleteDay: suspend (String) -> Unit = {}
) {
    CompositionLocalProvider(LocalFormularColors provides formularColors()) {
        if (startRecord == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@CompositionLocalProvider
        }

        var showCoeffDialog by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            ZoomableTable(minScale = 0.5f, maxScale = 3f) {
                Column(modifier = Modifier.width(Vk250001Cols.TOTAL_WIDTH)) {
                    HeaderAbsolute()
                    StartRow(start = startRecord, onUpdate = onUpdateStart)
                    records.forEach { rec -> DataRow(record = rec, onDeleteDay = onDeleteDay) }
                    Box(modifier = Modifier.height(80.dp))
                }
            }

            // Кнопка коэффициентов
            OutlinedFloatingActionButton(
                onClick = { showCoeffDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Icon(
                    Icons.Default.Settings,
                    contentDescription = tr(R.string.msg_coefficients_486752c)
                )
            }
        }

        if (showCoeffDialog) {
            CoeffDialog(
                coeffA = startRecord.coeffA,
                coeffH = startRecord.coeffH,
                onSave = { a, h ->
                    onUpdateStart(startRecord.copy(coeffA = a, coeffH = h))
                    showCoeffDialog = false
                },
                onDismiss = { showCoeffDialog = false }
            )
        }
    }
}

// ============================================================
// Диалог коэффициентов
// ============================================================

@Composable
private fun CoeffDialog(
    coeffA: Double,
    coeffH: Double,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var aText by remember { mutableStateOf(formatDouble(coeffA)) }
    var hText by remember { mutableStateOf(formatDouble(coeffH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.msg_vk_2500_01_coefficients_4fe531e)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(R.string.msg_coefficient_a_23d866f), style = MaterialTheme.typography.labelMedium)
                BasicTextField(
                    value = aText,
                    onValueChange = { text ->
                        var normalized = text.replace('.', ',')
                        normalized = normalized.filter { it.isDigit() || it == ',' }
                        aText = normalized
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = LocalFormularColors.current.cellText),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.bringIntoViewOnFocus()
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp)
                )

                Text(tr(R.string.msg_coefficient_h_f5c50d4), style = MaterialTheme.typography.labelMedium)
                BasicTextField(
                    value = hText,
                    onValueChange = { text ->
                        var normalized = text.replace('.', ',')
                        normalized = normalized.filter { it.isDigit() || it == ',' }
                        hText = normalized
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = LocalFormularColors.current.cellText),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.bringIntoViewOnFocus()
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = aText.replace(',', '.').toDoubleOrNull() ?: 0.0
                val h = hText.replace(',', '.').toDoubleOrNull() ?: 0.0
                onSave(a, h)
            }) { Text(tr(R.string.msg_save_4864057)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr(R.string.msg_cancel_0ec753b)) }
        }
    )
}

// ============================================================
// Шапка
// ============================================================

@Composable
private fun HeaderAbsolute() {
    Box(
        modifier = Modifier
            .width(Vk250001Cols.TOTAL_WIDTH)
            .height(Vk250001Cols.HEADER_HEIGHT)
    ) {
        val y1 = 0.dp
        val y2 = Vk250001Cols.H_LEVEL1
        val y3 = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2
        val y4 = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3
        val fullH = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3
        val h23 = Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3

        // Rowspan 3: Col0 (Дата), Col1 (Запуск), Col19 (Подпись), Col20 (✕)
        HCellV(Vk250001Cols.XS[0], y1, Vk250001Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Vk250001Cols.XS[1], y1, Vk250001Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Vk250001Cols.XS[19], y1, Vk250001Cols.WIDTHS[19], fullH, tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(Vk250001Cols.XS[20], y1, Vk250001Cols.WIDTHS[20], fullH, "")

        // У1: Col2-18 — общая группа "Наработка двигателя, ч:мин"
        val wEngine = Vk250001Cols.WIDTHS[2] + Vk250001Cols.WIDTHS[3] + Vk250001Cols.WIDTHS[4] +
                Vk250001Cols.WIDTHS[5] + Vk250001Cols.WIDTHS[6] + Vk250001Cols.WIDTHS[7] +
                Vk250001Cols.WIDTHS[8] + Vk250001Cols.WIDTHS[9] + Vk250001Cols.WIDTHS[10] +
                Vk250001Cols.WIDTHS[11] + Vk250001Cols.WIDTHS[12] + Vk250001Cols.WIDTHS[13] +
                Vk250001Cols.WIDTHS[14] + Vk250001Cols.WIDTHS[15] + Vk250001Cols.WIDTHS[16] +
                Vk250001Cols.WIDTHS[17] + Vk250001Cols.WIDTHS[18]
        HCellH(Vk250001Cols.XS[2], y1, wEngine, Vk250001Cols.H_LEVEL1,
            tr(R.string.msg_engine_operating_time_h_min_d10ebec))

        // У2: Col2-3 — "Эквивалентная наработка (G)"
        HCellH(Vk250001Cols.XS[2], y2, Vk250001Cols.WIDTHS[2] + Vk250001Cols.WIDTHS[3],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_equivalent_operating_time_g_5cf9acc))

        // У2: Col4-6 — "Суммарная (общая) наработка (A)"
        HCellH(Vk250001Cols.XS[4], y2,
            Vk250001Cols.WIDTHS[4] + Vk250001Cols.WIDTHS[5] + Vk250001Cols.WIDTHS[6],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_total_operating_time_a_5345b4f))

        // У2: Col7-8 — "Наработка на взлётном режиме (B)"
        HCellH(Vk250001Cols.XS[7], y2, Vk250001Cols.WIDTHS[7] + Vk250001Cols.WIDTHS[8],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_takeoff_operating_time_b_e171014))

        // У2: Col9-11 — "Наработка на максимально продолжительном режиме"
        HCellH(Vk250001Cols.XS[9], y2,
            Vk250001Cols.WIDTHS[9] + Vk250001Cols.WIDTHS[10] + Vk250001Cols.WIDTHS[11],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_maximum_continuous_mode_operating_time_9a36288))

        // У2: Col12-13 — "Наработка при повышенной температуре газов (D)"
        HCellH(Vk250001Cols.XS[12], y2, Vk250001Cols.WIDTHS[12] + Vk250001Cols.WIDTHS[13],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_high_exhaust_gas_temperature_operating_time_d_0c852d0))

        // У2: Col14-15 — "Наработка при отключенном БАРК (E)"
        HCellH(Vk250001Cols.XS[14], y2, Vk250001Cols.WIDTHS[14] + Vk250001Cols.WIDTHS[15],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_operating_time_with_bark_disabled_e_b57a145))

        // У2: Col16-18 — "Циклическая наработка (H)"
        HCellH(Vk250001Cols.XS[16], y2,
            Vk250001Cols.WIDTHS[16] + Vk250001Cols.WIDTHS[17] + Vk250001Cols.WIDTHS[18],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_cycle_based_operating_time_h_e4ab175))

        // У3: подписи
        // Col2 — пусто
        // Col3 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[3], y3, Vk250001Cols.WIDTHS[3], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col4 — пусто
        // Col5 — "С начала эксплуатации"
        HCellH(Vk250001Cols.XS[5], y3, Vk250001Cols.WIDTHS[5], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col6 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250001Cols.XS[6], y3, Vk250001Cols.WIDTHS[6], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))
        // Col7 — "От 6 до 15 мин"
        HCellH(Vk250001Cols.XS[7], y3, Vk250001Cols.WIDTHS[7], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_6_to_15_min_24673b0))
        // Col8 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[8], y3, Vk250001Cols.WIDTHS[8], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col9 — "Общая" (вертикально)
        HCellV(Vk250001Cols.XS[9], y3, Vk250001Cols.WIDTHS[9], Vk250001Cols.H_LEVEL3, tr(R.string.msg_overall_d7672c5))
        // Col10 — "На земле" (вертикально)
        HCellV(Vk250001Cols.XS[10], y3, Vk250001Cols.WIDTHS[10], Vk250001Cols.H_LEVEL3, tr(R.string.msg_on_ground_267312c))
        // Col11 — "В полёте" (вертикально)
        HCellV(Vk250001Cols.XS[11], y3, Vk250001Cols.WIDTHS[11], Vk250001Cols.H_LEVEL3, tr(R.string.msg_in_flight_2b66de6))
        // Col12 — пусто
        // Col13 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[13], y3, Vk250001Cols.WIDTHS[13], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col14 — пусто
        // Col15 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[15], y3, Vk250001Cols.WIDTHS[15], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col16 — пусто
        // Col17 — "С начала эксплуатации"
        HCellH(Vk250001Cols.XS[17], y3, Vk250001Cols.WIDTHS[17], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col18 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250001Cols.XS[18], y3, Vk250001Cols.WIDTHS[18], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))

        // Белая строка (уровень 4)
        HCellH(Vk250001Cols.XS[0], y4, Vk250001Cols.WIDTHS[0], Vk250001Cols.H_LEVEL4, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Vk250001Cols.XS[1], y4, Vk250001Cols.WIDTHS[1], Vk250001Cols.H_LEVEL4, tr(R.string.msg_starts_c001fc7), white = true)
        HCellH(Vk250001Cols.XS[2], y4, Vk250001Cols.WIDTHS[2], Vk250001Cols.H_LEVEL4, "3", white = true)
        HCellH(Vk250001Cols.XS[3], y4, Vk250001Cols.WIDTHS[3], Vk250001Cols.H_LEVEL4, "G", white = true)
        HCellH(Vk250001Cols.XS[4], y4, Vk250001Cols.WIDTHS[4], Vk250001Cols.H_LEVEL4, "5", white = true)
        HCellH(Vk250001Cols.XS[5], y4, Vk250001Cols.WIDTHS[5], Vk250001Cols.H_LEVEL4, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Vk250001Cols.XS[6], y4, Vk250001Cols.WIDTHS[6], Vk250001Cols.H_LEVEL4, tr(R.string.msg_so_a921cdf), white = true)
        HCellH(Vk250001Cols.XS[7], y4, Vk250001Cols.WIDTHS[7], Vk250001Cols.H_LEVEL4, "8", white = true)
        HCellH(Vk250001Cols.XS[8], y4, Vk250001Cols.WIDTHS[8], Vk250001Cols.H_LEVEL4, "B", white = true)
        HCellH(Vk250001Cols.XS[9], y4, Vk250001Cols.WIDTHS[9], Vk250001Cols.H_LEVEL4, "10", white = true)
        HCellH(Vk250001Cols.XS[10], y4, Vk250001Cols.WIDTHS[10], Vk250001Cols.H_LEVEL4, "11", white = true)
        HCellH(Vk250001Cols.XS[11], y4, Vk250001Cols.WIDTHS[11], Vk250001Cols.H_LEVEL4, "12", white = true)
        HCellH(Vk250001Cols.XS[12], y4, Vk250001Cols.WIDTHS[12], Vk250001Cols.H_LEVEL4, "13", white = true)
        HCellH(Vk250001Cols.XS[13], y4, Vk250001Cols.WIDTHS[13], Vk250001Cols.H_LEVEL4, "D", white = true)
        HCellH(Vk250001Cols.XS[14], y4, Vk250001Cols.WIDTHS[14], Vk250001Cols.H_LEVEL4, "15", white = true)
        HCellH(Vk250001Cols.XS[15], y4, Vk250001Cols.WIDTHS[15], Vk250001Cols.H_LEVEL4, "E", white = true)
        HCellH(Vk250001Cols.XS[16], y4, Vk250001Cols.WIDTHS[16], Vk250001Cols.H_LEVEL4, "17", white = true)
        HCellH(Vk250001Cols.XS[17], y4, Vk250001Cols.WIDTHS[17], Vk250001Cols.H_LEVEL4, "H", white = true)
        HCellH(Vk250001Cols.XS[18], y4, Vk250001Cols.WIDTHS[18], Vk250001Cols.H_LEVEL4, tr(R.string.msg_so_h_e7084ac), white = true)
        HCellH(Vk250001Cols.XS[19], y4, Vk250001Cols.WIDTHS[19], Vk250001Cols.H_LEVEL4, "", white = true)
        HCellH(Vk250001Cols.XS[20], y4, Vk250001Cols.WIDTHS[20], Vk250001Cols.H_LEVEL4, "", white = true)
    }
}

@Composable
private fun HCellH(
    x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false
) {
    val bg = if (white) LocalFormularColors.current.whiteRow else LocalFormularColors.current.header
    val fontSize = if (white) 9.sp else 10.sp

    Box(
        modifier = Modifier
            .formularCellBounds(x, y, width, height)
            .background(bg)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = LocalFormularColors.current.headerText,
            textAlign = TextAlign.Center,
            lineHeight = (fontSize.value + 1).sp,
            maxLines = 8
        )
    }
}

@Composable
private fun HCellV(
    x: Dp, y: Dp, width: Dp, height: Dp, text: String
) {
    Box(
        modifier = Modifier
            .formularCellBounds(x, y, width, height)
            .background(LocalFormularColors.current.header)
            .border(0.5.dp, LocalFormularColors.current.grid),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .rotate(-90f)
        ) {
            Text(
                text = text,
                fontSize = Vk250001Cols.V_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                color = LocalFormularColors.current.headerText,
                textAlign = TextAlign.Center,
                lineHeight = Vk250001Cols.V_LINE_HEIGHT.sp,
                softWrap = true,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .width(height)
                    .padding(2.dp)
            )
        }
    }
}

// ============================================================
// Стартовая строка
// ============================================================

@Composable
private fun StartRow(
    start: Vk250001StartEntity,
    onUpdate: (Vk250001StartEntity) -> Unit
) {
    val w = Vk250001Cols.WIDTHS
    FormularRow(widths = w, modifier = Modifier.width(Vk250001Cols.TOTAL_WIDTH)) {
        CellDateField(w[0], start.date) { onUpdate(start.copy(date = it)) }
        CellIntField(w[1], start.i) { onUpdate(start.copy(i = it)) }
        CellEmpty(w[2])
        CellDoubleField(w[3], start.g) { onUpdate(start.copy(g = it)) }
        CellEmpty(w[4])
        CellTimeField(w[5], start.colSNE) { onUpdate(start.copy(colSNE = it)) }
        CellTimeField(w[6], start.pprA) { onUpdate(start.copy(pprA = it)) }
        CellDoubleField(w[7], start.col8) { onUpdate(start.copy(col8 = it)) }
        CellDoubleField(w[8], start.b) { onUpdate(start.copy(b = it)) }
        CellDoubleField(w[9], start.col10) { onUpdate(start.copy(col10 = it)) }
        CellDoubleField(w[10], start.col11) { onUpdate(start.copy(col11 = it)) }
        CellDoubleField(w[11], start.col12) { onUpdate(start.copy(col12 = it)) }
        CellEmpty(w[12])
        CellDoubleField(w[13], start.d) { onUpdate(start.copy(d = it)) }
        CellEmpty(w[14])
        CellDoubleField(w[15], start.e) { onUpdate(start.copy(e = it)) }
        CellEmpty(w[16])
        CellDoubleField1(w[17], start.h) { onUpdate(start.copy(h = it)) }
        CellDoubleField1(w[18], start.pprH) { onUpdate(start.copy(pprH = it)) }
        CellTextField(w[19], "") { }
        CellEmpty(w[20])
    }
}

// ============================================================
// Строка данных
// ============================================================

@Composable
private fun DataRow(record: Vk250001Entity, onDeleteDay: suspend (String) -> Unit) {
    val w = Vk250001Cols.WIDTHS
    Column(modifier = Modifier.width(Vk250001Cols.TOTAL_WIDTH)) {
        FormularRow(
            widths = w,
            modifier = Modifier
                .width(Vk250001Cols.TOTAL_WIDTH)
                .then(
                    if (record.needsManualRecalc) {
                        Modifier.border(2.dp, LocalFormularColors.current.error)
                    } else {
                        Modifier
                    }
                )
        ) {
            CellText(w[0], displayDate(record.date), isTotal = record.isTotal)
            CellText(w[1], if (record.i == 0) "" else record.i.toString(), isTotal = record.isTotal)
            CellText(w[2], "", isTotal = record.isTotal)
            CellText(w[3], formatDouble(record.g), isTotal = record.isTotal)
            CellText(w[4], "", isTotal = record.isTotal)
            CellText(w[5], minutesToTime(record.colSNE), isTotal = record.isTotal)
            CellText(w[6], if (record.pprA == 0) "" else minutesToTime(record.pprA), isTotal = record.isTotal)
            CellText(w[7], formatDouble(record.col8), isTotal = record.isTotal)
            CellText(w[8], formatDouble(record.b), isTotal = record.isTotal)
            CellText(w[9], formatDouble(record.col10), isTotal = record.isTotal)
            CellText(w[10], formatDouble(record.col11), isTotal = record.isTotal)
            CellText(w[11], formatDouble(record.col12), isTotal = record.isTotal)
            CellText(w[12], "", isTotal = record.isTotal)
            CellText(w[13], formatDouble(record.d), isTotal = record.isTotal)
            CellText(w[14], "", isTotal = record.isTotal)
            CellText(w[15], formatDouble(record.e), isTotal = record.isTotal)
            CellText(w[16], "", isTotal = record.isTotal)
            CellText(w[17], formatDouble1(record.h), isTotal = record.isTotal)
            CellText(w[18], formatDouble1(record.pprH), isTotal = record.isTotal)
            CellText(w[19], "", isTotal = record.isTotal)
            DeleteDayCell(
                width = w[20],
                height = Vk250001Cols.ROW_HEIGHT,
                date = record.date,
                enabled = !record.isTotal,
                backgroundColor = if (record.isTotal) LocalFormularColors.current.totalRow else LocalFormularColors.current.dataRow,
                gridColor = LocalFormularColors.current.grid,
                onDelete = { onDeleteDay(record.date) }
            )
        }
        if (record.needsManualRecalc) {
            Box(
                modifier = Modifier
                    .width(Vk250001Cols.TOTAL_WIDTH)
                    .height(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tr(R.string.msg_manual_recalculation_is_required_722f7f5),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    color = LocalFormularColors.current.cellText,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================================
// Ячейки
// ============================================================

@Composable
private fun CellText(width: Dp, text: String, isTotal: Boolean = false) {
    val bg = if (isTotal) LocalFormularColors.current.totalRow else LocalFormularColors.current.dataRow
    Box(
        modifier = Modifier
            .width(width)
            .height(Vk250001Cols.ROW_HEIGHT)
            .background(bg)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            color = LocalFormularColors.current.cellText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CellIntField(width: Dp, value: Int, onValueChange: (Int) -> Unit) {
    val initial = if (value == 0) "" else value.toString()
    var tfv by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    var lastCommitted by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastCommitted) {
            val expected = if (value == 0) "" else value.toString()
            tfv = TextFieldValue(expected, TextRange(expected.length))
            lastCommitted = value
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val digits = newValue.text.filter { it.isDigit() }
            tfv = TextFieldValue(digits, TextRange(digits.length))
            val parsed = digits.toIntOrNull() ?: 0
            lastCommitted = parsed
            onValueChange(parsed)
        },
        textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellDoubleField(width: Dp, value: Double, onValueChange: (Double) -> Unit) {
    var tfv by remember { mutableStateOf(TextFieldValue(formatDouble(value), TextRange(formatDouble(value).length))) }
    var lastCommitted by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastCommitted) {
            val expected = formatDouble(value)
            tfv = TextFieldValue(expected, TextRange(expected.length))
            lastCommitted = value
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            var normalized = newValue.text.replace('.', ',')
            normalized = normalized.filter { it.isDigit() || it == ',' }
            tfv = TextFieldValue(normalized, TextRange(normalized.length))
            val parsed = normalized.replace(',', '.').toDoubleOrNull() ?: 0.0
            lastCommitted = parsed
            onValueChange(parsed)
        },
        textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellDoubleField1(width: Dp, value: Double, onValueChange: (Double) -> Unit) {
    var tfv by remember { mutableStateOf(TextFieldValue(formatDouble1(value), TextRange(formatDouble1(value).length))) }
    var lastCommitted by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastCommitted) {
            val expected = formatDouble1(value)
            tfv = TextFieldValue(expected, TextRange(expected.length))
            lastCommitted = value
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            var normalized = newValue.text.replace('.', ',')
            normalized = normalized.filter { it.isDigit() || it == ',' }
            tfv = TextFieldValue(normalized, TextRange(normalized.length))
            val parsed = normalized.replace(',', '.').toDoubleOrNull() ?: 0.0
            lastCommitted = parsed
            onValueChange(parsed)
        },
        textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellTimeField(width: Dp, minutes: Int, onChange: (Int) -> Unit) {
    val initial = minutesToTime(minutes)
    var tfv by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    var isError by remember { mutableStateOf(false) }
    var lastCommitted by remember { mutableIntStateOf(minutes) }

    LaunchedEffect(minutes) {
        if (minutes != lastCommitted) {
            val expected = minutesToTime(minutes)
            tfv = TextFieldValue(expected, TextRange(expected.length))
            lastCommitted = minutes
            isError = false
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatTimeInput(newValue.text)
            var error = false
            var parsed = 0
            if (formatted.contains(':')) {
                val parts = formatted.split(':')
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val effectiveM = if (parts.getOrNull(1)?.length == 1) m * 10 else m
                if (effectiveM > 59) error = true
                else parseTimeInput(formatted)?.let { parsed = it }
            } else {
                formatted.toIntOrNull()?.let { parsed = it * 60 }
            }
            isError = error
            val display = if (error) tr(R.string.msg_error_72aecd9) else formatted
            tfv = TextFieldValue(display, TextRange(display.length))
            if (!error) { lastCommitted = parsed; onChange(parsed) }
            else { lastCommitted = 0; onChange(0) }
        },
        textStyle = TextStyle(fontSize = if (isError) 11.sp else 13.sp, textAlign = TextAlign.Center,
            color = if (isError) LocalFormularColors.current.error else LocalFormularColors.current.cellText,
            fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellDateField(width: Dp, dbDate: String, onChange: (String) -> Unit) {
    val initial = displayDate(dbDate)
    var tfv by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    var isError by remember { mutableStateOf(false) }
    var lastCommitted by remember { mutableStateOf(dbDate) }

    LaunchedEffect(dbDate) {
        if (dbDate != lastCommitted) {
            val expected = displayDate(dbDate)
            tfv = TextFieldValue(expected, TextRange(expected.length))
            lastCommitted = dbDate
            isError = false
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatDateInput(newValue.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            val digits = formatted.filter { it.isDigit() }
            if (digits.length == 6) {
                when (val parsed = parseDateInput(formatted)) {
                    tr(R.string.msg_error_72aecd9) -> { isError = true; onChange(""); lastCommitted = "" }
                    "" -> { onChange(""); lastCommitted = "" }
                    else -> { isError = false; onChange(parsed); lastCommitted = parsed }
                }
            } else { onChange(""); lastCommitted = "" }
        },
        textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center,
            color = if (isError) LocalFormularColors.current.error else LocalFormularColors.current.cellText),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellTextField(width: Dp, value: String, onValueChange: (String) -> Unit) {
    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var lastCommitted by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != lastCommitted) {
            tfv = TextFieldValue(value, TextRange(value.length))
            lastCommitted = value
        }
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = TextFieldValue(newValue.text, TextRange(newValue.text.length))
            lastCommitted = newValue.text
            onValueChange(newValue.text)
        },
        textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText),
        singleLine = true,
        modifier = Modifier.bringIntoViewOnFocus().width(width).height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow).border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds().padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellEmpty(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(Vk250001Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
    )
}

// ============================================================
// Форматирование
// ============================================================

private fun formatDouble(value: Double): String {
    if (value == 0.0) return ""
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString().replace('.', ',')
}

private fun formatDouble1(value: Double): String {
    if (value == 0.0) return ""
    return BigDecimal(value).setScale(1, RoundingMode.HALF_UP)
        .toString().replace('.', ',')
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "ВК-2500-01 — таблица",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true
)
@Composable
fun Vk250001TabPreview() {
    MaterialTheme {
        Vk250001TabContent(
            startRecord = Vk250001StartEntity(
                id = 1, profileId = 1,
                date = "01.09.2026",
                i = 1338, g = 22.76, colSNE = 70884, pprA = 30884,
                col8 = 0.736, b = 0.0, col10 = 0.0, col11 = 0.0, col12 = 0.0,
                d = 0.01, e = 0.01, h = 904.4, pprH = 904.4,
                coeffA = 1.01, coeffH = 0.00744
            ),
            records = listOf(
                Vk250001Entity(
                    id = 1, profileId = 1, date = "01.09.2026",
                    i = 1338, g = 22.76, colSNE = 70884, pprA = 30884,
                    col8 = 0.736, d = 0.01, h = 904.4
                ),
                Vk250001Entity(
                    id = 2, profileId = 1, date = "02.09.2026",
                    i = 1339, g = 22.76, colSNE = 70903, pprA = 30903,
                    col8 = 0.736, d = 0.01, h = 905.2
                )
            ),
            onUpdateStart = {}
        )
    }
}
