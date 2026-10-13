package com.example.logbook.ui.tabs.resources.oils

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
import com.example.logbook.data.model.OilItem
import com.example.logbook.data.model.PINNED_GROUP_ID
import kotlin.math.absoluteValue

@Composable
fun OilsTableCards(
    oils: List<OilItem>,
    groups: List<AggregateGroup>,
    fuselageSne: Int,
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
    onUpdate: (Int, OilItem) -> Unit,
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
    val indexed = oils.mapIndexed { index, item -> index to item }

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
                Text(tr(R.string.msg_no_oils_c8c7530))
            }
            return
        }

        if (orderedGroups.isNotEmpty()) {
            val validGroups = orderedGroups.mapNotNull { group ->
                val groupItems = indexed.filter { (_, item) -> item.groupId == group.id }
                val filtered = if (searchQuery.isBlank()) groupItems
                else groupItems.filter { (_, item) ->
                    OilsSort.filter(listOf(item), searchQuery).isNotEmpty()
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
                                OilGroupCard(
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
                    OpenedOilGroupCard(
                        group = group,
                        items = filteredItems,
                        fuselageSne = fuselageSne,
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
                OilsSort.apply(rootItems.map { it.second }, sortMode, fuselageSne)
            }
            val filteredRoot = sortedRoot.filter { item ->
                OilsSort.filter(listOf(item), searchQuery).isNotEmpty()
            }

            OilRootRowsSection(
                indexed = indexed,
                items = filteredRoot,
                fuselageSne = fuselageSne,
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
private fun OilGroupCard(
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
                imageVector = oilIconFor(group.name),
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
private fun OpenedOilGroupCard(
    group: AggregateGroup,
    items: List<Pair<Int, OilItem>>,
    fuselageSne: Int,
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
    onUpdate: (Int, OilItem) -> Unit,
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
                    imageVector = oilIconFor(group.name),
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
                val sortedByMode = OilsSort.apply(items.map { it.second }, sortMode, fuselageSne)
                val mapByIdentity = items.associateBy { it.second }
                sortedByMode.mapNotNull { mapByIdentity[it] }
            }

            val contextIndices = sortedItems.map { it.first }

            OilInlineRowsWithHeader(
                items = sortedItems,
                contextIndices = contextIndices,
                fuselageSne = fuselageSne,
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
private fun OilInlineRowsWithHeader(
    items: List<Pair<Int, OilItem>>,
    contextIndices: List<Int>,
    fuselageSne: Int,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onUpdate: (Int, OilItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onDragStart: (Int, List<Int>) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit
) {
    val wName = 120.dp
    val wByHours = 90.dp
    val wByYears = 90.dp
    val wRem1 = 80.dp
    val wRem2 = 80.dp
    val wCoef = 90.dp
    val wTerm = 70.dp

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
            OilHeaderCell(tr(R.string.msg_location_67e4235), wName)
            OilHeaderCell(tr(R.string.msg_tsn_at_filling_6a89b3b), wByHours)
            OilHeaderCell(tr(R.string.msg_filling_date_1191c80), wByYears)
            OilHeaderCell(tr(R.string.msg_remaining_hh_mm_7b50555), wRem1)
            OilHeaderCell(tr(R.string.msg_remaining_yy_mm_b392e31), wRem2)
            OilHeaderCell(tr(R.string.msg_service_life_in_hours_50794b6), wCoef)
            OilHeaderCell(tr(R.string.msg_service_life_in_years_9a9952b), wTerm)
        }
        HorizontalDivider()

        items.forEach { (idx, item) ->
            if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
                OilInsertionIndicator(width = 620.dp)
            }
            Row(
                modifier = Modifier.zIndex(if (draggingIndex == idx) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OilRow(
                    item = item,
                    fuselageSne = fuselageSne,
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
                    wName = wName, wByHours = wByHours, wByYears = wByYears,
                    wRem1 = wRem1, wRem2 = wRem2, wCoef = wCoef, wTerm = wTerm
                )
            }
            HorizontalDivider()
            if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                OilInsertionIndicator(width = 620.dp)
            }
        }
    }
}

@Composable
private fun OilRootRowsSection(
    indexed: List<Pair<Int, OilItem>>,
    items: List<OilItem>,
    fuselageSne: Int,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    draggingIndex: Int?,
    dragOffsetY: Float,
    targetInsertIndex: Int?,
    targetInsertAfter: Boolean,
    onUpdate: (Int, OilItem) -> Unit,
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
                OilInsertionIndicator(width = 620.dp)
            }
            Row(
                modifier = Modifier.zIndex(if (draggingIndex == idx) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OilRow(
                    item = item,
                    fuselageSne = fuselageSne,
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
                    wName = 120.dp, wByHours = 90.dp, wByYears = 90.dp,
                    wRem1 = 80.dp, wRem2 = 80.dp, wCoef = 90.dp, wTerm = 70.dp
                )
            }
            HorizontalDivider()
            if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                OilInsertionIndicator(width = 620.dp)
            }
        }
    }
}

@Composable
private fun OilInsertionIndicator(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun OilHeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
    }
}

private fun oilIconFor(name: String): ImageVector {
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