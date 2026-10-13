package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val FORMULAR_TYPES = listOf("Chast1", "Vr80", "Ta14", "Vk250001", "Vk250002", "Journal")
private val labels get() = listOf(
    com.example.logbook.localization.I18n.display("Часть 1"),
    com.example.logbook.localization.I18n.display("ВР-80"),
    com.example.logbook.localization.I18n.display("Та-14"),
    com.example.logbook.localization.I18n.display("ВК-2500-01"),
    com.example.logbook.localization.I18n.display("ВК-2500-02"),
    tr(R.string.journal_title)
)

@Composable
fun FormularPrintDialog(profileId: Long, initialType: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: FormularPrintViewModel = viewModel(key = "printing_$profileId", factory = LogbookViewModelFactory(context, profileId))
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf"), vm::save)
    var selection by rememberSaveable { mutableStateOf(ArrayList(initialType?.let { listOf(it) } ?: FORMULAR_TYPES)) }
    var fromText by rememberSaveable { mutableStateOf("") }
    var toText by rememberSaveable { mutableStateOf("") }
    val from = fromText.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    val to = toText.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    var baseline by rememberSaveable { mutableStateOf(true) }
    var totals by rememberSaveable { mutableStateOf(true) }
    var a3 by rememberSaveable { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    val valid = selection.isNotEmpty() && (from == null || to == null || from <= to)
    fun export(save: Boolean) {
        vm.prepare(PrintOptions(selection.toSet(), from, to, baseline, totals), a3, save) { snapshot, filename ->
            if (save) launcher.launch(filename)
            else {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(filename, FormularPrintAdapter(snapshot), PrintAttributes.Builder()
                    .setMediaSize((if (a3) PrintAttributes.MediaSize.ISO_A3 else PrintAttributes.MediaSize.ISO_A4).asLandscape())
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).build())
            }
        }
    }
    Dialog(onDismissRequest = { if (!vm.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr(R.string.msg_print_pdf_73a93ea), style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        FORMULAR_TYPES.forEachIndexed { index, type ->
                            PrintCheck(labels[index], type in selection, !vm.busy) { checked ->
                                selection = ArrayList(if (checked) selection + type else selection - type)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { picker = "from" }, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                                Text(tr(R.string.msg_from_f6ca017) + (from?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: tr(R.string.msg_start_6cc4504)))
                            }
                            OutlinedButton(onClick = { picker = "to" }, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                                Text(tr(R.string.msg_to_f2d93cf) + (to?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: tr(R.string.msg_end_909d249)))
                            }
                        }
                        TextButton(onClick = { fromText = ""; toText = "" }, enabled = !vm.busy) { Text(tr(R.string.msg_all_dates_c2001e5)) }
                        if (from != null && to != null && from > to) Text(tr(R.string.msg_the_start_date_is_after_the_end_date_954bf8b), color = MaterialTheme.colorScheme.error)
                        PrintCheck(tr(R.string.msg_initial_values_33b6a4b), baseline, !vm.busy) { baseline = it }
                        PrintCheck(tr(R.string.msg_totals_for_fully_selected_months_abee96c), totals, !vm.busy) { totals = it }
                        PrintCheck(tr(R.string.msg_a3_paper_a4_by_default_6911b23), a3, !vm.busy) { a3 = it }
                    }
                    vm.message?.let { Text(it, Modifier.padding(vertical = 8.dp)) }
                    if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { export(false) }, enabled = valid && !vm.busy, modifier = Modifier.weight(1f)) { Text(tr(R.string.msg_print_9f81a37)) }
                        Button(onClick = { export(true) }, enabled = valid && !vm.busy, modifier = Modifier.weight(1f)) { Text(tr(R.string.msg_save_pdf_59ea19b)) }
                    }
                    TextButton(onClick = onDismiss, enabled = !vm.busy, modifier = Modifier.align(Alignment.End)) { Text(tr(R.string.msg_close_4ae50d3)) }
                }
            }
        }
    }
    picker?.let { side ->
        ScrollableDatePickerDialog(if (side == "from") from ?: LocalDate.now() else to ?: LocalDate.now(), {
            if (side == "from") fromText = it.toString() else toText = it.toString()
            picker = null
        }, { picker = null })
    }
}

@Composable
private fun PrintCheck(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange, enabled = enabled)
        Text(label, Modifier.weight(1f))
    }
}
