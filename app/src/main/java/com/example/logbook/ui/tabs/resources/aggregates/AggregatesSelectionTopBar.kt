package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Верхняя панель в режиме выделения — уменьшенная высота (32dp вместо 64dp).
 */
@Composable
fun AggregatesSelectionTopBar(
    selectionState: AggregatesSelectionState,
    editingIndex: Int?,
    onCancel: () -> Unit,
    onCreateGroup: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✕
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = tr(R.string.msg_clear_selection_716e537),
                    modifier = Modifier.size(24.dp)
                )
            }

            // «N выбрано»
            Text(
                text = tr(R.string.msg_1_s_selected_95eda55, selectionState.count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )

            // Растяжка
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            // Карандаш — если выбрана ровно одна строка (не группа)
            if (selectionState.selectedIndices.size == 1 && selectionState.singleGroupSelected == null) {
                val isEditing = editingIndex == selectionState.selectedIndices.first()
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (isEditing) tr(R.string.msg_finish_editing_a466547) else tr(R.string.msg_edit_901beb5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Создать группу / Переименовать
            val singleGroup = selectionState.singleGroupSelected
            if (singleGroup != null) {
                IconButton(
                    onClick = { onRenameGroup(singleGroup) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = tr(R.string.msg_rename_715e8f0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (selectionState.hasItemSelection) {
                IconButton(
                    onClick = onCreateGroup,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = tr(R.string.msg_create_group_305f472),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}