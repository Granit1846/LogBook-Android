package com.example.logbook.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val database: AppDatabase
) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity?> = database.appSettingsDao()
        .observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            val existing = database.appSettingsDao().get()
            if (existing == null) {
                database.appSettingsDao().insert(AppSettingsEntity())
            }
        }
    }

    fun setTheme(theme: String) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(theme = theme))
        }
    }

    fun setNotifications(enabled: Boolean) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(notifications = enabled))
        }
    }

    fun setLanguage(language: String) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(language = language))
            com.example.logbook.localization.I18n.changeLanguage(language)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(autoSave = enabled))
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(autoCheckUpdates = enabled))
        }
    }

    fun setSupabaseUrl(url: String) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(supabaseUrl = url))
        }
    }

    fun setSupabaseKey(key: String) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(supabaseKey = key))
        }
    }

    fun setSyncOnFinishDay(enabled: Boolean) {
        val current = settings.value ?: return
        viewModelScope.launch {
            database.appSettingsDao().update(current.copy(syncOnFinishDay = enabled))
        }
    }
}

class SettingsViewModelFactory(
    private val database: AppDatabase
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
