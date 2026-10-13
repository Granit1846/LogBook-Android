package com.example.logbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цвета формуляров (Часть 1, ВР-80, Та-14, ВК-01/02).
 *
 * Берутся из MaterialTheme.colorScheme, чтобы автоматически
 * адаптироваться к текущей теме (Light / Dark / Nexus).
 */
data class FormularColors(
    val dataRow: Color,
    val header: Color,
    val whiteRow: Color,
    val totalRow: Color,
    val grid: Color,
    val cellText: Color,
    val headerText: Color,
    val error: Color
)

@Composable
fun formularColors(): FormularColors {
    val scheme = MaterialTheme.colorScheme
    return FormularColors(
        dataRow = scheme.surface,
        header = scheme.surfaceVariant,
        whiteRow = scheme.surfaceVariant.copy(alpha = 0.6f),
        totalRow = scheme.secondaryContainer,
        grid = scheme.outline,
        cellText = scheme.onSurface,
        headerText = scheme.onSurfaceVariant,
        error = scheme.error
    )
}

/**
 * CompositionLocal для цветов формуляров.
 * Устанавливается в Chast1Tab / Vr80Tab / ... через CompositionLocalProvider.
 */
val LocalFormularColors = staticCompositionLocalOf<FormularColors> {
    error("LocalFormularColors not provided")
}