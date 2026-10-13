package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.AggregateViewMode
import com.example.logbook.ui.util.CompactTopBar
import com.example.logbook.ui.util.ZoomControls

@Composable
fun AggregatesTopBar(
    zoomState: MutableState<Float>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortMode: AggregateSortMode,
    onSortModeChange: (AggregateSortMode) -> Unit,
    viewMode: AggregateViewMode,
    onViewModeChange: (AggregateViewMode) -> Unit,
    onBack: () -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var viewMenuOpen by remember { mutableStateOf(false) }

    if (searchActive) {
        CompactTopBar(
            title = "",
            navigationIcon = Icons.Default.Close,
            onNavigationClick = {
                onSearchQueryChange("")
                searchActive = false
            },
            actions = {
                SearchFieldInline(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.width(300.dp)
                )
            }
        )
    } else {
        CompactTopBar(
            title = tr(R.string.msg_components_8104a71),
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Поиск
                IconButton(
                    onClick = { searchActive = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = tr(R.string.msg_search_6433b52),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Сортировка
                IconButton(
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = tr(R.string.msg_sort_ed03011),
                        modifier = Modifier.size(20.dp)
                    )
                }
                SortMenu(
                    expanded = sortMenuOpen,
                    current = sortMode,
                    onSelect = { onSortModeChange(it); sortMenuOpen = false },
                    onDismiss = { sortMenuOpen = false }
                )

                // Вид
                IconButton(
                    onClick = { viewMenuOpen = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = tr(R.string.msg_view_88f1a5a),
                        modifier = Modifier.size(20.dp)
                    )
                }
                ViewMenu(
                    expanded = viewMenuOpen,
                    current = viewMode,
                    onSelect = { onViewModeChange(it); viewMenuOpen = false },
                    onDismiss = { viewMenuOpen = false }
                )
                ZoomControls(zoomState)
            }
        )
    }
}

@Composable
private fun SearchFieldInline(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tfv by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    if (value != tfv.text) {
        tfv = TextFieldValue(value, TextRange(value.length))
    }

    BasicTextField(
        value = tfv,
        onValueChange = { newValue ->
            tfv = newValue.copy(selection = TextRange(newValue.text.length))
            onValueChange(newValue.text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier,
        decorationBox = { inner ->
            if (tfv.text.isEmpty()) {
                Text(
                    text = tr(R.string.msg_search_by_name_09c8900),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            inner()
        }
    )
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: AggregateSortMode,
    onSelect: (AggregateSortMode) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortMenuItem(tr(R.string.msg_default_469631c), current == AggregateSortMode.NONE) { onSelect(AggregateSortMode.NONE) }
        SortMenuItem(tr(R.string.msg_by_remaining_life_hh_mm_3433f32), current == AggregateSortMode.BY_REMAINING_HOURS) { onSelect(AggregateSortMode.BY_REMAINING_HOURS) }
        SortMenuItem(tr(R.string.msg_by_remaining_life_yy_mm_2b8fb17), current == AggregateSortMode.BY_REMAINING_YEARS) { onSelect(AggregateSortMode.BY_REMAINING_YEARS) }
        SortMenuItem(tr(R.string.msg_by_manufacture_date_24cedba), current == AggregateSortMode.BY_ISSUE_DATE) { onSelect(AggregateSortMode.BY_ISSUE_DATE) }
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, modifier = Modifier.width(200.dp))
                if (selected) Text("✓", color = MaterialTheme.colorScheme.primary)
            }
        },
        onClick = onClick
    )
}

@Composable
private fun ViewMenu(
    expanded: Boolean,
    current: AggregateViewMode,
    onSelect: (AggregateViewMode) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortMenuItem(tr(R.string.msg_rows_230701b), current == AggregateViewMode.ROWS) { onSelect(AggregateViewMode.ROWS) }
        SortMenuItem(tr(R.string.msg_rows_compact_17fef44), current == AggregateViewMode.ROWS_COMPACT) { onSelect(AggregateViewMode.ROWS_COMPACT) }
        SortMenuItem(tr(R.string.msg_cards_a0d17d2), current == AggregateViewMode.CARDS) { onSelect(AggregateViewMode.CARDS) }
    }
}
