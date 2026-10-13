package com.example.logbook.ui.tabs.resources.aggregatesOnBoard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.tabs.resources.AggregatesOnBoardContent
import com.example.logbook.ui.tabs.resources.HistoryDialog
import com.example.logbook.ui.tabs.resources.ReplaceAggregateDialog
import com.example.logbook.ui.tabs.resources.ResourcesViewModel
import com.example.logbook.ui.tabs.resources.AggregateOperationFeedback
import com.example.logbook.ui.tabs.resources.SetupFlowDialog
import com.example.logbook.ui.util.CompactTopBar

@Composable
fun AggregatesOnBoardScreen(
    profileId: Long,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val viewModel: ResourcesViewModel = viewModel(
        key = "resources_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var replaceType by remember { mutableStateOf<String?>(null) }
    var showSetupFlow by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    if (!showSetupFlow) AggregateOperationFeedback(viewModel)

    AggregatesOnBoardContent(
        assignments = assignments,
        onReplace = { replaceType = it },
        onSetup = { showSetupFlow = true },
        onHistory = { showHistory = true },
        onDismiss = onBack
    )

    if (showSetupFlow) {
        SetupFlowDialog(
            viewModel = viewModel,
            onDismiss = { showSetupFlow = false }
        )
    }

    replaceType?.let { type ->
        ReplaceAggregateDialog(
            type = type,
            currentNumber = assignments[type] ?: "",
            onConfirm = { newNumber, reason, installationDate ->
                viewModel.replaceAggregate(type, newNumber, reason, installationDate) { replaceType = null }
            },
            onDismiss = { replaceType = null }
        )
    }

    if (showHistory) {
        HistoryDialog(
            history = history,
            onDismiss = { showHistory = false }
        )
    }
}
