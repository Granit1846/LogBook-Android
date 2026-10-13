package com.example.logbook.ui.tabs.resources.oils

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logbook.data.model.AggregateGroup
import com.example.logbook.data.model.OilItem

/**
 * Заголовок группы масел.
 *
 * Левая 1/3 — название и счётчик.
 * Правая 2/3 — имена масел (первые 3 + «...»), вертикально центрированы.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OilGroupHeader(
    group: AggregateGroup,
    items: List<OilItem>,
    totalWidth: Dp,
    selectionMode: Boolean,
    selected: Boolean,
    compact: Boolean = false,
    onToggleCollapse: () -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    wCheck: Dp = 36.dp
) {
    val verticalPadding = if (compact) 2.dp else 6.dp
    val titleStyle = if (compact) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.titleSmall
    }
    val iconButtonSize = if (compact) 28.dp else 48.dp

    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = modifier
            .width(totalWidth)
            .background(bg)
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection() else onToggleCollapse()
                },
                onLongClick = {
                    if (!selectionMode) onLongPress() else onToggleSelection()
                }
            )
            .padding(vertical = verticalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (group.collapsed) Icons.Default.ExpandMore
                    else Icons.Default.ExpandLess,
                    contentDescription = if (group.collapsed) tr(R.string.msg_expand_fd3ae0e) else tr(R.string.msg_collapse_4faa82f),
                    modifier = Modifier.size(iconButtonSize)
                )

                Text(
                    text = group.name,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )

                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                if (group.pinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = tr(R.string.msg_pinned_133e615)
                    )
                }
            }

            if (!compact) {
                Column(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    val preview = items.take(3)
                    preview.forEach { item ->
                        Text(
                            text = item.name.ifBlank { "—" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                    if (items.size > 3) {
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}