package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logbook.data.model.OilItem
import com.example.logbook.data.model.calcRemaining1
import com.example.logbook.data.model.calcRemaining2
import com.example.logbook.ui.util.CompactTopBar
import com.example.logbook.ui.util.LockLandscape
import com.example.logbook.ui.util.formatDateInput
import com.example.logbook.ui.util.formatTimeInput
import com.example.logbook.ui.util.minutesToTime
import com.example.logbook.ui.util.parseTimeInput
import com.example.logbook.ui.util.selectAllOnFocus

// ============================================================
// Диалог «Масла»
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OilsDialog(
    viewModel: ResourcesViewModel,
    onDismiss: () -> Unit
) {
    LockLandscape()

    val oils by viewModel.oils.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

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
                    title = tr(R.string.msg_oils_0331c66),
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onDismiss
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    OilsTable(
                        oils = oils,
                        fuselageSne = profile?.fuselageSne ?: 0,
                        onUpdate = { index, item -> viewModel.updateOil(index, item) },
                        onDelete = { item -> viewModel.removeOil(item) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    OutlinedFloatingActionButton(
                        onClick = { viewModel.addOil() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 21.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = tr(R.string.msg_add_oil_b489224))
                    }
                }
            }
        }
    }
}

// ============================================================
// Таблица масел
// ============================================================

@Composable
private fun OilsTable(
    oils: List<OilItem>,
    fuselageSne: Int,
    onUpdate: (Int, OilItem) -> Unit,
    onDelete: (OilItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val wName = 120.dp
    val wByHours = 90.dp
    val wByYears = 90.dp
    val wRem1 = 80.dp
    val wRem2 = 80.dp
    val wCoef = 90.dp
    val wTerm = 70.dp
    val wAct = 40.dp

    Column(modifier = modifier) {
        if (oils.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(tr(R.string.msg_no_oils_tap_add_cd1f9e7))
            }
            return
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
        ) {
            Column {
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    HeaderCell(tr(R.string.msg_location_67e4235), wName)
                    HeaderCell(tr(R.string.msg_tsn_at_filling_6a89b3b), wByHours)
                    HeaderCell(tr(R.string.msg_filling_date_1191c80), wByYears)
                    HeaderCell(tr(R.string.msg_remaining_hh_mm_7b50555), wRem1)
                    HeaderCell(tr(R.string.msg_remaining_yy_mm_b392e31), wRem2)
                    HeaderCell(tr(R.string.msg_service_life_in_hours_50794b6), wCoef)
                    HeaderCell(tr(R.string.msg_service_life_in_years_9a9952b), wTerm)
                    HeaderCell("", wAct)
                }

                HorizontalDivider()

                oils.forEachIndexed { index, oil ->
                    OilRow(
                        oil = oil,
                        fuselageSne = fuselageSne,
                        onUpdate = { updated -> onUpdate(index, updated) },
                        onDelete = { onDelete(oil) },
                        wName = wName,
                        wByHours = wByHours,
                        wByYears = wByYears,
                        wRem1 = wRem1,
                        wRem2 = wRem2,
                        wCoef = wCoef,
                        wTerm = wTerm,
                        wAct = wAct
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OilRow(
    oil: OilItem,
    fuselageSne: Int,
    onUpdate: (OilItem) -> Unit,
    onDelete: () -> Unit,
    wName: Dp,
    wByHours: Dp,
    wByYears: Dp,
    wRem1: Dp,
    wRem2: Dp,
    wCoef: Dp,
    wTerm: Dp,
    wAct: Dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OilTextField(
            value = oil.name,
            onValueChange = { onUpdate(oil.copy(name = it)) },
            width = wName
        )
        OilTimeField(
            value = oil.byHours,
            onValueChange = { onUpdate(oil.copy(byHours = it)) },
            width = wByHours
        )
        OilDateField(
            value = oil.byYears,
            onValueChange = { onUpdate(oil.copy(byYears = it)) },
            width = wByYears
        )

        // Remaining1 — геттер от fuselageSne
        val rem1 = oil.calcRemaining1(fuselageSne)
        OilTextReadonly(
            value = if (rem1 == 0) "" else minutesToTime(rem1),
            width = wRem1
        )

        // Remaining2 — геттер от byYears + term
        OilTextReadonly(
            value = oil.calcRemaining2(),
            width = wRem2
        )

        OilTimeField(
            value = oil.coefficient,
            onValueChange = { onUpdate(oil.copy(coefficient = it)) },
            width = wCoef
        )
        OilTermField(
            value = oil.term,
            onValueChange = { onUpdate(oil.copy(term = it)) },
            width = wTerm
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.width(wAct)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = tr(R.string.msg_delete_86ea33a),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OilTextField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
    )
}

@Composable
private fun OilTimeField(
    value: Int,
    onValueChange: (Int) -> Unit,
    width: Dp
) {
    val initialText = remember { minutesToTime(value) }
    var tfv by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatTimeInput(newValue.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            val parsed = parseTimeInput(formatted) ?: 0
            onValueChange(parsed)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
            .selectAllOnFocus(tfv) { tfv = it }
    )
}

@Composable
private fun OilDateField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatDateInput(newValue.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            onValueChange(formatted)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
            .selectAllOnFocus(tfv) { tfv = it }
    )
}

@Composable
private fun OilTextReadonly(
    value: String,
    width: Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OilTermField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf("0,5", "1", "1,5", "2", "2,5", "3", "3,5", "4", "4,5", "5")

    Box(modifier = Modifier.width(width)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(value, fontSize = 12.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Масла — пусто",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun OilsPreviewEmpty() {
    MaterialTheme {
        OilsPreviewContent(oils = emptyList())
    }
}

@Preview(
    name = "Масла — заполнено",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun OilsPreviewFilled() {
    MaterialTheme {
        OilsPreviewContent(
            oils = listOf(
                OilItem(
                    name = tr(R.string.msg_left_eng_50bbbdf),
                    byHours = 1066 * 60 + 26,
                    byYears = "10.09.24",
                    coefficient = 0,
                    term = "2"
                ),
                OilItem(
                    name = tr(R.string.msg_right_eng_2b7891d),
                    byHours = 1066 * 60 + 26,
                    byYears = "10.09.24",
                    coefficient = 0,
                    term = "2"
                ),
                OilItem(
                    name = "ВР-80",
                    byHours = 774 * 60 + 11,
                    byYears = "05.04.24",
                    coefficient = 0,
                    term = "1"
                ),
                OilItem(
                    name = "ПВР(Л)",
                    byHours = 1066 * 60 + 26,
                    byYears = "10.09.24",
                    coefficient = 0,
                    term = "1"
                )
            )
        )
    }
}

// Обёртка — содержимое таблицы масел без Dialog (для Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OilsPreviewContent(
    oils: List<OilItem>
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompactTopBar(
                title = tr(R.string.msg_oils_0331c66),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = {}
            )

            Box(modifier = Modifier.fillMaxSize()) {
                OilsTable(
                    oils = oils,
                    fuselageSne = 0,
                    onUpdate = { _, _ -> },
                    onDelete = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                OutlinedFloatingActionButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 21.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = tr(R.string.msg_add_oil_b489224))
                }
            }
        }
    }
}