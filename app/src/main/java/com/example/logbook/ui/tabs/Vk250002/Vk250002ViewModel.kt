package com.example.logbook.ui.tabs.vk250002

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.Vk250002Entity
import com.example.logbook.data.local.entity.Vk250002StartEntity
import com.example.logbook.data.local.recalc.Vk250002Recalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Vk250002ViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val records: StateFlow<List<Vk250002Entity>> = database.vk250002Dao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val startRecord: StateFlow<Vk250002StartEntity?> = database.vk250002StartDao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var recalcJob: Job? = null

    fun ensureStartExists() {
        viewModelScope.launch {
            val existing = database.vk250002StartDao().getByProfile(profileId)
            if (existing == null) {
                database.vk250002StartDao().insert(
                    Vk250002StartEntity(profileId = profileId)
                )
            }
        }
    }

    suspend fun deleteDay(date: String) {
        recalcJob?.cancelAndJoin()
        DayWriter(database).removeDateFromAllFormulars(profileId, date)
    }

    fun updateStart(start: Vk250002StartEntity) {
        viewModelScope.launch {
            database.vk250002StartDao().update(start)
        }
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            delay(500)
            Vk250002Recalc(database, profileId).recalculateAll()
        }
    }
}