package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logbook.data.model.AggregateGroup
import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.AggregateViewMode
import com.example.logbook.data.model.PINNED_GROUP_ID
import com.example.logbook.data.model.ResourceItem
import kotlin.math.roundToInt

@Composable
fun AggregatesTable(
    resources: List<ResourceItem>,
    groups: List<AggregateGroup>,
    searchQuery: String,
    sortMode: AggregateSortMode,
    viewMode: AggregateViewMode,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    selectedGroupIds: Set<String>,
    onUpdate: (Int, ResourceItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onLongPressRow: (Int) -> Unit,
    onToggleGroupCollapse: (String) -> Unit,
    onToggleGroupSelection: (String) -> Unit,
    onLongPressGroup: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // ============================================================
    // Состояние drag — ОБЩЕЕ для ROWS и CARDS
    // ============================================================
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragContextIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var dragInitialContextPosition by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val rowHeightPx = with(density) { 26.dp.toPx() }

    fun computeTargetInsert(
        offsetY: Float,
        contextIndices: List<Int>,
        initialPosition: Int
    ): Pair<Int, Boolean>? {
        if (contextIndices.isEmpty()) return null
        val steps = (offsetY / rowHeightPx).roundToInt()
        val rawTarget = initialPosition + steps
        return when {
            rawTarget < 0 -> contextIndices.first() to false
            rawTarget >= contextIndices.size -> contextIndices.last() to true
            else -> contextIndices[rawTarget] to false
        }
    }

    fun startDrag(index: Int, contextIndices: List<Int>) {
        draggingIndex = index
        dragOffsetY = 0f
        dragContextIndices = contextIndices
        dragInitialContextPosition = contextIndices.indexOf(index).coerceAtLeast(0)
    }

    fun updateDrag(amountY: Float) {
        dragOffsetY += amountY
    }

    fun endDrag() {
        val from = draggingIndex
        if (from != null) {
            val targetPair = computeTargetInsert(
                offsetY = dragOffsetY,
                contextIndices = dragContextIndices,
                initialPosition = dragInitialContextPosition
            )
            if (targetPair != null) {
                val (target, insertAfter) = targetPair
                val to = if (insertAfter) target + 1 else target
                if (to != from) {
                    onMove(from, to)
                }
            }
        }
        draggingIndex = null
        dragOffsetY = 0f
        dragContextIndices = emptyList()
        dragInitialContextPosition = 0
    }

    fun cancelDrag() {
        draggingIndex = null
        dragOffsetY = 0f
        dragContextIndices = emptyList()
        dragInitialContextPosition = 0
    }

    val targetInsertPair: Pair<Int, Boolean>? = draggingIndex?.let {
        computeTargetInsert(
            offsetY = dragOffsetY,
            contextIndices = dragContextIndices,
            initialPosition = dragInitialContextPosition
        )
    }
    val targetInsertIndex: Int? = targetInsertPair?.first
    val targetInsertAfter: Boolean = targetInsertPair?.second ?: false

    // ============================================================
    // Режим карточек
    // ============================================================
    if (viewMode == AggregateViewMode.CARDS) {
        AggregatesTableCards(
            resources = resources,
            groups = groups,
            searchQuery = searchQuery,
            sortMode = sortMode,
            selectionMode = selectionMode,
            editingIndex = editingIndex,
            selectedIndices = selectedIndices,
            selectedGroupIds = selectedGroupIds,
            draggingIndex = draggingIndex,
            dragOffsetY = dragOffsetY,
            targetInsertIndex = targetInsertIndex,
            targetInsertAfter = targetInsertAfter,
            onUpdate = onUpdate,
            onToggleSelection = onToggleSelection,
            onLongPressRow = onLongPressRow,
            onToggleGroupCollapse = onToggleGroupCollapse,
            onToggleGroupSelection = onToggleGroupSelection,
            onLongPressGroup = onLongPressGroup,
            onDragStart = ::startDrag,
            onDrag = { _, y -> updateDrag(y) },
            onDragEnd = { endDrag() },
            onDragCancel = { cancelDrag() },
            modifier = modifier
        )
        return
    }

    // ============================================================
    // ROWS / ROWS_COMPACT
    // ============================================================
    val compactHeader = viewMode == AggregateViewMode.ROWS_COMPACT

    val horizontalScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val verticalScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    val wCheck = 36.dp
    val wDrag = 32.dp
    val wName = 210.dp
    val wSne = 80.dp
    val wRes1 = 100.dp
    val wRem1 = 90.dp
    val wYears = 80.dp
    val wIssue = 100.dp
    val wRem2 = 90.dp
    val wInst1 = 100.dp
    val wInst2 = 100.dp

    val totalWidth: Dp = wName + wSne + wRes1 + wRem1 + wYears + wIssue + wRem2 + wInst1 + wInst2 +
            (if (selectionMode) wCheck + wDrag else 0.dp)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
        ) {
            Column {
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
                    if (selectionMode) Box(modifier = Modifier.width(wDrag))
                }
                HorizontalDivider()

                val indexed = resources.mapIndexed { index, item -> index to item }

                val pinnedGroup = groups.firstOrNull { it.id == PINNED_GROUP_ID }
                val otherPinnedGroups = groups
                    .filter { it.id != PINNED_GROUP_ID && it.pinned }
                    .sortedBy { it.order }
                val regularGroups = groups
                    .filter { it.id != PINNED_GROUP_ID && !it.pinned }
                    .sortedBy { it.order }

                val rootItems = indexed.filter { (_, item) -> item.groupId.isEmpty() }
                val pinnedRootItems = rootItems.filter { (_, item) -> item.pinned }
                    .sortedBy { (_, item) -> item.order }
                val regularRootItems = rootItems.filter { (_, item) -> !item.pinned }
                    .sortedBy { (_, item) -> item.order }

                if (pinnedGroup != null) {
                    RenderGroup(
                        group = pinnedGroup,
                        items = indexed.filter { (_, item) -> item.groupId == PINNED_GROUP_ID },
                        searchQuery = searchQuery,
                        sortMode = sortMode,
                        selectionMode = selectionMode,
                        editingIndex = editingIndex,
                        selectedIndices = selectedIndices,
                        selectedGroup = pinnedGroup.id in selectedGroupIds,
                        totalWidth = totalWidth,
                        compactHeader = compactHeader,
                        draggingIndex = draggingIndex,
                        dragOffsetY = dragOffsetY,
                        targetInsertIndex = targetInsertIndex,
                        targetInsertAfter = targetInsertAfter,
                        onUpdate = onUpdate,
                        onToggleSelection = onToggleSelection,
                        onLongPressRow = onLongPressRow,
                        onToggleGroupCollapse = onToggleGroupCollapse,
                        onToggleGroupSelection = onToggleGroupSelection,
                        onLongPressGroup = onLongPressGroup,
                        onDragStart = ::startDrag,
                        onDrag = { _, y -> updateDrag(y) },
                        onDragEnd = { endDrag() },
                        onDragCancel = { cancelDrag() },
                        wCheck = wCheck, wDrag = wDrag,
                        wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                        wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                        wInst1 = wInst1, wInst2 = wInst2
                    )
                }

                otherPinnedGroups.forEach { group ->
                    RenderGroup(
                        group = group,
                        items = indexed.filter { (_, item) -> item.groupId == group.id },
                        searchQuery = searchQuery,
                        sortMode = sortMode,
                        selectionMode = selectionMode,
                        editingIndex = editingIndex,
                        selectedIndices = selectedIndices,
                        selectedGroup = group.id in selectedGroupIds,
                        totalWidth = totalWidth,
                        compactHeader = compactHeader,
                        draggingIndex = draggingIndex,
                        dragOffsetY = dragOffsetY,
                        targetInsertIndex = targetInsertIndex,
                        targetInsertAfter = targetInsertAfter,
                        onUpdate = onUpdate,
                        onToggleSelection = onToggleSelection,
                        onLongPressRow = onLongPressRow,
                        onToggleGroupCollapse = onToggleGroupCollapse,
                        onToggleGroupSelection = onToggleGroupSelection,
                        onLongPressGroup = onLongPressGroup,
                        onDragStart = ::startDrag,
                        onDrag = { _, y -> updateDrag(y) },
                        onDragEnd = { endDrag() },
                        onDragCancel = { cancelDrag() },
                        wCheck = wCheck, wDrag = wDrag,
                        wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                        wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                        wInst1 = wInst1, wInst2 = wInst2
                    )
                }

                val rootContextIndices = (pinnedRootItems + regularRootItems).map { it.first }
                val sortedPinnedRoot = if (editingIndex != null || sortMode == AggregateSortMode.NONE) {
                    pinnedRootItems.map { it.second }
                } else {
                    AggregatesSort.apply(pinnedRootItems.map { it.second }, sortMode)
                }
                sortedPinnedRoot.forEach { item ->
                    val idx = indexed.first { it.second === item }.first
                    if (AggregatesSort.filter(listOf(item), searchQuery).isNotEmpty()) {
                        if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
                            InsertionIndicator(width = totalWidth)
                        }
                        RenderRow(
                            index = idx, item = item, isFirst = item.name == "Фюзеляж",
                            selectionMode = selectionMode,
                            selected = idx in selectedIndices,
                            editable = editingIndex == idx,
                            isDragging = draggingIndex == idx,
                            dragOffsetY = if (draggingIndex == idx) dragOffsetY else 0f,
                            onUpdate = { onUpdate(idx, it) },
                            onToggleSelection = { onToggleSelection(idx) },
                            onLongPress = { onLongPressRow(idx) },
                            onDragStart = { startIdx -> startDrag(startIdx, rootContextIndices) },
                            onDrag = { _, y -> updateDrag(y) },
                            onDragEnd = { endDrag() },
                            onDragCancel = { cancelDrag() },
                            wCheck = wCheck, wDrag = wDrag,
                            wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                            wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                            wInst1 = wInst1, wInst2 = wInst2
                        )
                        HorizontalDivider()
                        if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                            InsertionIndicator(width = totalWidth)
                        }
                    }
                }

                regularGroups.forEach { group ->
                    RenderGroup(
                        group = group,
                        items = indexed.filter { (_, item) -> item.groupId == group.id },
                        searchQuery = searchQuery,
                        sortMode = sortMode,
                        selectionMode = selectionMode,
                        editingIndex = editingIndex,
                        selectedIndices = selectedIndices,
                        selectedGroup = group.id in selectedGroupIds,
                        totalWidth = totalWidth,
                        compactHeader = compactHeader,
                        draggingIndex = draggingIndex,
                        dragOffsetY = dragOffsetY,
                        targetInsertIndex = targetInsertIndex,
                        targetInsertAfter = targetInsertAfter,
                        onUpdate = onUpdate,
                        onToggleSelection = onToggleSelection,
                        onLongPressRow = onLongPressRow,
                        onToggleGroupCollapse = onToggleGroupCollapse,
                        onToggleGroupSelection = onToggleGroupSelection,
                        onLongPressGroup = onLongPressGroup,
                        onDragStart = ::startDrag,
                        onDrag = { _, y -> updateDrag(y) },
                        onDragEnd = { endDrag() },
                        onDragCancel = { cancelDrag() },
                        wCheck = wCheck, wDrag = wDrag,
                        wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                        wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                        wInst1 = wInst1, wInst2 = wInst2
                    )
                }

                val sortedRegularRoot = if (editingIndex != null || sortMode == AggregateSortMode.NONE) {
                    regularRootItems.map { it.second }
                } else {
                    AggregatesSort.apply(regularRootItems.map { it.second }, sortMode)
                }
                sortedRegularRoot.forEach { item ->
                    val idx = indexed.first { it.second === item }.first
                    if (AggregatesSort.filter(listOf(item), searchQuery).isNotEmpty()) {
                        if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
                            InsertionIndicator(width = totalWidth)
                        }
                        RenderRow(
                            index = idx, item = item, isFirst = item.name == "Фюзеляж",
                            selectionMode = selectionMode,
                            selected = idx in selectedIndices,
                            editable = editingIndex == idx,
                            isDragging = draggingIndex == idx,
                            dragOffsetY = if (draggingIndex == idx) dragOffsetY else 0f,
                            onUpdate = { onUpdate(idx, it) },
                            onToggleSelection = { onToggleSelection(idx) },
                            onLongPress = { onLongPressRow(idx) },
                            onDragStart = { startIdx -> startDrag(startIdx, rootContextIndices) },
                            onDrag = { _, y -> updateDrag(y) },
                            onDragEnd = { endDrag() },
                            onDragCancel = { cancelDrag() },
                            wCheck = wCheck, wDrag = wDrag,
                            wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
                            wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
                            wInst1 = wInst1, wInst2 = wInst2
                        )
                        HorizontalDivider()
                        if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
                            InsertionIndicator(width = totalWidth)
                        }
                    }
                }

                Box(modifier = Modifier.padding(bottom = 80.dp))
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
private fun RenderGroup(
    group: AggregateGroup,
    items: List<Pair<Int, ResourceItem>>,
    searchQuery: String,
    sortMode: AggregateSortMode,
    selectionMode: Boolean,
    editingIndex: Int?,
    selectedIndices: Set<Int>,
    selectedGroup: Boolean,
    totalWidth: Dp,
    compactHeader: Boolean,
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
    wCheck: Dp, wDrag: Dp,
    wName: Dp, wSne: Dp, wRes1: Dp, wRem1: Dp,
    wYears: Dp, wIssue: Dp, wRem2: Dp, wInst1: Dp, wInst2: Dp
) {
    val filtered = if (searchQuery.isBlank()) items
    else items.filter { (_, item) ->
        AggregatesSort.filter(listOf(item), searchQuery).isNotEmpty()
    }
    if (filtered.isEmpty()) return

    val sortedItems = when {
        editingIndex != null -> filtered.sortedWith(
            compareByDescending<Pair<Int, ResourceItem>> { it.second.pinned }
                .thenBy { it.second.order }
        )
        sortMode == AggregateSortMode.NONE -> filtered.sortedWith(
            compareByDescending<Pair<Int, ResourceItem>> { it.second.pinned }
                .thenBy { it.second.order }
        )
        else -> {
            val sortedByMode = AggregatesSort.apply(filtered.map { it.second }, sortMode)
            val mapByIdentity = filtered.associateBy { it.second }
            sortedByMode.mapNotNull { mapByIdentity[it] }
        }
    }

    AggregateGroupHeader(
        group = group,
        items = sortedItems.map { it.second },
        totalWidth = totalWidth,
        selectionMode = selectionMode,
        selected = selectedGroup,
        compact = compactHeader,
        onToggleCollapse = { onToggleGroupCollapse(group.id) },
        onToggleSelection = { onToggleGroupSelection(group.id) },
        onLongPress = { onLongPressGroup(group.id) },
        wCheck = wCheck
    )
    HorizontalDivider()

    if (group.collapsed) return

    val contextIndices = sortedItems.map { it.first }

    sortedItems.forEach { (idx, item) ->
        if (targetInsertIndex == idx && !targetInsertAfter && draggingIndex != idx) {
            InsertionIndicator(width = totalWidth)
        }
        RenderRow(
            index = idx, item = item, isFirst = item.name == "Фюзеляж",
            selectionMode = selectionMode,
            selected = idx in selectedIndices,
            editable = editingIndex == idx,
            isDragging = draggingIndex == idx,
            dragOffsetY = if (draggingIndex == idx) dragOffsetY else 0f,
            onUpdate = { onUpdate(idx, it) },
            onToggleSelection = { onToggleSelection(idx) },
            onLongPress = { onLongPressRow(idx) },
            onDragStart = { startIdx -> onDragStart(startIdx, contextIndices) },
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            wCheck = wCheck, wDrag = wDrag,
            wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
            wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
            wInst1 = wInst1, wInst2 = wInst2
        )
        HorizontalDivider()
        if (targetInsertIndex == idx && targetInsertAfter && draggingIndex != idx) {
            InsertionIndicator(width = totalWidth)
        }
    }
}

@Composable
private fun RenderRow(
    index: Int, item: ResourceItem, isFirst: Boolean,
    selectionMode: Boolean, selected: Boolean, editable: Boolean,
    isDragging: Boolean, dragOffsetY: Float,
    onUpdate: (ResourceItem) -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Int, Float) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit,
    wCheck: Dp, wDrag: Dp,
    wName: Dp, wSne: Dp, wRes1: Dp, wRem1: Dp,
    wYears: Dp, wIssue: Dp, wRem2: Dp, wInst1: Dp, wInst2: Dp
) {
    AggregateRow(
        item = item,
        isFirst = isFirst,
        selectionMode = selectionMode,
        selected = selected,
        editable = editable,
        rowIndex = index,
        isDragging = isDragging,
        dragOffsetY = dragOffsetY,
        onUpdate = onUpdate,
        onToggleSelection = onToggleSelection,
        onLongPress = onLongPress,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        wCheck = wCheck, wDrag = wDrag,
        wName = wName, wSne = wSne, wRes1 = wRes1, wRem1 = wRem1,
        wYears = wYears, wIssue = wIssue, wRem2 = wRem2,
        wInst1 = wInst1, wInst2 = wInst2
    )
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
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
