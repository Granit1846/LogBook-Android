package com.example.logbook.ui.tabs.chast1

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.example.logbook.ui.theme.FormularColors
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.Chast1Entity
import com.example.logbook.data.local.entity.Chast1StartEntity
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.theme.LocalFormularColors
import com.example.logbook.ui.theme.formularColors
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
fun Chast1Tab(profileId: Long) {
    LockLandscape()

    val context = LocalContext.current
    val viewModel: Chast1ViewModel = viewModel(
        key = "chast1_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val records by viewModel.records.collectAsStateWithLifecycle()
    val startRecord by viewModel.startRecord.collectAsStateWithLifecycle()

    LaunchedEffect(startRecord) {
        if (startRecord == null) viewModel.ensureStartExists()
    }

    val colors = formularColors()

    CompositionLocalProvider(LocalFormularColors provides colors) {
        Chast1TabContent(
            startRecord = startRecord,
            records = records,
            onUpdateStart = { viewModel.updateStart(it) },
            onDeleteDay = { viewModel.deleteDay(it) }
        )
    }
}

// ============================================================
// Содержимое (без ViewModel) — используется и в Preview
// ============================================================

@Composable
private fun Chast1TabContent(
    startRecord: Chast1StartEntity?,
    records: List<Chast1Entity>,
    onUpdateStart: (Chast1StartEntity) -> Unit,
    onDeleteDay: suspend (String) -> Unit = {}
) {
    val colors = LocalFormularColors.current

    if (startRecord == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ZoomableTable(minScale = 0.5f, maxScale = 3f) {
        Column(modifier = Modifier.width(Chast1Cols.TOTAL_WIDTH)) {
            HeaderAbsolute(colors = colors)
            StartRow(start = startRecord, onUpdate = onUpdateStart, colors = colors)
            records.forEach { rec -> DataRow(record = rec, colors = colors, onDeleteDay = onDeleteDay) }
            Box(modifier = Modifier.height(80.dp))
        }
    }
}

// ============================================================
// Шапка
// ============================================================

@Composable
private fun HeaderAbsolute(colors: FormularColors) {
    Box(
        modifier = Modifier
            .width(Chast1Cols.TOTAL_WIDTH)
            .height(Chast1Cols.HEADER_HEIGHT)
    ) {
        val y1 = 0.dp
        val y2 = Chast1Cols.H_LEVEL1
        val y3 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2
        val y4 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3
        val y5 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4

        HCellV(x = Chast1Cols.XS[0], y = 0.dp, width = Chast1Cols.WIDTHS[0], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_date_a5b49d2), colors = colors)
        HCellV(x = Chast1Cols.XS[1], y = 0.dp, width = Chast1Cols.WIDTHS[1], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_flight_type_224b7b5), colors = colors)
        HCellV(x = Chast1Cols.XS[2], y = 0.dp, width = Chast1Cols.WIDTHS[2], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_ground_operating_time_including_20_of_flight_t_dc38bce), colors = colors)
        HCellV(x = Chast1Cols.XS[3], y = 0.dp, width = Chast1Cols.WIDTHS[3], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_flight_duration_h_min_aad3cea), colors = colors)
        HCellV(x = Chast1Cols.XS[14], y = 0.dp, width = Chast1Cols.WIDTHS[14], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_logbook_keeper_s_signature_1ee8168), colors = colors)
        HCellH(x = Chast1Cols.XS[15], y = 0.dp, width = Chast1Cols.WIDTHS[15], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = "", colors = colors)

        HCellV(x = Chast1Cols.XS[12], y = y2, width = Chast1Cols.WIDTHS[12],
            height = Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_pilot_instructor_d07c5b8), colors = colors)
        HCellV(x = Chast1Cols.XS[13], y = y2, width = Chast1Cols.WIDTHS[13],
            height = Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_technician_mechanic_d0a9b58), colors = colors)

        val wTotal1 = Chast1Cols.WIDTHS[4] + Chast1Cols.WIDTHS[5] + Chast1Cols.WIDTHS[6] +
                Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8] + Chast1Cols.WIDTHS[9] +
                Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11]
        HCellH(x = Chast1Cols.XS[4], y = y1, width = wTotal1, height = Chast1Cols.H_LEVEL1, text = tr(R.string.msg_total_operating_time_f693e7c), colors = colors)
        HCellH(x = Chast1Cols.XS[12], y = y1, width = Chast1Cols.WIDTHS[12] + Chast1Cols.WIDTHS[13],
            height = Chast1Cols.H_LEVEL1, text = tr(R.string.msg_surname_5aa7f89), colors = colors)

        HCellH(x = Chast1Cols.XS[4], y = y2, width = Chast1Cols.WIDTHS[4] + Chast1Cols.WIDTHS[5],
            height = Chast1Cols.H_LEVEL2, text = tr(R.string.msg_by_flight_time_h_min_90cde71), colors = colors)
        HCellH(x = Chast1Cols.XS[6], y = y2,
            width = Chast1Cols.WIDTHS[6] + Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8] +
                    Chast1Cols.WIDTHS[9] + Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11],
            height = Chast1Cols.H_LEVEL2, text = tr(R.string.msg_by_landings_b8bf40d), colors = colors)

        HCellV(x = Chast1Cols.XS[4], y = y3, width = Chast1Cols.WIDTHS[4],
            height = Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_since_last_overhaul_a1cdda4), colors = colors)
        HCellV(x = Chast1Cols.XS[5], y = y3, width = Chast1Cols.WIDTHS[5],
            height = Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_since_new_10ee563), colors = colors)
        HCellH(x = Chast1Cols.XS[6], y = y3,
            width = Chast1Cols.WIDTHS[6] + Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8],
            height = Chast1Cols.H_LEVEL3, text = tr(R.string.msg_since_last_overhaul_a1cdda4), colors = colors)
        HCellH(x = Chast1Cols.XS[9], y = y3,
            width = Chast1Cols.WIDTHS[9] + Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11],
            height = Chast1Cols.H_LEVEL3, text = tr(R.string.msg_since_new_10ee563), colors = colors)

        HCellV(x = Chast1Cols.XS[6], y = y4, width = Chast1Cols.WIDTHS[6], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_total_3b51439), colors = colors)
        HCellV(x = Chast1Cols.XS[7], y = y4, width = Chast1Cols.WIDTHS[7], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_aeroplane_mode_c51981c), colors = colors)
        HCellV(x = Chast1Cols.XS[8], y = y4, width = Chast1Cols.WIDTHS[8], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_helicopter_mode_41b9feb), colors = colors)
        HCellV(x = Chast1Cols.XS[9], y = y4, width = Chast1Cols.WIDTHS[9], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_total_3b51439), colors = colors)
        HCellV(x = Chast1Cols.XS[10], y = y4, width = Chast1Cols.WIDTHS[10], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_aeroplane_mode_c51981c), colors = colors)
        HCellV(x = Chast1Cols.XS[11], y = y4, width = Chast1Cols.WIDTHS[11], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_helicopter_mode_41b9feb), colors = colors)

        HCellH(x = Chast1Cols.XS[0], y = y5, width = Chast1Cols.WIDTHS[0], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_date_a5b49d2), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[1], y = y5, width = Chast1Cols.WIDTHS[1], height = Chast1Cols.H_LEVEL5, text = "2", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[2], y = y5, width = Chast1Cols.WIDTHS[2], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_ground_42ff10b), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[3], y = y5, width = Chast1Cols.WIDTHS[3], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_flight_62a693a), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[4], y = y5, width = Chast1Cols.WIDTHS[4], height = Chast1Cols.H_LEVEL5, text = "5", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[5], y = y5, width = Chast1Cols.WIDTHS[5], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_sn_835d66a), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[6], y = y5, width = Chast1Cols.WIDTHS[6], height = Chast1Cols.H_LEVEL5, text = "7", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[7], y = y5, width = Chast1Cols.WIDTHS[7], height = Chast1Cols.H_LEVEL5, text = "8", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[8], y = y5, width = Chast1Cols.WIDTHS[8], height = Chast1Cols.H_LEVEL5, text = "9", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[9], y = y5, width = Chast1Cols.WIDTHS[9], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_landings_c4f82d2), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[10], y = y5, width = Chast1Cols.WIDTHS[10], height = Chast1Cols.H_LEVEL5, text = "11", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[11], y = y5, width = Chast1Cols.WIDTHS[11], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_landings_2_5693047), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[12], y = y5, width = Chast1Cols.WIDTHS[12], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_pilot_857680d), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[13], y = y5, width = Chast1Cols.WIDTHS[13], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_technician_bda968f), colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[14], y = y5, width = Chast1Cols.WIDTHS[14], height = Chast1Cols.H_LEVEL5, text = "15", colors = colors, white = true)
        HCellH(x = Chast1Cols.XS[15], y = y5, width = Chast1Cols.WIDTHS[15], height = Chast1Cols.H_LEVEL5, text = "", colors = colors, white = true)
    }
}

