package com.example.logbook.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileListViewModel(
    private val database: AppDatabase
) : ViewModel() {

    // Список профилей как StateFlow — UI подписывается и автоматически обновляется
    val profiles: StateFlow<List<ProfileEntity>> = database.profileDao()
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addProfile(name: String, boardNumber: String) {
        viewModelScope.launch {
            database.profileDao().insert(
                ProfileEntity(
                    name = name.trim(),
                    boardNumber = boardNumber.trim()
                )
            )
        }
    }
    fun addProfileWithCallback(
        name: String,
        boardNumber: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newId = database.profileDao().insert(
                ProfileEntity(
                    name = name.trim(),
                    boardNumber = boardNumber.trim()
                )
            )
            onCreated(newId)
        }
    }
    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            database.profileDao().delete(profile)
        }
    }

    fun touchProfile(profileId: Long) {
        viewModelScope.launch {
            database.profileDao().touch(profileId)
        }
    }
}

// Ручная фабрика — создаёт ViewModel с нужным DAO
class ProfileListViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileListViewModel::class.java)) {
            val db = AppDatabase.getInstance(context.applicationContext)
            return ProfileListViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}