package com.example.logbook.ui.tabs.vr80

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.logbook.data.local.entity.Vr80Entity
import com.example.logbook.data.local.entity.Vr80StartEntity
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

// ============================================================
// Основной экран (с ViewModel)
// ============================================================

@Composable
fun Vr80Tab(profileId: Long) {
    LockLandscape()

    val context = LocalContext.current
    val viewModel: Vr80ViewModel = viewModel(
        key = "vr80_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val records by viewModel.records.collectAsStateWithLifecycle()
    val startRecord by viewModel.startRecord.collectAsStateWithLifecycle()

    LaunchedEffect(startRecord) {
        if (startRecord == null) viewModel.ensureStartExists()
    }

    Vr80TabContent(
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
private fun Vr80TabContent(
    startRecord: Vr80StartEntity?,
    records: List<Vr80Entity>,
    onUpdateStart: (Vr80StartEntity) -> Unit,
    onDeleteDay: suspend (String) -> Unit = {}
) {
    CompositionLocalProvider(LocalFormularColors provides formularColors()) {
        if (startRecord == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@CompositionLocalProvider
        }

        ZoomableTable(minScale = 0.5f, maxScale = 3f) {
            Column(modifier = Modifier.width(Vr80Cols.TOTAL_WIDTH)) {
                HeaderAbsolute()
                StartRow(start = startRecord, onUpdate = onUpdateStart)
                records.forEach { rec -> DataRow(record = rec, onDeleteDay = onDeleteDay) }
                Box(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ============================================================
// Шапка
// ============================================================

@Composable
private fun HeaderAbsolute() {
    Box(
        modifier = Modifier
            .width(Vr80Cols.TOTAL_WIDTH)
            .height(Vr80Cols.HEADER_HEIGHT)
    ) {
        val y1 = 0.dp
        val y2 = Vr80Cols.H_LEVEL1
        val y3 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2
        val y4 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3
        val y5 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val fullH = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val h234 = Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val h34 = Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4

        // ============================================================
        // Rowspan 4: Col0, Col1, Col13, Col14
        // ============================================================
        HCellV(Vr80Cols.XS[0], y1, Vr80Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Vr80Cols.XS[1], y1, Vr80Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Vr80Cols.XS[13], y1, Vr80Cols.WIDTHS[13], fullH, tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(Vr80Cols.XS[14], y1, Vr80Cols.WIDTHS[14], fullH, "")

        // ============================================================
        // Col2-5 — Продолжительность работы ГР на земле
        // ============================================================
        // У1: Col2-5
        val wOnGround = Vr80Cols.WIDTHS[2] + Vr80Cols.WIDTHS[3] +
                Vr80Cols.WIDTHS[4] + Vr80Cols.WIDTHS[5]
        HCellH(Vr80Cols.XS[2], y1, wOnGround, Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_ground_operating_time_04f64fa))

        // У2: Col2-4 — "С приводом от двух двигателей"
        val wTwoEngines = Vr80Cols.WIDTHS[2] + Vr80Cols.WIDTHS[3] + Vr80Cols.WIDTHS[4]
        HCellH(Vr80Cols.XS[2], y2, wTwoEngines, Vr80Cols.H_LEVEL2,
            tr(R.string.msg_driven_by_both_engines_990e953))

        // У2-4: Col5 — "От турбопривода" (rowspan 3)
        HCellV(Vr80Cols.XS[5], y2, Vr80Cols.WIDTHS[5], h234, tr(R.string.msg_turbine_drive_2c2ef82))

        // У3-4: Col2 — "Полное время" (rowspan 2)
        HCellV(Vr80Cols.XS[2], y3, Vr80Cols.WIDTHS[2], h34, tr(R.string.msg_total_time_5bae5f0))

        // У3: Col3-4 — "На режимах"
        HCellH(Vr80Cols.XS[3], y3, Vr80Cols.WIDTHS[3] + Vr80Cols.WIDTHS[4],
            Vr80Cols.H_LEVEL3, tr(R.string.msg_by_mode_a3a328e))

        // У4: Col3 — "Номинальный", Col4 — "Взлётный"
        HCellV(Vr80Cols.XS[3], y4, Vr80Cols.WIDTHS[3], Vr80Cols.H_LEVEL4, tr(R.string.msg_nominal_63e315b))
        HCellV(Vr80Cols.XS[4], y4, Vr80Cols.WIDTHS[4], Vr80Cols.H_LEVEL4, tr(R.string.msg_takeoff_4235534))

        // ============================================================
        // Col6-8 — Продолжительность работы ГР в полёте
        // ============================================================
        // У1: Col6-8
        val wInFlight = Vr80Cols.WIDTHS[6] + Vr80Cols.WIDTHS[7] + Vr80Cols.WIDTHS[8]
        HCellH(Vr80Cols.XS[6], y1, wInFlight, Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_flight_operating_time_8e15cbb))

        // У2-4: Col6 — "Полное время" (rowspan 3)
        HCellV(Vr80Cols.XS[6], y2, Vr80Cols.WIDTHS[6], h234, tr(R.string.msg_total_time_5bae5f0))

        // У2: Col7-8 — "На режимах"
        HCellH(Vr80Cols.XS[7], y2, Vr80Cols.WIDTHS[7] + Vr80Cols.WIDTHS[8],
            Vr80Cols.H_LEVEL2, tr(R.string.msg_by_mode_a3a328e))

        // У3-4: Col7 — "Номинальный" (rowspan 2)
        HCellV(Vr80Cols.XS[7], y3, Vr80Cols.WIDTHS[7], h34, tr(R.string.msg_nominal_63e315b))

        // У3-4: Col8 — "Взлётный" (rowspan 2)
        HCellV(Vr80Cols.XS[8], y3, Vr80Cols.WIDTHS[8], h34, tr(R.string.msg_takeoff_4235534))

        // ============================================================
        // Col9-10 — Сумма работы ГР от двигателей
        // ============================================================
        // У1: Col9-10
        HCellH(Vr80Cols.XS[9], y1, Vr80Cols.WIDTHS[9] + Vr80Cols.WIDTHS[10],
            Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_engine_driven_time_in_all_modes_i_57599f1))

        // У2-4: Col9 — "После последнего ремонта" (rowspan 3)
        HCellV(Vr80Cols.XS[9], y2, Vr80Cols.WIDTHS[9], h234,
            tr(R.string.msg_since_last_overhaul_a1cdda4))

        // У2-4: Col10 — "С начала эксплуатации" (rowspan 3)
        HCellV(Vr80Cols.XS[10], y2, Vr80Cols.WIDTHS[10], h234,
            tr(R.string.msg_since_new_10ee563))

        // ============================================================
        // Col11-12 — Сумма работы ГР от турбопривода
        // ============================================================
        // У1: Col11-12
        HCellH(Vr80Cols.XS[11], y1, Vr80Cols.WIDTHS[11] + Vr80Cols.WIDTHS[12],
            Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_turbine_driven_operating_time_4478b34))

        // У2-4: Col11 — "После последнего ремонта" (rowspan 3)
        HCellV(Vr80Cols.XS[11], y2, Vr80Cols.WIDTHS[11], h234,
            tr(R.string.msg_since_last_overhaul_a1cdda4))

        // У2-4: Col12 — "С начала эксплуатации" (rowspan 3)
        HCellV(Vr80Cols.XS[12], y2, Vr80Cols.WIDTHS[12], h234,
            tr(R.string.msg_since_new_10ee563))

        // ============================================================
        // Уровень 5 — белая строка
        // ============================================================
        HCellH(Vr80Cols.XS[0], y5, Vr80Cols.WIDTHS[0], Vr80Cols.H_LEVEL5, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Vr80Cols.XS[1], y5, Vr80Cols.WIDTHS[1], Vr80Cols.H_LEVEL5, tr(R.string.msg_starts_c001fc7), white = true)
        HCellH(Vr80Cols.XS[2], y5, Vr80Cols.WIDTHS[2], Vr80Cols.H_LEVEL5, tr(R.string.msg_ground_42ff10b), white = true)
        HCellH(Vr80Cols.XS[3], y5, Vr80Cols.WIDTHS[3], Vr80Cols.H_LEVEL5, "4", white = true)
        HCellH(Vr80Cols.XS[4], y5, Vr80Cols.WIDTHS[4], Vr80Cols.H_LEVEL5, "5", white = true)
        HCellH(Vr80Cols.XS[5], y5, Vr80Cols.WIDTHS[5], Vr80Cols.H_LEVEL5, "6", white = true)
        HCellH(Vr80Cols.XS[6], y5, Vr80Cols.WIDTHS[6], Vr80Cols.H_LEVEL5, tr(R.string.msg_flight_62a693a), white = true)
        HCellH(Vr80Cols.XS[7], y5, Vr80Cols.WIDTHS[7], Vr80Cols.H_LEVEL5, "8", white = true)
        HCellH(Vr80Cols.XS[8], y5, Vr80Cols.WIDTHS[8], Vr80Cols.H_LEVEL5, "9", white = true)
        HCellH(Vr80Cols.XS[9], y5, Vr80Cols.WIDTHS[9], Vr80Cols.H_LEVEL5, "24", white = true)
        HCellH(Vr80Cols.XS[10], y5, Vr80Cols.WIDTHS[10], Vr80Cols.H_LEVEL5, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Vr80Cols.XS[11], y5, Vr80Cols.WIDTHS[11], Vr80Cols.H_LEVEL5, "26", white = true)
        HCellH(Vr80Cols.XS[12], y5, Vr80Cols.WIDTHS[12], Vr80Cols.H_LEVEL5, "27", white = true)
        HCellH(Vr80Cols.XS[13], y5, Vr80Cols.WIDTHS[13], Vr80Cols.H_LEVEL5, "28", white = true)
        HCellH(Vr80Cols.XS[14], y5, Vr80Cols.WIDTHS[14], Vr80Cols.H_LEVEL5, "", white = true)
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
            maxLines = 6
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
                fontSize = Vr80Cols.V_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                color = LocalFormularColors.current.headerText,
                textAlign = TextAlign.Center,
                lineHeight = Vr80Cols.V_LINE_HEIGHT.sp,
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
    start: Vr80StartEntity,
    onUpdate: (Vr80StartEntity) -> Unit
) {
    val w = Vr80Cols.WIDTHS
    FormularRow(widths = w, modifier = Modifier.width(Vr80Cols.TOTAL_WIDTH)) {
        CellDateField(w[0], start.date) { onUpdate(start.copy(date = it)) }
        CellIntField(w[1], start.launchCount) { onUpdate(start.copy(launchCount = it)) }
        CellTimeField(w[2], start.groundHours) { onUpdate(start.copy(groundHours = it)) }
        CellIntField(w[3], start.col4) { onUpdate(start.copy(col4 = it)) }
        CellIntField(w[4], start.col5) { onUpdate(start.copy(col5 = it)) }
        CellTimeField(w[5], start.col6) { onUpdate(start.copy(col6 = it)) }
        CellTimeField(w[6], start.col7) { onUpdate(start.copy(col7 = it)) }
        CellIntField(w[7], start.col8) { onUpdate(start.copy(col8 = it)) }
        CellIntField(w[8], start.col9) { onUpdate(start.copy(col9 = it)) }
        CellTimeField(w[9], start.col24) { onUpdate(start.copy(col24 = it)) }
        CellTimeField(w[10], start.colSNE) { onUpdate(start.copy(colSNE = it)) }
        CellTimeField(w[11], start.col26) { onUpdate(start.copy(col26 = it)) }
        CellTimeField(w[12], start.col27) { onUpdate(start.copy(col27 = it)) }
        CellTextField(w[13], start.col28) { onUpdate(start.copy(col28 = it)) }
        CellEmpty(w[14])
    }
}

// ============================================================
// Строка данных
// ============================================================

@Composable
private fun DataRow(record: Vr80Entity, onDeleteDay: suspend (String) -> Unit) {
    val w = Vr80Cols.WIDTHS
    Column(modifier = Modifier.width(Vr80Cols.TOTAL_WIDTH)) {
        FormularRow(
            widths = w,
            modifier = Modifier
                .width(Vr80Cols.TOTAL_WIDTH)
                .then(
                    if (record.needsManualRecalc) {
                        Modifier.border(2.dp, LocalFormularColors.current.error)
                    } else {
                        Modifier
                    }
                )
        ) {
            CellText(w[0], displayDate(record.date), isTotal = record.isTotal)
            CellText(w[1], if (record.launchCount == 0) "" else record.launchCount.toString(), isTotal = record.isTotal)
            CellText(w[2], minutesToTime(record.groundHours), isTotal = record.isTotal)
            CellText(w[3], if (record.col4 == 0) "" else record.col4.toString(), isTotal = record.isTotal)
            CellText(w[4], if (record.col5 == 0) "" else record.col5.toString(), isTotal = record.isTotal)
            CellText(w[5], minutesToTime(record.col6), isTotal = record.isTotal)
            CellText(w[6], minutesToTime(record.col7), isTotal = record.isTotal)
            CellText(w[7], if (record.col8 == 0) "" else record.col8.toString(), isTotal = record.isTotal)
            CellText(w[8], if (record.col9 == 0) "" else record.col9.toString(), isTotal = record.isTotal)
            CellText(w[9], minutesToTime(record.col24), isTotal = record.isTotal)
            CellText(w[10], minutesToTime(record.colSNE), isTotal = record.isTotal)
            CellText(w[11], minutesToTime(record.col26), isTotal = record.isTotal)
            CellText(w[12], minutesToTime(record.col27), isTotal = record.isTotal)
            CellText(w[13], record.col28, isTotal = record.isTotal)
            DeleteDayCell(
                width = w[14],
                height = Vr80Cols.ROW_HEIGHT,
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
                    .width(Vr80Cols.TOTAL_WIDTH)
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
            .height(Vr80Cols.ROW_HEIGHT)
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
private fun CellTextField(width: Dp, value: String, onValueChange: (String) -> Unit) {
    var tfv by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
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
        textStyle = TextStyle(
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText
        ),
        singleLine = true,
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Vr80Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellIntField(width: Dp, value: Int, onValueChange: (Int) -> Unit) {
    val initial = if (value == 0) "" else value.toString()
    var tfv by remember {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
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
        textStyle = TextStyle(
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = LocalFormularColors.current.cellText
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Vr80Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellTimeField(width: Dp, minutes: Int, onChange: (Int) -> Unit) {
    val initial = minutesToTime(minutes)
    var tfv by remember {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
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
                if (effectiveM > 59) {
                    error = true
                } else {
                    parseTimeInput(formatted)?.let { parsed = it }
                }
            } else {
                formatted.toIntOrNull()?.let { parsed = it * 60 }
            }

            isError = error
            val display = if (error) tr(R.string.msg_error_72aecd9) else formatted
            tfv = TextFieldValue(display, TextRange(display.length))

            if (!error) {
                lastCommitted = parsed
                onChange(parsed)
            } else {
                lastCommitted = 0
                onChange(0)
            }
        },
        textStyle = TextStyle(
            fontSize = if (isError) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
            color = if (isError) LocalFormularColors.current.error else LocalFormularColors.current.cellText,
            fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Vr80Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellDateField(width: Dp, dbDate: String, onChange: (String) -> Unit) {
    val initial = displayDate(dbDate)
    var tfv by remember {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
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
                    tr(R.string.msg_error_72aecd9) -> {
                        isError = true
                        onChange("")
                        lastCommitted = ""
                    }
                    "" -> {
                        onChange("")
                        lastCommitted = ""
                    }
                    else -> {
                        isError = false
                        onChange(parsed)
                        lastCommitted = parsed
                    }
                }
            } else {
                onChange("")
                lastCommitted = ""
            }
        },
        textStyle = TextStyle(
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = if (isError) LocalFormularColors.current.error else LocalFormularColors.current.cellText
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Vr80Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellEmpty(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(Vr80Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
    )
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "ВР-80 — таблица",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true
)
@Composable
fun Vr80TabPreview() {
    MaterialTheme {
        Vr80TabContent(
            startRecord = Vr80StartEntity(
                id = 1, profileId = 1,
                date = "01.10.2026",
                launchCount = 45,
                groundHours = 856,
                col6 = 529,
                col7 = 3401,
                col24 = 315000,
                colSNE = 747309
            ),
            records = listOf(
                Vr80Entity(
                    id = 1, profileId = 1,
                    date = "01.10.2026",
                    launchCount = 4, groundHours = 64,
                    col6 = 4, col7 = 241,
                    col24 = 26044, colSNE = 69253
                ),
                Vr80Entity(
                    id = 2, profileId = 1,
                    date = "02.10.2026",
                    launchCount = 1, groundHours = 11,
                    col6 = 0, col7 = 108,
                    col24 = 26234, colSNE = 69443
                )
            ),
            onUpdateStart = {}
        )
    }
}
