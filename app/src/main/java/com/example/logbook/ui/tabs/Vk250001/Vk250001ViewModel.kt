package com.example.logbook.ui.tabs.vk250001

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.Vk250001Entity
import com.example.logbook.data.local.entity.Vk250001StartEntity
import com.example.logbook.data.local.recalc.Vk250001Recalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Vk250001ViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val records: StateFlow<List<Vk250001Entity>> = database.vk250001Dao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val startRecord: StateFlow<Vk250001StartEntity?> = database.vk250001StartDao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var recalcJob: Job? = null

    fun ensureStartExists() {
        viewModelScope.launch {
            val existing = database.vk250001StartDao().getByProfile(profileId)
            if (existing == null) {
                database.vk250001StartDao().insert(
                    Vk250001StartEntity(profileId = profileId)
                )
            }
        }
    }

    suspend fun deleteDay(date: String) {
        recalcJob?.cancelAndJoin()
        DayWriter(database).removeDateFromAllFormulars(profileId, date)
    }

    fun updateStart(start: Vk250001StartEntity) {
        viewModelScope.launch {
            database.vk250001StartDao().update(start)
        }
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            delay(500)
            Vk250001Recalc(database, profileId).recalculateAll()
        }
    }
}