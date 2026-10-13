package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Вкладка «Ресурсы».
 *
 * Все три раздела — отдельные экраны:
 *  - Агрегаты на борту
 *  - Агрегаты
 *  - Масла
 */
@Composable
fun ResourcesTab(
    profileId: Long,
    onNavigateToAggregates: () -> Unit = {},
    onNavigateToOils: () -> Unit = {},
    onNavigateToAggregatesOnBoard: () -> Unit = {}
) {
    ResourcesTabContent(
        onAggregatesOnBoard = { onNavigateToAggregatesOnBoard() },
        onAggregates = { onNavigateToAggregates() },
        onOils = { onNavigateToOils() }
    )
}

@Composable
fun ResourcesTabContent(
    onAggregatesOnBoard: () -> Unit,
    onAggregates: () -> Unit,
    onOils: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onAggregatesOnBoard,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text(tr(R.string.msg_installed_components_0378de4), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onAggregates,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text(tr(R.string.msg_components_8104a71), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onOils,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text(tr(R.string.msg_oils_0331c66), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}