package com.example.logbook.ui.util

import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/** Round column boundaries, rather than adding independently rounded cell widths. */
@Composable
fun FormularRow(
    widths: List<Dp>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        require(measurables.size == widths.size) { "Each formular column must have one cell" }
        val boundaries = IntArray(widths.size + 1)
        var total = 0.dp
        widths.forEachIndexed { index, width ->
            total += width
            boundaries[index + 1] = total.roundToPx()
        }
        val cells = measurables.mapIndexed { index, measurable ->
            val width = boundaries[index + 1] - boundaries[index]
            measurable.measure(
                Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight)
            )
        }
        layout(
            constraints.constrainWidth(boundaries.last()),
            constraints.constrainHeight(cells.maxOfOrNull { it.height } ?: 0)
        ) {
            cells.forEachIndexed { index, cell -> cell.place(boundaries[index], 0) }
        }
    }
}

/** Header cells use the same rounded start/end coordinates as FormularRow. */
fun Modifier.formularCellBounds(x: Dp, y: Dp, width: Dp, height: Dp): Modifier =
    absoluteOffset(x = x, y = y).layout { measurable, _ ->
        val cellWidth = (x + width).roundToPx() - x.roundToPx()
        val cellHeight = (y + height).roundToPx() - y.roundToPx()
        val cell = measurable.measure(Constraints.fixed(cellWidth, cellHeight))
        layout(cellWidth, cellHeight) { cell.place(0, 0) }
    }
