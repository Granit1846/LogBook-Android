package com.example.logbook.ui.tabs.flights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.SnkEntity
import com.example.logbook.data.local.relation.DayDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class FlightsViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    data class LoadedPage(val date: String, val details: DayDetails?)

    val loadedPage: StateFlow<LoadedPage?> = _selectedDate
        .flatMapLatest { date ->
            database.dayDao().observeDetailsByDate(profileId, date)
                .map { LoadedPage(date, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _currentDay = MutableStateFlow<DayEntity?>(null)
    val currentDay: StateFlow<DayEntity?> = _currentDay.asStateFlow()

    val launches: StateFlow<List<LaunchEntity>> = _currentDay
        .flatMapLatest { day ->
            if (day == null) flowOf(emptyList())
            else database.launchDao().observeByDay(day.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val snk: StateFlow<SnkEntity?> = _currentDay
        .flatMapLatest { day ->
            if (day == null) flowOf(null)
            else database.snkDao().observeByDay(day.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ============================================================
    // Состояния для «Закончить день»
    // ============================================================

    private val _finishDayErrors = MutableStateFlow<List<DayWriter.LaunchError>>(emptyList())
    val finishDayErrors: StateFlow<List<DayWriter.LaunchError>> = _finishDayErrors.asStateFlow()

    private val _lastFinishWritten = MutableStateFlow<Set<DayWriter.FormularType>?>(null)
    val lastFinishWritten: StateFlow<Set<DayWriter.FormularType>?> = _lastFinishWritten.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    private val _triggerAutoSync = MutableStateFlow(false)
    val triggerAutoSync: StateFlow<Boolean> = _triggerAutoSync.asStateFlow()

    fun consumeAutoSync() {
        _triggerAutoSync.value = false
    }

    init {
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date ->
                database.dayDao().observeByDate(profileId, date)
            }.collect { day ->
                _currentDay.value = day
            }
        }
        loadDayForDate(_selectedDate.value)
    }

    private suspend fun getOrCreateDay(date: String): DayEntity = database.withTransaction {
        database.dayDao().getByDate(profileId, date) ?: run {
            val id = database.dayDao().insert(DayEntity(profileId = profileId, date = date))
            requireNotNull(database.dayDao().getById(id))
        }
    }

    private fun loadDayForDate(date: String) {
        viewModelScope.launch {
            val day = database.dayDao().getByDate(profileId, date)
            if (_selectedDate.value == date) _currentDay.value = day
        }
    }

    fun setDate(date: String) {
        _selectedDate.value = date
        loadDayForDate(date)
    }

    fun shiftDay(deltaDays: Int) {
        val newDate = shiftDate(_selectedDate.value, deltaDays)
        setDate(newDate)
    }

    fun updateCrew(crew: String) {
        val date = _selectedDate.value
        viewModelScope.launch {
            database.withTransaction {
                val day = getOrCreateDay(date)
                database.dayDao().update(day.copy(crew = crew))
            }
        }
    }

    fun addLaunch() {
        val date = _selectedDate.value
        viewModelScope.launch {
            database.withTransaction {
                val day = getOrCreateDay(date)
                val maxOrder = database.launchDao().maxOrder(day.id) ?: -1
                database.launchDao().insert(
                    LaunchEntity(dayId = day.id, order = maxOrder + 1)
                )
            }
        }
    }

    fun updateLaunch(launch: LaunchEntity) {
        viewModelScope.launch {
            val updated = launch.copy(
                zapravkaKg = if (launch.zapravkaLiters > 0 && launch.zapravkaDensity > 0)
                    launch.zapravkaLiters * launch.zapravkaDensity
                else 0.0
            )
            database.launchDao().update(updated)
        }
    }

    fun deleteLaunch(launch: LaunchEntity) {
        viewModelScope.launch {
            database.launchDao().delete(launch)
        }
    }

    fun ensureSnkExists() {
        val day = _currentDay.value ?: return
        viewModelScope.launch {
            val existing = database.snkDao().getByDay(day.id)
            if (existing == null) {
                database.snkDao().insert(SnkEntity(dayId = day.id))
            }
        }
    }

    fun updateSnk(updated: SnkEntity) {
        val date = _selectedDate.value
        viewModelScope.launch {
            database.withTransaction {
                val day = getOrCreateDay(date)
                val existing = database.snkDao().getByDay(day.id)
                if (existing == null) {
                    database.snkDao().insert(updated.copy(id = 0, dayId = day.id))
                } else {
                    database.snkDao().update(updated.copy(id = existing.id, dayId = day.id))
                }
            }
        }
    }

    // ============================================================
    // ЗАКОНЧИТЬ ДЕНЬ
    // ============================================================

    /**
     * Закрыть день: записать данные из «Полётов» во все 5 формуляров.
     *
     * Сначала сохраняет crew в DayEntity (если изменилось), затем вызывает
     * DayWriter.writeDay() — одна транзакция, полный пересчёт формуляров.
     *
     * Ошибки (tp с заполненными земля/воздух/ВСУ) возвращаются в _finishDayErrors
     * для показа пользователю. Запись при этом всё равно выполняется.
     */
    fun finishDay() {
        val day = _currentDay.value ?: return
        if (_isFinishing.value) return

        _isFinishing.value = true
        viewModelScope.launch {
            try {
                database.withTransaction {
                    if (database.dayDao().getById(day.id) == null) return@withTransaction
                    // Подстраховка: убедиться, что все запуски/СНК сохранены в БД
                    val freshLaunches = database.launchDao().getByDay(day.id)
                    val freshSnk = database.snkDao().getByDay(day.id)
    
                    val result = DayWriter(database).writeDay(
                        profileId = profileId,
                        date = day.date,
                        launches = freshLaunches,
                        snk = freshSnk
                    )
    
                    _lastFinishWritten.value = result.writtenFormulars
                    _finishDayErrors.value = result.errors
    
                    // После успешной записи — пометить день как закрытый.
                    // Сбрасывать isClosed нужно при любом изменении данных — сейчас
                    // этого нет, но пользователь может снова нажать «Закончить день».
                    database.dayDao().setClosed(day.id, true)
                }

                // Автоматическая синхронизация после окончания дня
                val settings = database.appSettingsDao().get()
                if (settings?.syncOnFinishDay == true) {
                    _triggerAutoSync.value = true
                }
            } finally {
                _isFinishing.value = false
            }
        }
    }

    /** Очистить список ошибок (после показа диалога). */
    fun dismissFinishDayErrors() {
        _finishDayErrors.value = emptyList()
    }

    /** Очистить отметку «какие формуляры записались» (после показа). */
    fun dismissFinishWritten() {
        _lastFinishWritten.value = null
    }
}

private fun todayString(): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
}

private fun shiftDate(dateString: String, deltaDays: Int): String {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val date = format.parse(dateString) ?: return todayString()
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.add(Calendar.DAY_OF_MONTH, deltaDays)
    return format.format(calendar.time)
}
