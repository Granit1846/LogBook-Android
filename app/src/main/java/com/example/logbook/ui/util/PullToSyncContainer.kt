package com.example.logbook.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.ui.settings.SyncDialogs
import com.example.logbook.ui.settings.SyncViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.logbook.localization.I18n
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Обертка для Pull-to-Refresh:
 * При оттягивании экрана вниз сверху плавно появляется шторка со значком синхронизации.
 * При превышении порога и отпускании запускается [onSync].
 */
@Composable
fun PullToSyncContainer(
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdDp = 72.dp
    val maxPullDp = 120.dp
    val curtainHeightDp = 40.dp

    val thresholdPx = with(density) { thresholdDp.toPx() }
    val maxPullPx = with(density) { maxPullDp.toPx() }
    val curtainHeightPx = with(density) { curtainHeightDp.toPx() }

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isReleasing by remember { mutableStateOf(false) }
    val animOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerRelease() {
        if (isReleasing) return
        isReleasing = true
        val shouldSync = pullOffset >= thresholdPx
        if (shouldSync) {
            onSync()
        }
        scope.launch {
            animOffset.snapTo(pullOffset)
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                pullOffset = value
            }
            isReleasing = false
        }
    }

    val nestedScrollConnection = remember(enabled, thresholdPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || isReleasing) return Offset.Zero
                // Если шторка уже оттянута и палец движется вверх — сначала схлопываем шторку
                if (available.y < 0f && pullOffset > 0f) {
                    val newOffset = (pullOffset + available.y).coerceAtLeast(0f)
                    val consumed = newOffset - pullOffset
                    pullOffset = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || isReleasing) return Offset.Zero
                // Если тянем вниз на самом верху списка/сетки
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    val dragResistance = (1f - (pullOffset / maxPullPx).coerceIn(0f, 0.75f)) * 0.5f
                    val delta = available.y * dragResistance
                    val newOffset = (pullOffset + delta).coerceAtMost(maxPullPx)
                    pullOffset = newOffset
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset > 0f) {
                    triggerRelease()
                    return Velocity.Zero
                }
                return Velocity.Zero
            }
        }
    }

    val isTriggered = pullOffset >= thresholdPx
    val rotationAngle = (pullOffset / thresholdPx) * 360f
    val curtainY = (pullOffset * 0.8f - curtainHeightPx).coerceAtLeast(-curtainHeightPx)

    Box(
        modifier = modifier
            .clipToBounds()
            .then(if (enabled) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
            .pointerInput(enabled, isReleasing) {
                if (enabled) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val anyDown = event.changes.any { it.pressed }
                            if (!anyDown && pullOffset > 0f && !isReleasing) {
                                triggerRelease()
                            }
                        }
                    }
                }
            }
    ) {
        // Контент с легким смещением вниз при оттягивании
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = pullOffset * 0.5f
                }
        ) {
            content()
        }

        // Шторка синхронизации, появляющаяся сверху
        if (pullOffset > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, curtainY.roundToInt()) }
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                color = if (isTriggered) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotationAngle }
                            .then(if (isTriggered) Modifier.scale(1.15f) else Modifier),
                        tint = if (isTriggered) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (isTriggered) {
                            I18n.display("Отпустите для синхронизации")
                        } else {
                            I18n.display("Потяните для синхронизации")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTriggered) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/** Lets content that does not scroll by itself (a menu, an empty state) still be pulled down. */
@Composable
fun PullToSyncStatic(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = maxHeight
        Box(Modifier.verticalScroll(rememberScrollState()).height(height)) { content() }
    }
}

/**
 * Pull-to-sync for screens without their own sync UI: uses the shared sync model, reports the result
 * in a toast and shows conflict/sign-in dialogs.
 */
@Composable
fun PullToSyncScreen(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val vm: SyncViewModel = viewModel(key = "flights_sync", factory = SyncViewModel.Factory(context))
    val state by vm.state.collectAsStateWithLifecycle()
    var shownSeq by remember { mutableLongStateOf(state.syncSeq) }
    LaunchedEffect(state.syncSeq) {
        if (state.syncSeq != shownSeq && state.message.isNotBlank() && state.conflict == null) {
            Toast.makeText(context, I18n.display(state.message), Toast.LENGTH_SHORT).show()
        }
        shownSeq = state.syncSeq
    }
    PullToSyncContainer(onSync = { vm.sync() }, modifier = modifier, enabled = !state.busy, content = content)
    SyncDialogs(vm, onDownloaded = {})
}
