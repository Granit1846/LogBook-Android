package com.example.logbook.ui.tabs.resources.oils

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.logbook.data.model.OilItem
import com.example.logbook.data.model.calcRemaining1
import com.example.logbook.data.model.calcRemaining2
import com.example.logbook.ui.util.minutesToTime

import com.example.logbook.data.model.ResourceWarnings
import com.example.logbook.ui.tabs.resources.ResourceRemainingField
import com.example.logbook.ui.tabs.resources.LocalResourceToday

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OilRow(
    item: OilItem,
    fuselageSne: Int,
    selectionMode: Boolean,
    selected: Boolean,
    editable: Boolean,
    rowIndex: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onUpdate: (OilItem) -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: (index: Int) -> Unit,
    onDrag: (index: Int, dragAmountY: Float) -> Unit,
    onDragEnd: (index: Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,

    compactDragVisual: Boolean = false,

    wDrag: Dp = 32.dp,
    wName: Dp = 120.dp,
    wByHours: Dp = 90.dp,
    wByYears: Dp = 90.dp,
    wRem1: Dp = 80.dp,
    wRem2: Dp = 80.dp,
    wCoef: Dp = 90.dp,
    wTerm: Dp = 70.dp
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
        // Название
        if (editable) {
            OilTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                width = wName
            )
        } else {
            OilReadonlyField(item.name, wName)
        }

        // СНЭ при заливке
        if (editable) {
            OilTimeField(
                value = item.byHours,
                onValueChange = { onUpdate(item.copy(byHours = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(byHours = value, emptyFields = if (blank) item.emptyFields + "byHours" else item.emptyFields - "byHours")) },
                width = wByHours
            )
        } else {
            OilReadonlyField(minutesToTime(item.byHours), wByHours)
        }

        // Дата заливки
        if (editable) {
            OilDateField(
                value = item.byYears,
                onValueChange = { onUpdate(item.copy(byYears = it)) },
                width = wByYears
            )
        } else {
            OilReadonlyField(item.byYears, wByYears)
        }

        // Остаток чч:мм
        val rem1 = item.calcRemaining1(fuselageSne)
        ResourceRemainingField(if (item.coefficient <= 0) "—" else if (rem1 == 0) "0:00" else minutesToTime(rem1), wRem1, ResourceWarnings.oilHours(item, fuselageSne))

        // Остаток гг:мм
        ResourceRemainingField(item.calcRemaining2(today), wRem2, ResourceWarnings.calendar(ResourceWarnings.oilExpiry(item), today, oil = true))

        // Ресурс в часах
        if (editable) {
            OilTimeField(
                value = item.coefficient,
                onValueChange = { onUpdate(item.copy(coefficient = it)) },
                onValueChangeDetailed = { value, blank -> onUpdate(item.copy(coefficient = value, emptyFields = if (blank) item.emptyFields + "coefficient" else item.emptyFields - "coefficient")) },
                width = wCoef
            )
        } else {
            OilReadonlyField(minutesToTime(item.coefficient), wCoef)
        }

        // Ресурс в годах
        if (editable) {
            OilTermField(
                value = item.term,
                onValueChange = { onUpdate(item.copy(term = it)) },
                width = wTerm
            )
        } else {
            OilReadonlyField(item.term, wTerm)
        }

        // Drag-handle
        if (selectionMode) {
            OilDragHandleCell(
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
private fun OilDragHandleCell(
    item: OilItem,
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