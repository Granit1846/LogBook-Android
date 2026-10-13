package com.example.logbook.ui.tabs.ta14

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.Ta14Entity
import com.example.logbook.data.local.entity.Ta14StartEntity
import com.example.logbook.data.local.recalc.Ta14Recalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Ta14ViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val records: StateFlow<List<Ta14Entity>> = database.ta14Dao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val startRecord: StateFlow<Ta14StartEntity?> = database.ta14StartDao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var recalcJob: Job? = null

    fun ensureStartExists() {
        viewModelScope.launch {
            val existing = database.ta14StartDao().getByProfile(profileId)
            if (existing == null) {
                database.ta14StartDao().insert(
                    Ta14StartEntity(profileId = profileId)
                )
            }
        }
    }

    suspend fun deleteDay(date: String) {
        recalcJob?.cancelAndJoin()
        DayWriter(database).removeDateFromAllFormulars(profileId, date)
    }

    fun updateStart(start: Ta14StartEntity) {
        viewModelScope.launch {
            database.ta14StartDao().update(start)
        }
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            delay(500)
            Ta14Recalc(database, profileId).recalculateAll()
        }
    }
}