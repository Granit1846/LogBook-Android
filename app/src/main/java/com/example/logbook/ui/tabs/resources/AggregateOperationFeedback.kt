package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AggregateOperationFeedback(viewModel: ResourcesViewModel) {
    val error by viewModel.operationError.collectAsStateWithLifecycle()
    error?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearOperationError,
            title = { Text(tr(R.string.msg_components_8104a71)) }, text = { Text(com.example.logbook.localization.I18n.display(message)) },
            confirmButton = { TextButton(onClick = viewModel::clearOperationError) { Text(tr(R.string.msg_ok_e40fc0b)) } })
    }
}
