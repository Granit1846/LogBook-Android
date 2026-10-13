package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable as rememberSaveableState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.logbook.data.model.AggregateGroup
import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.PINNED_GROUP_ID
import com.example.logbook.data.model.ResourceItem
import kotlin.math.absoluteValue

@Composable
fun AggregatesTableCards(
    resources: List<ResourceItem>,
    groups: List<AggregateGroup>,
    searchQuery: String,
    sortMode: AggregateSortMode,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    selectedGroupIds: Set<String>,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onUpdate: (Int, ResourceItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onToggleGroupCollapse: (String) -> Unit,
    onToggleGroupSelection: (String) -> Unit,
    onLongPressGroup: (String) -> Unit,
    onDragStart: (Int, List<Int>) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indexed = resources.mapIndexed { index, item -> index to item }

    val pinnedGroup = groups.firstOrNull { it.id == PINNED_GROUP_ID }
    val orderedGroups = buildList {
        pinnedGroup?.let { add(it) }
        addAll(groups.filter { it.id != PINNED_GROUP_ID && it.pinned }.sortedBy { it.order })
        addAll(groups.filter { it.id != PINNED_GROUP_ID && !it.pinned }.sortedBy { it.order })
    }

    var openedGroupId by rememberSaveableState { mutableStateOf<String?>(null) }

    val rootItems = indexed.filter { (_, item) -> item.groupId.isEmpty() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (orderedGroups.isEmpty() && rootItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(tr(R.string.msg_no_components_8a7b478))
            }
            return
        }

        if (orderedGroups.isNotEmpty()) {
            val validGroups = orderedGroups.mapNotNull { group ->
                val groupItems = indexed.filter { (_, item) -> item.groupId == group.id }
                val filtered = if (searchQuery.isBlank()) groupItems
                else groupItems.filter { (_, item) ->
                    AggregatesSort.filter(listOf(item), searchQuery).isNotEmpty()
                }
                if (filtered.isEmpty()) null else group to filtered
            }

            val groupsWithoutOpened = validGroups.filter { it.first.id != openedGroupId }
            val chunks = groupsWithoutOpened.chunked(6)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunks.forEach { rowGroups ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGroups.forEach { (group, filteredItems) ->
                            Box(modifier = Modifier.weight(1f)) {
                                GroupCard(
                                    group = group,
                                    itemCount = filteredItems.size,
                                    selectionMode = selectionMode,
                                    selected = group.id in selectedGroupIds,
                                    onClick = {
                                        if (selectionMode) onToggleGroupSelection(group.id)
                                        else openedGroupId = group.id
                                    },
                                    onLongPress = {
                                        onLongPressGroup(group.id)
                                    }
                                )
                            }
                        }
                        repeat(6 - rowGroups.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            validGroups.firstOrNull { it.first.id == openedGroupId }?.let { (group, filteredItems) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    OpenedGroupCard(
                        group = group,
                        items = filteredItems,
                        sortMode = sortMode,
                        selectionMode = selectionMode,
                        editingIndex = editingIndex,
                        selectedIndices = selectedIndices,
                        selectedGroup = group.id in selectedGroupIds,
                        draggingIndex = draggingIndex,
                        dragOffsetY = dragOffsetY,
                        targetInsertIndex = targetInsertIndex,
                        targetInsertAfter = targetInsertAfter,
                        onClose = { openedGroupId = null },
                        onUpdate = onUpdate,
                        onToggleSelection = onToggleSelection,
                        onLongPressRow = onLongPressRow,
                        onToggleGroupSelection = { onToggleGroupSelection(group.id) },
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }
            }
        }

        if (rootItems.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = tr(R.string.msg_ungrouped_94921eb),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            val sortedRoot = if (editingIndex != null || sortMode == AggregateSortMode.NONE) {
                rootItems.map { it.second }
            } else {
                AggregatesSort.apply(rootItems.map { it.second }, sortMode)
            }
            val filteredRoot = sortedRoot.filter { item ->
                AggregatesSort.filter(listOf(item), searchQuery).isNotEmpty()
            }

            RootRowsSection(
                indexed = indexed,
                items = filteredRoot,
                selectionMode = selectionMode,
                editingIndex = editingIndex,
                selectedIndices = selectedIndices,
                draggingIndex = draggingIndex,
                dragOffsetY = dragOffsetY,
                targetInsertIndex = targetInsertIndex,
                targetInsertAfter = targetInsertAfter,
                onUpdate = onUpdate,
                onToggleSelection = onToggleSelection,
                onLongPressRow = onLongPressRow,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: AggregateGroup,
    itemCount: Int,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!selectionMode) onLongPress()
                    else onClick()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = iconFor(group.name),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = "$itemCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OpenedGroupCard(
    group: AggregateGroup,
    items: List<Pair<Int, ResourceItem>>,
    sortMode: AggregateSortMode,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    selectedGroup: Boolean,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onClose: () -> Unit,
    onUpdate: (Int, ResourceItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onToggleGroupSelection: () -> Unit,
    onDragStart: (Int, List<Int>) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { clip = false }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClose)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconFor(group.name),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${items.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = tr(R.string.msg_collapse_5dd6dcd),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            val sortedItems = if (editingIndex != null || sortMode == AggregateSortMode.NONE) {
                items
            } else {
                val sortedByMode = AggregatesSort.apply(items.map { it.second }, sortMode)
                val mapByIdentity = items.associateBy { it.second }
                sortedByMode.mapNotNull { mapByIdentity[it] }
            }

            val contextIndices = sortedItems.map { it.first }

            InlineRowsWithHeader(
                items = sortedItems,
                contextIndices = contextIndices,
                selectionMode = selectionMode,
                editingIndex = editingIndex,
                selectedIndices = selectedIndices,
                draggingIndex = draggingIndex,
                dragOffsetY = dragOffsetY,
                targetInsertIndex = targetInsertIndex,
                targetInsertAfter = targetInsertAfter,
                onUpdate = onUpdate,
                onToggleSelection = onToggleSelection,
                onLongPressRow = onLongPressRow,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            )
        }
    }
}

@Composable
private fun InlineRowsWithHeader(
    items: List<Pair<Int, ResourceItem>>,
    contextIndices: List<Int>,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onUpdate: (Int, ResourceItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onDragStart: (Int, List<Int>) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit
) {
    val wName = 210.dp
    val wSne = 80.dp
    val wRes1 = 100.dp
    val wRem1 = 90.dp
    val wYears = 80.dp
    val wIssue = 100.dp
    val wRem2 = 90.dp
    val wInst1 = 100.dp
    val wInst2 = 100.dp

    val hScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .graphicsLayer { clip = false }
    ) {
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HeaderCell(tr(R.string.msg_component_name_b06a232), wName)
            HeaderCell(tr(R.string.msg_sn_835d66a), wSne)
            HeaderCell(tr(R.string.msg_assigned_life_h_min_87852f8), wRes1)
            HeaderCell(tr(R.string.msg_remaining_h_min_92e8527), wRem1)
            HeaderCell(tr(R.string.msg_assigned_life_years_d81be85), wYears)
            HeaderCell(tr(R.string.msg_manufacture_date_a6d626c), wIssue)
            HeaderCell(tr(R.string.msg_remaining_yy_mm_1d8679f), wRem2)
            HeaderCell(tr(R.string.msg_operating_time_at_installation_0fd5d75), wInst1)
            HeaderCell(tr(R.string.msg_operating_time_before_installation_84bfc49), wInst2)
        }
        HorizontalDivider()

        items.forEach { (idx, item) ->
            if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
                InsertionIndicator(width = 900.dp)
            }
            Row(
                modifier = Modifier.zIndex(if (draggingIndex == idx) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AggregateRow(
                    item = item,
                    isFirst = item.name == "Фюзеляж",
                    selectionMode = selectionMode,
                    selected = idx in selectedIndices,
                    editable = editingIndex == idx,
                    rowIndex = idx,
                    isDragging = draggingIndex == idx,
                    dragOffsetY = if (draggingIndex == idx) dragOffsetY else 0f,
                    compactDragVisual = false,
                    onUpdate = { onUpdate(idx, it) },
                    onToggleSelection = { onToggleSelection(idx) },
                    onLongPress = { onLongPressRow(idx) },
                    onDragStart = { startIdx -> onDragStart(startIdx, contextIndices) },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                    wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                    wInst1 = wInst1, wInst2 = wInst2
                )
            }
            HorizontalDivider()
            if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                InsertionIndicator(width = 900.dp)
            }
        }
    }
}

@Composable
private fun RootRowsSection(
    indexed: List<Pair<Int, ResourceItem>>,
    items: List<ResourceItem>,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onUpdate: (Int, ResourceItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onDragStart: (Int, List<Int>) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit
) {
    val hScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    val contextIndices = items.mapNotNull { item ->
        indexed.firstOrNull { it.second === item }?.first
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .graphicsLayer { clip = false }
    ) {
        items.forEach { item ->
            val idx = indexed.first { it.second === item }.first
            if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
                InsertionIndicator(width = 900.dp)
            }
            Row(
                modifier = Modifier.zIndex(if (draggingIndex == idx) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AggregateRow(
                    item = item,
                    isFirst = item.name == "Фюзеляж",
                    selectionMode = selectionMode,
                    selected = idx in selectedIndices,
                    editable = editingIndex == idx,
                    rowIndex = idx,
                    isDragging = draggingIndex == idx,
                    dragOffsetY = if (draggingIndex == idx) dragOffsetY else 0f,
                    compactDragVisual = false,
                    onUpdate = { onUpdate(idx, it) },
                    onToggleSelection = { onToggleSelection(idx) },
                    onLongPress = { onLongPressRow(idx) },
                    onDragStart = { startIdx -> onDragStart(startIdx, contextIndices) },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    wName = 210.dp, wSne = 80.dp, wRes1 = 100.dp, wRem1 = 90.dp,
                    wYears = 80.dp, wIssue = 100.dp, wRem2 = 90.dp,
                    wInst1 = 100.dp, wInst2 = 100.dp
                )
            }
            HorizontalDivider()
            if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                InsertionIndicator(width = 900.dp)
            }
        }
    }
}

@Composable
private fun InsertionIndicator(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}

private fun iconFor(name: String): ImageVector {
    val icons = listOf(
        Icons.Default.Build,
        Icons.Default.Flight,
        Icons.Default.Memory,
        Icons.Default.Settings,
        Icons.Default.Speed
    )
    if (name.isBlank()) return icons[0]
    val idx = (name.hashCode().absoluteValue) % icons.size
    return icons[idx]
}