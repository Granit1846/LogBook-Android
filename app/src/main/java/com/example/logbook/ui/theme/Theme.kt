package com.example.logbook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ============================================================
// Тема «Light» (существующая)
// ============================================================
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ============================================================
// Тема «Dark» (стандартная Material3 dark)
// ============================================================
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// ============================================================
// Тема «Nexus» — тёмно-серая с оранжевым акцентом
// (на основе внутренних палитр Nexus Mods)
// ============================================================
private val NexusColorScheme = darkColorScheme(
    // Primary — оранжевый акцент Nexus
    primary = Color(0xFFF97316),             // Orange 500
    onPrimary = Color(0xFF1D1D21),
    primaryContainer = Color(0xFF7C2D12),    // Orange 900 (тёмный)
    onPrimaryContainer = Color(0xFFFFEDD5),  // Orange 100

    secondary = Color(0xFFFB923C),           // Orange 400
    onSecondary = Color(0xFF1D1D21),
    secondaryContainer = Color(0xFF431407),  // Orange 950
    onSecondaryContainer = Color(0xFFFFF7ED),

    tertiary = Color(0xFFFDBA74),            // Orange 300
    onTertiary = Color(0xFF1D1D21),
    tertiaryContainer = Color(0xFF431407),
    onTertiaryContainer = Color(0xFFFFF7ED),

    // Background / Surface — Zinc-палитра Nexus
    background = Color(0xFF0F0F10),           // Zinc 950
    onBackground = Color(0xFFE4E4E7),         // Zinc 200
    surface = Color(0xFF1D1D21),              // Zinc 900
    onSurface = Color(0xFFE4E4E7),            // Zinc 200
    surfaceVariant = Color(0xFF29292E),       // Zinc 800
    onSurfaceVariant = Color(0xFFA1A1AA),     // Zinc 400
    surfaceContainer = Color(0xFF29292E),     // Zinc 800
    surfaceContainerHigh = Color(0xFF3E3E47), // Zinc 700

    outline = Color(0xFF3E3E47),              // Zinc 700
    outlineVariant = Color(0xFF29292E),       // Zinc 800

    error = Color(0xFFEF4444),
    onError = Color(0xFF1D1D21),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

// ============================================================
// LogBookTheme — принимает mode: "System" / "Light" / "Dark" / "Nexus"
// ============================================================
@Composable
fun LogBookTheme(
    themeMode: String = "System",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        "Light" -> LightColorScheme
        "Dark" -> DarkColorScheme
        "Nexus" -> NexusColorScheme
        "System" -> {
            if (systemDark) DarkColorScheme else LightColorScheme
        }
        else -> {
            // Fallback — system
            if (systemDark) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}