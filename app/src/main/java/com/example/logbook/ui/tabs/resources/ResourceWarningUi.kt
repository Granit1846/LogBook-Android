package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logbook.data.model.ResourceWarning
import com.example.logbook.data.model.ResourceWarningLevel
import java.time.LocalDate
import kotlinx.coroutines.delay

val LocalResourceToday = staticCompositionLocalOf { LocalDate.now() }

@Composable
fun rememberResourceToday(): LocalDate {
    val today by produceState(LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(30_000)
        }
    }
    return today
}

@Composable
fun ResourceRemainingField(value: String, width: Dp, level: ResourceWarningLevel) {
    val background = when (level) {
        ResourceWarningLevel.WARNING -> Color(0xFFFFE082)
        ResourceWarningLevel.CRITICAL, ResourceWarningLevel.EXPIRED -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val foreground = when (level) {
        ResourceWarningLevel.WARNING -> Color(0xFF3E3000)
        ResourceWarningLevel.URGENT -> MaterialTheme.colorScheme.error
        ResourceWarningLevel.CRITICAL, ResourceWarningLevel.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(Modifier.width(width).padding(2.dp).background(background)
        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        .padding(4.dp).semantics { contentDescription = if (level == ResourceWarningLevel.NONE) value else "${level.label}: $value" },
        contentAlignment = Alignment.Center) {
        Text(com.example.logbook.localization.I18n.duration(value), color = foreground, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (level >= ResourceWarningLevel.CRITICAL) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1)
    }
}

@Composable
fun ResourceWarningsButton(warnings: List<ResourceWarning>) {
    var show by remember { mutableStateOf(false) }
    if (warnings.isEmpty()) { show = false; return }
    TextButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text(tr(R.string.msg_warnings_1_s_d6d47bc, warnings.size), color = MaterialTheme.colorScheme.error)
    }
    if (show) ResourceWarningsDialog(warnings, onDismiss = { show = false })
}

@Composable
fun ResourceWarningsDialog(warnings: List<ResourceWarning>, title: String = tr(R.string.msg_warnings_4cbde65), onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                warnings.forEach { warning ->
                    Column {
                        Text(warning.level.label, fontWeight = FontWeight.Bold)
                        Text(com.example.logbook.localization.I18n.display(warning.text))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.msg_close_4ae50d3)) } })
}

@Composable
fun ProfileEntryWarnings(profile: com.example.logbook.data.local.entity.ProfileEntity) {
    var show by androidx.compose.runtime.saveable.rememberSaveable(profile.id) { mutableStateOf(true) }
    val today = rememberResourceToday()
    val warnings = com.example.logbook.data.model.ResourceWarnings.forProfile(profile, today)
    if (show && warnings.isNotEmpty()) {
        ResourceWarningsDialog(warnings, title = tr(R.string.msg_warnings_1_s_95e7132, profile.name), onDismiss = { show = false })
    }
}
