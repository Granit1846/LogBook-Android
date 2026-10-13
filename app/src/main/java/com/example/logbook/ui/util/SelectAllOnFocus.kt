package com.example.logbook.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun Modifier.selectAllOnFocus(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
): Modifier = this.onFocusChanged { focusState ->
    if (focusState.isFocused) {
        if (value.selection != TextRange(0, value.text.length)) {
            onValueChange(value.copy(selection = TextRange(0, value.text.length)))
        }
    }
}