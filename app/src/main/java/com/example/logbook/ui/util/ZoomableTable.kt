package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val ZOOM_STEP = 0.2f
const val MIN_ZOOM = 0.5f
const val MAX_ZOOM = 3f

val LocalFormularZoomState = compositionLocalOf<MutableState<Float>?> { null }

@Composable
fun ZoomControls(
    scaleState: MutableState<Float>,
    minScale: Float = MIN_ZOOM,
    maxScale: Float = MAX_ZOOM
) {
    Row {
        IconButton(
            onClick = { scaleState.value = (scaleState.value - ZOOM_STEP).coerceAtLeast(minScale) },
            enabled = scaleState.value > minScale,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ZoomOut, contentDescription = tr(R.string.msg_zoom_out_bf9ec49), modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = { scaleState.value = (scaleState.value + ZOOM_STEP).coerceAtMost(maxScale) },
            enabled = scaleState.value < maxScale,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.ZoomIn, contentDescription = tr(R.string.msg_zoom_in_b618f47), modifier = Modifier.size(20.dp))
        }
    }
}

private fun Modifier.pinchZoom(
    scaleState: MutableState<Float>,
    minScale: Float,
    maxScale: Float
): Modifier = pointerInput(scaleState, minScale, maxScale) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var transforming = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                transforming = true
                scaleState.value = (scaleState.value * event.calculateZoom()).coerceIn(minScale, maxScale)
            }
            if (transforming) event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
fun ZoomableDensityContent(
    scaleState: MutableState<Float>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    Box(modifier.pinchZoom(scaleState, MIN_ZOOM, MAX_ZOOM)) {
        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density * scaleState.value, baseDensity.fontScale)
        ) {
            content()
        }
    }
}

@Composable
fun ZoomableTable(
    minScale: Float = MIN_ZOOM,
    maxScale: Float = MAX_ZOOM,
    initialScale: Float = 1f,
    content: @Composable () -> Unit
) {
    require(minScale > 0f && maxScale >= minScale)
    val fallbackState = rememberSaveable { mutableStateOf(initialScale.coerceIn(minScale, maxScale)) }
    val scaleState = LocalFormularZoomState.current ?: fallbackState
    val scale = scaleState.value
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pinchZoom(scaleState, minScale, maxScale)
            .horizontalScroll(horizontalScroll)
            .verticalScroll(verticalScroll)
    ) {
            Box(
                Modifier.layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    layout(
                        (placeable.width * scale).roundToInt(),
                        (placeable.height * scale).roundToInt()
                    ) {
                        placeable.placeWithLayer(0, 0) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                }
            ) {
                content()
            }
    }
}
