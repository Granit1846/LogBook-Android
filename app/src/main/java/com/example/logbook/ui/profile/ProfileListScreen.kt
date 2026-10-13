package com.example.logbook.ui.profile

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.logbook.ui.util.OutlinedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.ui.util.LocalInformationActions
import com.example.logbook.ui.util.CompactTopBar

// ============================================================
// ProfileListScreen с ViewModel
// ============================================================

@Composable
fun ProfileListScreen(
    onProfileSelected: (Long, Boolean) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ProfileListViewModel = viewModel(
        factory = ProfileListViewModelFactory(context)
    )

    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    ProfileListContent(
        profiles = profiles,
        onProfileClick = { id -> onProfileSelected(id, false) },
        onDeleteProfile = { profile -> viewModel.deleteProfile(profile) },
        onOpenSettings = onOpenSettings,
        onAddProfile = { name, board, onCreated ->
            viewModel.addProfileWithCallback(name, board) { newId ->
                onProfileSelected(newId, true)
            }
        }
    )
}

// ============================================================
// ProfileListContent — без ViewModel
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListContent(
    profiles: List<ProfileEntity>,
    onProfileClick: (Long) -> Unit,
    onDeleteProfile: (ProfileEntity) -> Unit,
    onAddProfile: (String, String, (Long) -> Unit) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<ProfileEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 4 else 2

    val information = LocalInformationActions.current
    Scaffold(
        topBar = {
            CompactTopBar(
                title = tr(R.string.msg_logbook_profiles_8e13702),
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = tr(R.string.msg_settings_7f17c7c),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = information.show,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = tr(R.string.msg_information_e2a0109),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            OutlinedFloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = tr(R.string.msg_add_profile_ca6eaca))
            }
        }
    ) { innerPadding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tr(R.string.msg_no_profiles_tap_to_create_one_d5e9382),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onClick = { onProfileClick(profile.id) },
                        onDeleteClick = { profileToDelete = profile }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, board ->
                onAddProfile(name, board) { newId ->
                    showAddDialog = false
                    onProfileClick(newId)
                }
            }
        )
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(tr(R.string.msg_delete_profile_2a82b3a)) },
            text = {
                Text(tr(R.string.msg_profile_1_s_and_all_its_data_will_be_deleted_c85fb12, profile.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(profile)
                    profileToDelete = null
                }) {
                    Text(tr(R.string.msg_delete_86ea33a))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(tr(R.string.msg_cancel_0ec753b))
                }
            }
        )
    }
}

// ============================================================
// Карточка профиля
// ============================================================

@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Иконка удаления — в правом верхнем углу
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = tr(R.string.msg_delete_86ea33a),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Контент карточки
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.name.ifBlank { tr(R.string.msg_unnamed_cdf641f) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                if (profile.boardNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(R.string.msg_aircraft_1_s_2255c25, profile.boardNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ============================================================
// Диалог добавления профиля
// ============================================================

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.msg_new_profile_fde215b)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr(R.string.msg_profile_name_cd7d7c1)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = board,
                    onValueChange = { board = it.filter { it.isDigit() } },
                    label = { Text(tr(R.string.msg_aircraft_number_bb9b8b2)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, board) },
                enabled = name.isNotBlank()
            ) {
                Text(tr(R.string.msg_create_84370a2))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.msg_cancel_0ec753b))
            }
        }
    )
}

// ============================================================
// @Preview
// ============================================================

@Preview(
    name = "Профили — пусто (portrait)",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun ProfileListPreviewEmpty() {
    MaterialTheme {
        ProfileListContent(
            profiles = emptyList(),
            onProfileClick = {},
            onDeleteProfile = {},
            onAddProfile = { _, _, _ -> }
        )
    }
}

@Preview(
    name = "Профили — заполнено (portrait)",
    showBackground = true,
    device = "id:pixel_6"
)
@Composable
private fun ProfileListPreviewFilled() {
    MaterialTheme {
        ProfileListContent(
            profiles = listOf(
                ProfileEntity(id = 1, name = "Вертолёт 1", boardNumber = "12010"),
                ProfileEntity(id = 2, name = "Вертолёт 2", boardNumber = "12008"),
                ProfileEntity(id = 3, name = "Вертолёт 3", boardNumber = "12012"),
                ProfileEntity(id = 4, name = "Вертолёт 4", boardNumber = "12015")
            ),
            onProfileClick = {},
            onDeleteProfile = {},
            onAddProfile = { _, _, _ -> }
        )
    }
}

@Preview(
    name = "Профили — заполнено (landscape)",
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=landscape"
)
@Composable
private fun ProfileListPreviewLandscape() {
    MaterialTheme {
        ProfileListContent(
            profiles = listOf(
                ProfileEntity(id = 1, name = "Вертолёт 1", boardNumber = "12010"),
                ProfileEntity(id = 2, name = "Вертолёт 2", boardNumber = "12008"),
                ProfileEntity(id = 3, name = "Вертолёт 3", boardNumber = "12012"),
                ProfileEntity(id = 4, name = "Вертолёт 4", boardNumber = "12015")
            ),
            onProfileClick = {},
            onDeleteProfile = {},
            onAddProfile = { _, _, _ -> }
        )
    }
}
