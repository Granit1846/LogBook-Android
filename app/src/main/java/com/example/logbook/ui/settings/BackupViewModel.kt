package com.example.logbook.ui.settings

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.backup.BackupManager
import com.example.logbook.data.backup.BackupFolder
import com.example.logbook.data.local.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val prepared: BackupManager.PreparedBackup? = null,
    val restored: Boolean = false,
    val hasSafetyCopy: Boolean = false
)

class BackupViewModel(context: Context) : ViewModel() {
    private val context = context.applicationContext
    private val manager = BackupManager(this.context, AppDatabase.getInstance(this.context))
    private val folder = BackupFolder(this.context)
    private val _state = MutableStateFlow(BackupUiState(hasSafetyCopy = manager.hasSafetyCopy()))
    val state = _state.asStateFlow()

    fun hasFolderAccess(): Boolean = folder.hasAccess()
    fun restoreInitialUri(): Uri = folder.initialUri()

    fun selectFolderAndExport(uri: Uri, safetyCopy: Boolean) = operation {
        folder.select(uri)
        saveToFolder(safetyCopy)
    }

    fun export(safetyCopy: Boolean = false) = operation { saveToFolder(safetyCopy) }

    private suspend fun saveToFolder(safetyCopy: Boolean) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val name = if (safetyCopy) "LogBook_before_restore_$timestamp.json" else "LogBook_$timestamp.json"
        folder.save(name) { output ->
            if (safetyCopy) manager.exportSafetyCopy(output) else manager.export(output)
        }
        _state.value = BackupUiState(message = tr(R.string.msg_backup_saved_1_s_2_s_958d351, BackupFolder.PATH, name),
            hasSafetyCopy = manager.hasSafetyCopy())
    }

    fun prepare(uri: Uri) = operation {
        val prepared = context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input) { tr(R.string.msg_could_not_open_the_file_651ac1a) }
            manager.prepare(input)
        }
        _state.value = BackupUiState(prepared = prepared, hasSafetyCopy = manager.hasSafetyCopy())
    }

    fun restore() {
        val prepared = _state.value.prepared ?: return
        operation {
            manager.restore(prepared)
            _state.value = BackupUiState(restored = true, hasSafetyCopy = true,
                message = tr(R.string.msg_data_restored_open_the_desired_profile_in_the__395db2c))
        }
    }

    fun dismiss() {
        if (!_state.value.busy) _state.value = BackupUiState(hasSafetyCopy = manager.hasSafetyCopy())
    }

    private fun operation(block: suspend () -> Unit) {
        if (_state.value.busy || _state.value.restored) return
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.value = BackupUiState(
                    message = e.message ?: tr(R.string.msg_could_not_complete_the_backup_operation_e62a2a2),
                    hasSafetyCopy = manager.hasSafetyCopy())
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(context) as T
    }
}
