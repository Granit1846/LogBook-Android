package com.example.logbook.ui.util

import androidx.compose.foundation.border
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = FloatingActionButtonDefaults.shape
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape),
        shape = shape,
        content = content
    )
}
