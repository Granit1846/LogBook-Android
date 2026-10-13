package com.example.logbook.ui.tabs.chast1

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChast1Dialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, zemlya: Double, vozduh: Double, landings: Int) -> Unit
) {
    var date by remember { mutableStateOf(todayString()) }
    var zemlya by remember { mutableStateOf("") }
    var vozduh by remember { mutableStateOf("") }
    var landings by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.msg_new_entry_part_1_d3732df)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(tr(R.string.msg_date_dd_mm_yyyy_b75f44b)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = tr(R.string.msg_choose_date_642d36d))
                    }
                }
                OutlinedTextField(
                    value = zemlya,
                    onValueChange = { zemlya = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(tr(R.string.msg_ground_time_hours_29348d5)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vozduh,
                    onValueChange = { vozduh = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(tr(R.string.msg_flight_time_hours_b07035a)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = landings,
                    onValueChange = { landings = it.filter { ch -> ch.isDigit() } },
                    label = { Text(tr(R.string.msg_aeroplane_landings_count_2eebe2a)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        date,
                        zemlya.toDoubleOrNull() ?: 0.0,
                        vozduh.toDoubleOrNull() ?: 0.0,
                        landings.toIntOrNull() ?: 0
                    )
                },
                enabled = date.isNotBlank()
            ) { Text(tr(R.string.msg_create_84370a2)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr(R.string.msg_cancel_0ec753b)) }
        }
    )

    if (showDatePicker) {
        ScrollableDatePickerDialog(initialDate = com.example.logbook.data.model.ResourceDates.parse(date) ?: java.time.LocalDate.now(),
            onSelect = { date = it.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")); showDatePicker = false },
            onDismiss = { showDatePicker = false })
    }
}

private fun todayString(): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
}

private fun millisToDateString(millis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
}