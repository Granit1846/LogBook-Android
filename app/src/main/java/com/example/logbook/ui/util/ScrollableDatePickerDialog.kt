package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableDatePickerDialog(initialDate: LocalDate, onSelect: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis =
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(8.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        DatePicker(state = picker)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(tr(R.string.msg_cancel_0ec753b)) }
                        TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                            picker.selectedDateMillis?.let {
                                onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                            }
                        }) { Text(tr(R.string.msg_select_fe4b0c8)) }
                    }
                }
            }
        }
    }
}
