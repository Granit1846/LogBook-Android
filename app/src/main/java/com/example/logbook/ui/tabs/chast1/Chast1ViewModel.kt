package com.example.logbook.ui.tabs.chast1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.Chast1Entity
import com.example.logbook.data.local.entity.Chast1StartEntity
import com.example.logbook.data.local.recalc.Chast1Recalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Chast1ViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val records: StateFlow<List<Chast1Entity>> = database.chast1Dao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val startRecord: StateFlow<Chast1StartEntity?> = database.chast1StartDao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var recalcJob: Job? = null

    fun ensureStartExists() {
        viewModelScope.launch {
            val existing = database.chast1StartDao().getByProfile(profileId)
            if (existing == null) {
                database.chast1StartDao().insert(
                    Chast1StartEntity(profileId = profileId)
                )
            }
        }
    }

    suspend fun deleteDay(date: String) {
        recalcJob?.cancelAndJoin()
        DayWriter(database).removeDateFromAllFormulars(profileId, date)
    }

    fun updateStart(start: Chast1StartEntity) {
        viewModelScope.launch {
            database.chast1StartDao().update(start)
        }
        // Debounce 500 мс — как в C#-оригинале
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            delay(500)
            Chast1Recalc(database, profileId).recalculateAll()
        }
    }
}