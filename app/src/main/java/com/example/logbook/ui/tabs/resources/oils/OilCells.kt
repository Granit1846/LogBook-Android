package com.example.logbook.ui.tabs.resources.oils

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logbook.ui.util.expandShortYear
import androidx.compose.ui.focus.onFocusChanged
import com.example.logbook.ui.util.formatResourceDateInput
import com.example.logbook.ui.util.formatTimeInput
import com.example.logbook.ui.util.minutesToTime
import com.example.logbook.ui.util.parseTimeInput
import com.example.logbook.ui.util.bringIntoViewOnFocus
import com.example.logbook.ui.util.selectAllOnFocus

@Composable
fun OilTextField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (value != tfv.text) tfv = TextFieldValue(value, TextRange(value.length))
    }
    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = newValue
            onValueChange(newValue.text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
    )
}

@Composable
fun OilTimeField(
    value: Int,
    onValueChange: (Int) -> Unit,
    width: Dp,
    onValueChangeDetailed: ((Int, Boolean) -> Unit)? = null
) {
    val initialText = remember { minutesToTime(value) }
    var tfv by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatTimeInput(newValue.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            val parsed = parseTimeInput(formatted) ?: 0
            if (onValueChangeDetailed != null) onValueChangeDetailed(parsed, formatted.isBlank()) else onValueChange(parsed)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
            .selectAllOnFocus(tfv) { tfv = it }
    )
}

@Composable
fun OilDateField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    val shown = expandShortYear(value)
    var tfv by remember { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (value != tfv.text && shown != tfv.text) tfv = TextFieldValue(shown, TextRange(shown.length))
    }
    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            val formatted = formatResourceDateInput(newValue.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            onValueChange(formatted)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(4.dp)
            .selectAllOnFocus(tfv) { tfv = it }
            .onFocusChanged {
                // The year is completed on leaving the field, so a four-digit year can still be typed.
                if (focused && !it.isFocused) {
                    val full = expandShortYear(tfv.text)
                    if (full != tfv.text) { tfv = TextFieldValue(full, TextRange(full.length)); onValueChange(full) }
                }
                focused = it.isFocused
            }
    )
}

@Composable
fun OilReadonlyField(
    value: String,
    width: Dp
) {
    Box(
        modifier = Modifier.bringIntoViewOnFocus()
            .width(width)
            .padding(2.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

@Composable
fun OilTermField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("0,5", "1", "1,5", "2", "2,5", "3", "3,5", "4", "4,5", "5")

    Box(modifier = Modifier.width(width)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(width)
        ) {
            Text(value, fontSize = 12.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}