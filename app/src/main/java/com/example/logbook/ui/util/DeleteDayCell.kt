package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeleteDayCell(
    width: Dp,
    height: Dp,
    date: String,
    enabled: Boolean,
    backgroundColor: Color,
    gridColor: Color,
    onDelete: suspend () -> Unit
) {
    var showConfirmation by remember(date) { mutableStateOf(false) }
    var deleting by remember(date) { mutableStateOf(false) }
    var failed by remember(date) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(backgroundColor)
            .border(0.5.dp, gridColor)
            .clickable(enabled = enabled && !deleting, role = Role.Button) {
                failed = false
                showConfirmation = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            Icon(
                Icons.Default.Close,
                contentDescription = tr(R.string.msg_delete_all_data_for_1_s_6bef2a7, date),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showConfirmation = false },
                title = { Text(tr(R.string.msg_delete_1_s_40daa5f, date)) },
                text = {
                    Text(
                        if (failed) tr(R.string.msg_could_not_delete_the_day_try_again_39d90c6)
                        else tr(R.string.msg_all_data_for_this_day_will_be_deleted_from_the_1f4138a)
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            deleting = true
                            failed = false
                            scope.launch {
                                try {
                                    onDelete()
                                    showConfirmation = false
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    failed = true
                                } finally {
                                    deleting = false
                                }
                            }
                        }
                    ) { Text(if (deleting) tr(R.string.msg_deleting_ea76db2) else tr(R.string.msg_delete_86ea33a)) }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { showConfirmation = false }) {
                        Text(tr(R.string.msg_cancel_0ec753b))
                    }
                }
            )
        }
    }
}
