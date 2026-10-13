package com.example.logbook.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.ui.main.MainViewModel
import com.example.logbook.ui.settings.SettingsViewModel
import com.example.logbook.ui.tabs.chast1.Chast1ViewModel
import com.example.logbook.ui.tabs.flights.FlightsViewModel
import com.example.logbook.ui.tabs.flights.HistoryViewModel
import com.example.logbook.ui.tabs.vr80.Vr80ViewModel
import com.example.logbook.ui.tabs.ta14.Ta14ViewModel
import com.example.logbook.ui.tabs.vk250001.Vk250001ViewModel
import com.example.logbook.ui.tabs.vk250002.Vk250002ViewModel
import com.example.logbook.ui.tabs.resources.ResourcesViewModel

class LogbookViewModelFactory(
    private val context: Context,
    private val profileId: Long = 0L
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context.applicationContext)
        return when {
            modelClass.isAssignableFrom(com.example.logbook.printing.FormularPrintViewModel::class.java) ->
                com.example.logbook.printing.FormularPrintViewModel(context.applicationContext, db, profileId) as T

            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(db, profileId) as T

            modelClass.isAssignableFrom(Chast1ViewModel::class.java) ->
                Chast1ViewModel(db, profileId) as T

            modelClass.isAssignableFrom(FlightsViewModel::class.java) ->
                FlightsViewModel(db, profileId) as T

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(db, profileId) as T
            modelClass.isAssignableFrom(Vr80ViewModel::class.java) ->
                Vr80ViewModel(db, profileId) as T
            modelClass.isAssignableFrom(Ta14ViewModel::class.java) ->
                Ta14ViewModel(db, profileId) as T
            modelClass.isAssignableFrom(Vk250001ViewModel::class.java) ->
                Vk250001ViewModel(db, profileId) as T
            modelClass.isAssignableFrom(Vk250002ViewModel::class.java) ->
                Vk250002ViewModel(db, profileId) as T
            modelClass.isAssignableFrom(ResourcesViewModel::class.java) ->
                ResourcesViewModel(db, profileId) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(db) as T


            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}