@Composable
private fun HCellH(
    x: Dp, y: Dp, width: Dp, height: Dp, text: String,
    colors: FormularColors,
    white: Boolean = false
) {
    val bg = if (white) colors.whiteRow else colors.header
    val fontSize = if (white) 9.sp else 10.sp

    Box(
        modifier = Modifier
            .formularCellBounds(x, y, width, height)
            .background(bg)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = (fontSize.value + 1).sp,
            maxLines = 6,
            color = colors.headerText
        )
    }
}

@Composable
private fun HCellV(
    x: Dp, y: Dp, width: Dp, height: Dp, text: String,
    colors: FormularColors
) {
    Box(
        modifier = Modifier
            .formularCellBounds(x, y, width, height)
            .background(colors.header)
            .border(0.5.dp, colors.grid),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .rotate(-90f)
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                softWrap = true,
                maxLines = Int.MAX_VALUE,
                color = colors.headerText,
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
    start: Chast1StartEntity,
    onUpdate: (Chast1StartEntity) -> Unit,
    colors: FormularColors
) {
    val w = Chast1Cols.WIDTHS
    FormularRow(widths = w, modifier = Modifier.width(Chast1Cols.TOTAL_WIDTH)) {
        CellDateField(w[0], start.date, colors) { onUpdate(start.copy(date = it)) }
        CellTextField(w[1], start.flightType, colors) { onUpdate(start.copy(flightType = it)) }
        CellTimeField(w[2], start.groundHours, colors) { onUpdate(start.copy(groundHours = it)) }
        CellTimeField(w[3], start.flightDuration, colors) { onUpdate(start.copy(flightDuration = it)) }
        CellTimeField(w[4], start.col5, colors) { onUpdate(start.copy(col5 = it)) }
        CellTimeField(w[5], start.colSNE, colors) { onUpdate(start.copy(colSNE = it)) }
        CellIntField(w[6], start.col7, colors) { onUpdate(start.copy(col7 = it)) }
        CellIntField(w[7], start.col8, colors) { onUpdate(start.copy(col8 = it)) }
        CellIntField(w[8], start.col9, colors) { onUpdate(start.copy(col9 = it)) }
        CellIntField(w[9], start.landings, colors) { onUpdate(start.copy(landings = it)) }
        CellIntField(w[10], start.col11, colors) { onUpdate(start.copy(col11 = it)) }
        CellIntField(w[11], start.landings2, colors) { onUpdate(start.copy(landings2 = it)) }
        CellTextField(w[12], start.pilot, colors) { onUpdate(start.copy(pilot = it)) }
        CellTextField(w[13], start.mechanic, colors) { onUpdate(start.copy(mechanic = it)) }
        CellText(w[14], "", colors = colors)
        CellEmpty(w[15], colors)
    }
}

// ============================================================
// Строка данных
// ============================================================

@Composable
private fun DataRow(record: Chast1Entity, colors: FormularColors, onDeleteDay: suspend (String) -> Unit) {
    val w = Chast1Cols.WIDTHS
    Column(modifier = Modifier.width(Chast1Cols.TOTAL_WIDTH)) {
        FormularRow(
            widths = w,
            modifier = Modifier
                .width(Chast1Cols.TOTAL_WIDTH)
                .then(
                    if (record.needsManualRecalc) {
                        Modifier.border(2.dp, colors.error)
                    } else {
                        Modifier
                    }
                )
        ) {
            CellText(w[0], displayDate(record.date), isTotal = record.isTotal, colors = colors)
            CellText(w[1], record.flightType, isTotal = record.isTotal, colors = colors)
            CellText(w[2], minutesToTime(record.groundHours), isTotal = record.isTotal, colors = colors)
            CellText(w[3], minutesToTime(record.flightDuration), isTotal = record.isTotal, colors = colors)
            CellText(w[4], minutesToTime(record.col5), isTotal = record.isTotal, colors = colors)
            CellText(w[5], minutesToTime(record.colSNE), isTotal = record.isTotal, colors = colors)
            CellText(w[6], if (record.col7 == 0) "" else record.col7.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[7], if (record.col8 == 0) "" else record.col8.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[8], if (record.col9 == 0) "" else record.col9.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[9], if (record.landings == 0) "" else record.landings.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[10], if (record.col11 == 0) "" else record.col11.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[11], if (record.landings2 == 0) "" else record.landings2.toString(), isTotal = record.isTotal, colors = colors)
            CellText(w[12], record.pilot, isTotal = record.isTotal, colors = colors)
            CellText(w[13], record.mechanic, isTotal = record.isTotal, colors = colors)
            CellText(w[14], "", isTotal = record.isTotal, colors = colors)
            DeleteDayCell(
                width = w[15],
                height = Chast1Cols.ROW_HEIGHT,
                date = record.date,
                enabled = !record.isTotal,
                backgroundColor = if (record.isTotal) colors.totalRow else colors.dataRow,
                gridColor = colors.grid,
                onDelete = { onDeleteDay(record.date) }
            )
        }
        if (record.needsManualRecalc) {
            Box(
                modifier = Modifier
                    .width(Chast1Cols.TOTAL_WIDTH)
                    .height(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tr(R.string.msg_manual_recalculation_is_required_722f7f5),
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    color = colors.cellText,
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
private fun CellText(width: Dp, text: String, isTotal: Boolean = false, colors: FormularColors) {
    val bg = if (isTotal) colors.totalRow else colors.dataRow
    Box(
        modifier = Modifier
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(bg)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = colors.cellText
        )
    }
}

@Composable
private fun CellTextField(
    width: Dp, value: String, colors: FormularColors,
    onValueChange: (String) -> Unit
) {
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
            color = colors.cellText
        ),
        singleLine = true,
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(colors.dataRow)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellIntField(
    width: Dp, value: Int, colors: FormularColors,
    onValueChange: (Int) -> Unit
) {
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
            color = colors.cellText
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(colors.dataRow)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellTimeField(
    width: Dp, minutes: Int, colors: FormularColors,
    onChange: (Int) -> Unit
) {
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
            color = if (isError) colors.error else colors.cellText,
            fontWeight = if (isError) FontWeight.Bold else FontWeight.Normal
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(colors.dataRow)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellDateField(
    width: Dp, dbDate: String, colors: FormularColors,
    onChange: (String) -> Unit
) {
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
            color = if (isError) colors.error else colors.cellText
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(colors.dataRow)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
            .padding(4.dp),
        decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { inner() } }
    )
}

@Composable
private fun CellEmpty(width: Dp, colors: FormularColors) {
    Box(
        modifier = Modifier
            .width(width)
            .height(Chast1Cols.ROW_HEIGHT)
            .background(colors.dataRow)
            .border(0.5.dp, colors.grid)
            .clipToBounds()
    )
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Часть 1 — таблица",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape",
    showBackground = true
)
@Composable
private fun Chast1TabPreview() {
    androidx.compose.material3.MaterialTheme {
        CompositionLocalProvider(
            LocalFormularColors provides formularColors()
        ) {
            Chast1TabContent(
                startRecord = Chast1StartEntity(
                    id = 1, profileId = 1,
                    date = "01.10.2026",
                    flightType = "Тренировочный",
                    groundHours = 90, flightDuration = 120,
                    col5 = 1000, colSNE = 5000,
                    col7 = 100, col8 = 50, col9 = 50,
                    landings = 200, col11 = 100, landings2 = 100,
                    pilot = "Иванов", mechanic = "Петров"
                ),
                records = listOf(
                    Chast1Entity(
                        id = 1, profileId = 1,
                        date = "01.10.2026", flightType = "Тренировочный",
                        groundHours = 30, flightDuration = 60,
                        col5 = 1030, colSNE = 5060,
                        col7 = 101, col8 = 1, col9 = 0,
                        landings = 201, col11 = 1, landings2 = 0,
                        pilot = "Иванов", mechanic = "Петров"
                    ),
                    Chast1Entity(
                        id = 2, profileId = 1,
                        date = "02.10.2026", flightType = "Тренировочный",
                        groundHours = 30, flightDuration = 60,
                        col5 = 1060, colSNE = 5120,
                        col7 = 103, col8 = 1, col9 = 1,
                        landings = 203, col11 = 1, landings2 = 1,
                        pilot = "Иванов", mechanic = "Петров"
                    ),
                    Chast1Entity(
                        id = 3, profileId = 1,
                        date = "01.11.2026", isTotal = true,
                        col5 = 30, colSNE = 60,
                        col7 = 3, col8 = 1, col9 = 1,
                        landings = 3, col11 = 1, landings2 = 1
                    )
                ),
                onUpdateStart = {}
            )
        }
    }
}
