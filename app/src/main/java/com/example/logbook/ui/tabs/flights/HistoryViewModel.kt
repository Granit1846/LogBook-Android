package com.example.logbook.ui.tabs.flights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.relation.DayWithLaunches
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    database: AppDatabase,
    profileId: Long
) : ViewModel() {

    val days: StateFlow<List<DayWithLaunches>> = database.dayDao()
        .observeAllWithLaunches(profileId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}