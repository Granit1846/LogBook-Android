package com.example.logbook.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.AppSettingsEntity
import com.example.logbook.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val profile: StateFlow<ProfileEntity?> = database.profileDao()
        .observeById(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val appSettings: StateFlow<AppSettingsEntity?> = database.appSettingsDao()
        .observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun setLastOpenedTab(tab: Int) {
        viewModelScope.launch {
            val p = database.profileDao().getById(profileId) ?: return@launch
            if (p.selectedTab != tab) {
                database.profileDao().update(
                    p.copy(selectedTab = tab, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    // ============================================================
    // Zoom формуляров
    // ============================================================

    fun setChast1Zoom(value: Float) = updateSettings { it.copy(chast1Zoom = value) }
    fun setVr80Zoom(value: Float) = updateSettings { it.copy(vr80Zoom = value) }
    fun setTa14Zoom(value: Float) = updateSettings { it.copy(ta14Zoom = value) }
    fun setVk01Zoom(value: Float) = updateSettings { it.copy(vk01Zoom = value) }
    fun setVk02Zoom(value: Float) = updateSettings { it.copy(vk02Zoom = value) }

    private fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            val current = database.appSettingsDao().get() ?: AppSettingsEntity()
            database.appSettingsDao().insert(transform(current))
        }
    }
}