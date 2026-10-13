package com.example.logbook.ui.tabs.vr80

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.Vr80Entity
import com.example.logbook.data.local.entity.Vr80StartEntity
import com.example.logbook.data.local.recalc.Vr80Recalc
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class Vr80ViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    val records: StateFlow<List<Vr80Entity>> = database.vr80Dao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val startRecord: StateFlow<Vr80StartEntity?> = database.vr80StartDao()
        .observeByProfile(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var recalcJob: Job? = null

    fun ensureStartExists() {
        viewModelScope.launch {
            val existing = database.vr80StartDao().getByProfile(profileId)
            if (existing == null) {
                database.vr80StartDao().insert(
                    Vr80StartEntity(profileId = profileId)
                )
            }
        }
    }

    suspend fun deleteDay(date: String) {
        recalcJob?.cancelAndJoin()
        DayWriter(database).removeDateFromAllFormulars(profileId, date)
    }

    fun updateStart(start: Vr80StartEntity) {
        viewModelScope.launch {
            database.vr80StartDao().update(start)
        }
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            delay(500)
            Vr80Recalc(database, profileId).recalculateAll()
        }
    }
}