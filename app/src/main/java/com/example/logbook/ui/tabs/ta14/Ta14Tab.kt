package com.example.logbook.ui.tabs.ta14

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
import com.example.logbook.data.local.entity.Ta14Entity
import com.example.logbook.data.local.entity.Ta14StartEntity
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

@Composable
fun Ta14Tab(profileId: Long) {
    LockLandscape()

    val context = LocalContext.current
    val viewModel: Ta14ViewModel = viewModel(
        key = "ta14_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val records by viewModel.records.collectAsStateWithLifecycle()
    val startRecord by viewModel.startRecord.collectAsStateWithLifecycle()

    LaunchedEffect(startRecord) {
        if (startRecord == null) viewModel.ensureStartExists()
    }

    Ta14TabContent(
        startRecord = startRecord,
        records = records,
        onUpdateStart = { viewModel.updateStart(it) },
        onDeleteDay = { viewModel.deleteDay(it) }
    )
}

@Composable
private fun Ta14TabContent(
    startRecord: Ta14StartEntity?,
    records: List<Ta14Entity>,
    onUpdateStart: (Ta14StartEntity) -> Unit,
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
            Column(modifier = Modifier.width(Ta14Cols.TOTAL_WIDTH)) {
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
            .width(Ta14Cols.TOTAL_WIDTH)
            .height(Ta14Cols.HEADER_HEIGHT)
    ) {
        val y1 = 0.dp
        val y2 = Ta14Cols.H_LEVEL1
        val y3 = Ta14Cols.H_LEVEL1 + Ta14Cols.H_LEVEL2
        val fullH = Ta14Cols.H_LEVEL1 + Ta14Cols.H_LEVEL2

        // Rowspan 2: Col0, Col1, Col2, Col3, Col4, Col5, Col10, Col11
        HCellV(Ta14Cols.XS[0], y1, Ta14Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Ta14Cols.XS[1], y1, Ta14Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Ta14Cols.XS[2], y1, Ta14Cols.WIDTHS[2], fullH,
            tr(R.string.msg_operating_time_and_start_counter_readings_freq_e31b879))
        HCellV(Ta14Cols.XS[3], y1, Ta14Cols.WIDTHS[3], fullH,
            tr(R.string.msg_engine_ground_operating_time_h_min_4eff8e6))
        HCellV(Ta14Cols.XS[4], y1, Ta14Cols.WIDTHS[4], fullH,
            tr(R.string.msg_engine_flight_operating_time_h_min_ad084d1))
        HCellV(Ta14Cols.XS[5], y1, Ta14Cols.WIDTHS[5], fullH,
            tr(R.string.msg_total_engine_operating_time_h_min_e7bc036))
        HCellV(Ta14Cols.XS[10], y1, Ta14Cols.WIDTHS[10], fullH,
            tr(R.string.msg_cycle_counter_reading_98189da))
        HCellH(Ta14Cols.XS[11], y1, Ta14Cols.WIDTHS[11], fullH, "")

        // Col6-9 — группа "Суммарная наработка двигателя, ч.мин/запуски"
        val wGroup = Ta14Cols.WIDTHS[6] + Ta14Cols.WIDTHS[7] +
                Ta14Cols.WIDTHS[8] + Ta14Cols.WIDTHS[9]
        HCellH(Ta14Cols.XS[6], y1, wGroup, Ta14Cols.H_LEVEL1,
            tr(R.string.msg_total_engine_operating_time_h_min_starts_560f455))

        // Уровень 2: Col6-7 — "после последнего капитального ремонта"
        val wAfterRepair = Ta14Cols.WIDTHS[6] + Ta14Cols.WIDTHS[7]
        HCellH(Ta14Cols.XS[6], y2, wAfterRepair, Ta14Cols.H_LEVEL2,
            tr(R.string.msg_since_last_overhaul_00c7522))

        // Уровень 2: Col8-9 — "с начала эксплуатации"
        val wSinceOp = Ta14Cols.WIDTHS[8] + Ta14Cols.WIDTHS[9]
        HCellH(Ta14Cols.XS[8], y2, wSinceOp, Ta14Cols.H_LEVEL2,
            tr(R.string.msg_since_new_f8324b5))

        // Белая строка (уровень 3)
        HCellH(Ta14Cols.XS[0], y3, Ta14Cols.WIDTHS[0], Ta14Cols.H_LEVEL3, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Ta14Cols.XS[1], y3, Ta14Cols.WIDTHS[1], Ta14Cols.H_LEVEL3, tr(R.string.msg_starts_8338da8), white = true)
        HCellH(Ta14Cols.XS[2], y3, Ta14Cols.WIDTHS[2], Ta14Cols.H_LEVEL3, "3", white = true)
        HCellH(Ta14Cols.XS[3], y3, Ta14Cols.WIDTHS[3], Ta14Cols.H_LEVEL3, tr(R.string.msg_time_c80d7e8), white = true)
        HCellH(Ta14Cols.XS[4], y3, Ta14Cols.WIDTHS[4], Ta14Cols.H_LEVEL3, "5", white = true)
        HCellH(Ta14Cols.XS[5], y3, Ta14Cols.WIDTHS[5], Ta14Cols.H_LEVEL3, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Ta14Cols.XS[6], y3, Ta14Cols.WIDTHS[6], Ta14Cols.H_LEVEL3, "7", white = true)
        HCellH(Ta14Cols.XS[7], y3, Ta14Cols.WIDTHS[7], Ta14Cols.H_LEVEL3, "8", white = true)
        HCellH(Ta14Cols.XS[8], y3, Ta14Cols.WIDTHS[8], Ta14Cols.H_LEVEL3, tr(R.string.msg_sn_2_e8543ee), white = true)
        HCellH(Ta14Cols.XS[9], y3, Ta14Cols.WIDTHS[9], Ta14Cols.H_LEVEL3, tr(R.string.msg_starts_2_0264acb), white = true)
        HCellH(Ta14Cols.XS[10], y3, Ta14Cols.WIDTHS[10], Ta14Cols.H_LEVEL3, "11", white = true)
        HCellH(Ta14Cols.XS[11], y3, Ta14Cols.WIDTHS[11], Ta14Cols.H_LEVEL3, "", white = true)
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
                fontSize = Ta14Cols.V_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                color = LocalFormularColors.current.headerText,
                textAlign = TextAlign.Center,
                lineHeight = Ta14Cols.V_LINE_HEIGHT.sp,
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
    start: Ta14StartEntity,
    onUpdate: (Ta14StartEntity) -> Unit
) {
    val w = Ta14Cols.WIDTHS
    FormularRow(widths = w, modifier = Modifier.width(Ta14Cols.TOTAL_WIDTH)) {
        CellDateField(w[0], start.date) { onUpdate(start.copy(date = it)) }
        CellIntField(w[1], start.launchCount) { onUpdate(start.copy(launchCount = it)) }
        CellIntField(w[2], start.col3) { onUpdate(start.copy(col3 = it)) }
        CellTimeField(w[3], start.colVremya) { onUpdate(start.copy(colVremya = it)) }
        CellIntField(w[4], start.col5) { onUpdate(start.copy(col5 = it)) }
        CellTimeField(w[5], start.colSNE) { onUpdate(start.copy(colSNE = it)) }
        CellTimeField(w[6], start.col7) { onUpdate(start.copy(col7 = it)) }
        CellIntField(w[7], start.col8) { onUpdate(start.copy(col8 = it)) }
        CellTimeField(w[8], start.colSNE2) { onUpdate(start.copy(colSNE2 = it)) }
        CellIntField(w[9], start.launchCount2) { onUpdate(start.copy(launchCount2 = it)) }
        CellIntField(w[10], start.col11) { onUpdate(start.copy(col11 = it)) }
        CellEmpty(w[11])
    }
}

// ============================================================
// Строка данных
// ============================================================

@Composable
private fun DataRow(record: Ta14Entity, onDeleteDay: suspend (String) -> Unit) {
    val w = Ta14Cols.WIDTHS
    Column(modifier = Modifier.width(Ta14Cols.TOTAL_WIDTH)) {
        FormularRow(
            widths = w,
            modifier = Modifier
                .width(Ta14Cols.TOTAL_WIDTH)
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
            CellText(w[2], if (record.col3 == 0) "" else record.col3.toString(), isTotal = record.isTotal)
            CellText(w[3], minutesToTime(record.colVremya), isTotal = record.isTotal)
            CellText(w[4], if (record.col5 == 0) "" else record.col5.toString(), isTotal = record.isTotal)
            CellText(w[5], minutesToTime(record.colSNE), isTotal = record.isTotal)
            CellText(w[6], minutesToTime(record.col7), isTotal = record.isTotal)
            CellText(w[7], if (record.col8 == 0) "" else record.col8.toString(), isTotal = record.isTotal)
            CellText(w[8], minutesToTime(record.colSNE2), isTotal = record.isTotal)
            CellText(w[9], if (record.launchCount2 == 0) "" else record.launchCount2.toString(), isTotal = record.isTotal)
            CellText(w[10], if (record.col11 == 0) "" else record.col11.toString(), isTotal = record.isTotal)
            DeleteDayCell(
                width = w[11],
                height = Ta14Cols.ROW_HEIGHT,
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
                    .width(Ta14Cols.TOTAL_WIDTH)
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
            .height(Ta14Cols.ROW_HEIGHT)
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
            .height(Ta14Cols.ROW_HEIGHT)
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
            .height(Ta14Cols.ROW_HEIGHT)
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
            .height(Ta14Cols.ROW_HEIGHT)
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
            .height(Ta14Cols.ROW_HEIGHT)
            .background(LocalFormularColors.current.dataRow)
            .border(0.5.dp, LocalFormularColors.current.grid)
            .clipToBounds()
    )
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Та-14 — таблица",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true
)
@Composable
fun Ta14TabPreview() {
    MaterialTheme {
        Ta14TabContent(
            startRecord = Ta14StartEntity(
                id = 1, profileId = 1,
                date = "01.09.2026",
                launchCount = 45,
                colVremya = 344,
                colSNE = 291244,
                colSNE2 = 291244,
                launchCount2 = 45,
                col7 = 344
            ),
            records = listOf(
                Ta14Entity(
                    id = 1, profileId = 1,
                    date = "01.09.2026",
                    launchCount = 4, colVremya = 16,
                    colSNE = 28741, colSNE2 = 28741, launchCount2 = 1398
                ),
                Ta14Entity(
                    id = 2, profileId = 1,
                    date = "02.09.2026",
                    launchCount = 1, colVremya = 4,
                    colSNE = 28745, colSNE2 = 28745, launchCount2 = 1399
                )
            ),
            onUpdateStart = {}
        )
    }
}
