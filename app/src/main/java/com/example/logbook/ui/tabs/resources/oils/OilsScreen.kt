package com.example.logbook.ui.tabs.resources.oils

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.ui.LogbookViewModelFactory
import com.example.logbook.ui.tabs.resources.ResourcesViewModel
import com.example.logbook.ui.util.LockLandscape
import com.example.logbook.ui.util.ZoomableDensityContent
import com.example.logbook.ui.util.rememberPersistentZoomState

import com.example.logbook.data.model.ResourceWarnings
import com.example.logbook.ui.tabs.resources.LocalResourceToday
import com.example.logbook.ui.tabs.resources.ResourceWarningsButton
import com.example.logbook.ui.tabs.resources.rememberResourceToday

@Composable
fun OilsScreen(
    profileId: Long,
    onBack: () -> Unit
) {
    LockLandscape()

    val context = LocalContext.current
    val viewModel: ResourcesViewModel = viewModel(
        key = "resources_$profileId",
        factory = LogbookViewModelFactory(context, profileId)
    )

    val oils by viewModel.oils.collectAsStateWithLifecycle()
    val groups by viewModel.oilGroups.collectAsStateWithLifecycle()
    val sortMode by viewModel.oilSortMode.collectAsStateWithLifecycle()
    val viewMode by viewModel.oilViewMode.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val zoomState = rememberPersistentZoomState(profileId, "oils")
    var selectionState by remember { mutableStateOf(OilsSelectionState()) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showRenameGroupDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<String?>(null) }

    val today = rememberResourceToday()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

            if (selectionState.active) {
                OilsSelectionTopBar(
                    selectionState = selectionState,
                    editingIndex = editingIndex,
                    onCancel = {
                        selectionState = selectionState.clear()
                        editingIndex = null
                    },
                    onCreateGroup = { showCreateGroupDialog = true },
                    onRenameGroup = { groupId -> showRenameGroupDialog = groupId },
                    onEdit = {
                        editingIndex = selectionState.selectedIndices.firstOrNull()
                    }
                )
            } else {
                OilsTopBar(
                    zoomState = zoomState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    sortMode = sortMode,
                    onSortModeChange = { viewModel.setOilSortMode(it) },
                    viewMode = viewMode,
                    onViewModeChange = { viewModel.setOilViewMode(it) },
                    onBack = onBack
                )
            }

            if (!keyboardVisible) ResourceWarningsButton(ResourceWarnings.oils(oils, profile?.fuselageSne ?: 0, today))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 40.dp)
            ) {
                CompositionLocalProvider(LocalResourceToday provides today) {
                ZoomableDensityContent(scaleState = zoomState, modifier = Modifier.fillMaxSize()) {
                OilsTable(
                    oils = oils,
                    groups = groups,
                    fuselageSne = profile?.fuselageSne ?: 0,
                    searchQuery = searchQuery,
                    sortMode = sortMode,
                    viewMode = viewMode,
                    selectionMode = selectionState.active,
                    editingIndex = editingIndex,
                    selectedIndices = selectionState.selectedIndices,
                    selectedGroupIds = selectionState.selectedGroupIds,
                    onUpdate = { index, item -> viewModel.updateOil(index, item) },
                    onToggleSelection = { index ->
                        selectionState = selectionState.toggleIndex(index)
                        if (editingIndex != index) editingIndex = null
                    },
                    onLongPressRow = { index ->
                        selectionState = selectionState.addIndices(listOf(index))
                    },
                    onToggleGroupCollapse = { viewModel.toggleOilGroupCollapsed(it) },
                    onToggleGroupSelection = { groupId ->
                        selectionState = selectionState.toggleGroup(groupId)
                    },
                    onLongPressGroup = { groupId ->
                        selectionState = selectionState.addGroups(listOf(groupId))
                    },
                    onMove = { from, to ->
                        viewModel.moveOil(from, to)
                        selectionState = selectionState.clear()
                        editingIndex = null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                }

                }

                if (!selectionState.active) {
                    OutlinedFloatingActionButton(
                        onClick = {
                            viewModel.addOil()
                            editingIndex = oils.size
                            selectionState = selectionState.addIndices(listOf(oils.size))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 21.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = tr(R.string.msg_add_oil_b489224))
                    }
                }
            }

            if (selectionState.active && !keyboardVisible) {
                val selectedItems = selectionState.selectedIndices.mapNotNull { idx ->
                    oils.getOrNull(idx)
                }
                val allPinned = selectedItems.isNotEmpty() && selectedItems.all { it.pinned }
                val allInRoot = selectedItems.isNotEmpty() && selectedItems.all { it.groupId.isEmpty() }

                OilsSelectionBottomBar(
                    selectionState = selectionState,
                    allSelectedPinned = allPinned,
                    allSelectedInRoot = allInRoot,
                    onPin = {
                        val indices = selectionState.selectedIndices.toList()
                        viewModel.pinOils(indices)
                        selectionState = selectionState.clear()
                        editingIndex = null
                    },
                    onUnpin = {
                        val indices = selectionState.selectedIndices.toList()
                        viewModel.unpinOils(indices)
                        selectionState = selectionState.clear()
                        editingIndex = null
                    },
                    onRemoveFromGroup = {
                        val indices = selectionState.selectedIndices.toList()
                        viewModel.removeOilsFromGroup(indices)
                        selectionState = selectionState.clear()
                        editingIndex = null
                    },
                    onDelete = { showDeleteConfirm = true },
                    onDeleteGroup = { groupId ->
                        groupToDelete = groupId
                        showDeleteConfirm = true
                    },
                    onSelectAll = {
                        val allIndices = oils.indices.toSet()
                        val allGroupIds = groups.map { it.id }.toSet()
                        selectionState = selectionState
                            .addIndices(allIndices)
                            .addGroups(allGroupIds)
                    }
                )
            }
        }
    }

    // ============================================================
    // Диалоги
    // ============================================================
    if (showCreateGroupDialog) {
        CreateOilGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name ->
                val indices = selectionState.selectedIndices.toList()
                viewModel.createOilGroup(name, indices)
                selectionState = selectionState.clear()
                editingIndex = null
                showCreateGroupDialog = false
            }
        )
    }

    showRenameGroupDialog?.let { groupId ->
        val group = groups.firstOrNull { it.id == groupId }
        if (group != null) {
            RenameOilGroupDialog(
                currentName = group.name,
                onDismiss = { showRenameGroupDialog = null },
                onConfirm = { newName ->
                    viewModel.renameOilGroup(groupId, newName)
                    showRenameGroupDialog = null
                    selectionState = selectionState.clear()
                }
            )
        } else {
            showRenameGroupDialog = null
        }
    }

    if (showDeleteConfirm) {
        val isGroupDelete = groupToDelete != null && selectionState.selectedIndices.isEmpty()
        val title = if (isGroupDelete) tr(R.string.msg_dissolve_group_a348d84) else tr(R.string.msg_delete_373417a)
        val text = when {
            isGroupDelete ->
                tr(R.string.msg_group_oils_will_return_to_the_main_list_the_oi_9de7a4f)
            selectionState.selectedIndices.isNotEmpty() ->
                tr(R.string.msg_1_s_row_s_will_be_deleted_cc5bf1d, selectionState.selectedIndices.size)
            else -> tr(R.string.msg_confirm_deletion_ea2b2bb)
        }
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                groupToDelete = null
            },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    if (isGroupDelete) {
                        groupToDelete?.let { viewModel.deleteOilGroup(it, deleteItems = false) }
                    } else {
                        val indices = selectionState.selectedIndices.toList()
                        viewModel.deleteOils(indices)
                    }
                    selectionState = selectionState.clear()
                    editingIndex = null
                    showDeleteConfirm = false
                    groupToDelete = null
                }) { Text(tr(R.string.msg_delete_86ea33a)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    groupToDelete = null
                }) { Text(tr(R.string.msg_cancel_0ec753b)) }
            }
        )
    }
}
