package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Нижняя панель в режиме выделения — уменьшенная высота (32dp вместо 64dp).
 */
@Composable
fun AggregatesSelectionBottomBar(
    selectionState: AggregatesSelectionState,
    allSelectedPinned: Boolean,
    allSelectedInRoot: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDelete: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(32.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val singleGroup = selectionState.singleGroupSelected
            if (singleGroup != null) {
                // Режим группы
                BottomAction(
                    icon = Icons.Default.Unarchive,
                    contentDesc = tr(R.string.msg_dissolve_ac94170),
                    onClick = { onDeleteGroup(singleGroup) }
                )
                BottomAction(
                    icon = Icons.Default.Delete,
                    contentDesc = tr(R.string.msg_delete_all_e311ec3),
                    onClick = onDelete
                )
                BottomAction(
                    icon = Icons.Default.Checklist,
                    contentDesc = tr(R.string.msg_select_all_bdda768),
                    onClick = onSelectAll
                )
            } else {
                // Режим строк
                if (allSelectedPinned) {
                    BottomAction(
                        icon = Icons.Default.LockOpen,
                        contentDesc = tr(R.string.msg_unpin_4ce0faa),
                        onClick = onUnpin
                    )
                } else {
                    BottomAction(
                        icon = Icons.Default.Lock,
                        contentDesc = tr(R.string.msg_pin_1492abe),
                        onClick = onPin
                    )
                }

                if (!allSelectedInRoot) {
                    BottomAction(
                        icon = Icons.Default.Unarchive,
                        contentDesc = tr(R.string.msg_remove_from_group_13b7a02),
                        onClick = onRemoveFromGroup
                    )
                }

                BottomAction(
                    icon = Icons.Default.Delete,
                    contentDesc = tr(R.string.msg_delete_86ea33a),
                    onClick = onDelete
                )

                BottomAction(
                    icon = Icons.Default.Checklist,
                    contentDesc = tr(R.string.msg_select_all_bdda768),
                    onClick = onSelectAll
                )
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(18.dp)
        )
    }
}