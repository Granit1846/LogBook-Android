package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.logbook.data.model.ResourceItem
import com.example.logbook.ui.util.minutesToTime

import com.example.logbook.data.model.ResourceWarnings
import com.example.logbook.ui.tabs.resources.ResourceRemainingField
import com.example.logbook.ui.tabs.resources.LocalResourceToday

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AggregateRow(
    item: ResourceItem,
    isFirst: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    editable: Boolean,
    rowIndex: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onUpdate: (ResourceItem) -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: (index: Int) -> Unit,
    onDrag: (index: Int, dragAmountY: Float) -> Unit,
    onDragEnd: (index: Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,

    compactDragVisual: Boolean = false,

    wCheck: Dp = 36.dp,
    wDrag: Dp = 32.dp,
    wName: Dp = 210.dp,
    wSne: Dp = 80.dp,
    wRes1: Dp = 100.dp,
    wRem1: Dp = 90.dp,
    wYears: Dp = 80.dp,
    wIssue: Dp = 100.dp,
    wRem2: Dp = 90.dp,
    wInst1: Dp = 100.dp,
    wInst2: Dp = 100.dp
) {
    val today = LocalResourceToday.current
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (item.pinned) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    if (compactDragVisual) {
                        alpha = 0.4f
                    } else {
                        translationY = dragOffsetY
                        scaleX = 1.02f
                        scaleY = 1.02f
                        alpha = 0.9f
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(4.dp)
                        clip = false
                    }
                }
            }
            .background(background)
            .combinedClickable(
                onClick = {
                    if (selectionMode && !isDragging) onToggleSelection()
                },
                onLongClick = {
                    if (!selectionMode && !isDragging) onLongPress()
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editable) {
            AggregateTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                width = wName
            )
        } else {
            AggregateReadonlyFieldText(if (isFirst) com.example.logbook.localization.I18n.display(item.name) else item.name, wName)
        }

        if (editable && isFirst) {
            AggregateTimeField(
                value = item.sne,
                onValueChange = { onUpdate(item.copy(sne = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(sne = value, emptyFields = if (blank) item.emptyFields + "sne" else item.emptyFields - "sne")) },
                width = wSne
            )
        } else {
            AggregateReadonlyField(minutesToTime(item.sne), wSne)
        }

        if (editable) {
            AggregateTimeField(
                value = item.extendedTo1,
                onValueChange = { onUpdate(item.copy(extendedTo1 = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(extendedTo1 = value, emptyFields = if (blank) item.emptyFields + "extendedTo1" else item.emptyFields - "extendedTo1")) },
                width = wRes1
            )
        } else {
            AggregateReadonlyField(minutesToTime(item.extendedTo1), wRes1)
        }

        ResourceRemainingField(if (item.extendedTo1 <= 0) "—" else if (item.calcRemaining1() == 0) "0:00" else minutesToTime(item.calcRemaining1()), wRem1, ResourceWarnings.aggregateHours(item))

        if (editable) {
            AggregateTextField(
                value = item.yearsResource,
                onValueChange = { onUpdate(item.copy(yearsResource = it)) },
                width = wYears,
                keyboardType = KeyboardType.Decimal
            )
        } else {
            AggregateReadonlyFieldText(item.yearsResource, wYears)
        }

        if (editable) {
            AggregateDateField(
                value = item.issueDate,
                onValueChange = { onUpdate(item.copy(issueDate = it)) },
                width = wIssue
            )
        } else {
            AggregateReadonlyFieldText(com.example.logbook.ui.util.expandShortYear(item.issueDate), wIssue)
        }

        ResourceRemainingField(item.calcRemaining2(today), wRem2, ResourceWarnings.calendar(ResourceWarnings.aggregateExpiry(item), today))

        if (editable) {
            AggregateTimeField(
                value = item.installedAtSne1,
                onValueChange = { onUpdate(item.copy(installedAtSne1 = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(installedAtSne1 = value, emptyFields = if (blank) item.emptyFields + "installedAtSne1" else item.emptyFields - "installedAtSne1")) },
                width = wInst1
            )
        } else {
            AggregateReadonlyField(minutesToTime(item.installedAtSne1), wInst1)
        }

        if (editable) {
            AggregateTimeField(
                value = item.installedAtSne2,
                onValueChange = { onUpdate(item.copy(installedAtSne2 = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(installedAtSne2 = value, emptyFields = if (blank) item.emptyFields + "installedAtSne2" else item.emptyFields - "installedAtSne2")) },
                width = wInst2
            )
        } else {
            AggregateReadonlyField(minutesToTime(item.installedAtSne2), wInst2)
        }

        if (selectionMode) {
            DragHandleCell(
                item = item,
                rowIndex = rowIndex,
                isDragging = isDragging,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                wDrag = wDrag
            )
        }
    }
}

@Composable
private fun DragHandleCell(
    item: ResourceItem,
    rowIndex: Int,
    isDragging: Boolean,
    onDragStart: (index: Int) -> Unit,
    onDrag: (index: Int, dragAmountY: Float) -> Unit,
    onDragEnd: (index: Int) -> Unit,
    onDragCancel: () -> Unit,
    wDrag: Dp
) {
    Box(
        modifier = Modifier
            .width(wDrag)
            .pointerInput(item, rowIndex) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(rowIndex) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(rowIndex, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd(rowIndex) },
                    onDragCancel = { onDragCancel() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = tr(R.string.msg_move_54dacb0),
            tint = if (isDragging) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp)
        )
    }
}