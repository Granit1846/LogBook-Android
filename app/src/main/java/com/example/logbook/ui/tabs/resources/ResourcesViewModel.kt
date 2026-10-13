package com.example.logbook.ui.tabs.resources

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.room.withTransaction
import com.example.logbook.data.local.AggregateManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.AggregateGroup
import com.example.logbook.data.model.AggregateReplacement
import com.example.logbook.data.model.OilItem
import com.example.logbook.data.model.PINNED_GROUP_ID
import com.example.logbook.data.model.ResourceItem
import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.AggregateViewMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ResourcesViewModel(
    private val database: AppDatabase,
    private val profileId: Long
) : ViewModel() {

    private val gson = Gson()

    private val _profile = MutableStateFlow<ProfileEntity?>(null)
    val profile: StateFlow<ProfileEntity?> = _profile.asStateFlow()

    private val _resources = MutableStateFlow<List<ResourceItem>>(emptyList())
    val resources: StateFlow<List<ResourceItem>> = _resources.asStateFlow()

    private val _groups = MutableStateFlow<List<AggregateGroup>>(emptyList())
    val groups: StateFlow<List<AggregateGroup>> = _groups.asStateFlow()

    private val _oils = MutableStateFlow<List<OilItem>>(emptyList())
    val oils: StateFlow<List<OilItem>> = _oils.asStateFlow()

    private val _oilGroups = MutableStateFlow<List<AggregateGroup>>(emptyList())
    val oilGroups: StateFlow<List<AggregateGroup>> = _oilGroups.asStateFlow()

    private val _assignments = MutableStateFlow<Map<String, String>>(emptyMap())
    val assignments: StateFlow<Map<String, String>> = _assignments.asStateFlow()

    private val _history = MutableStateFlow<List<AggregateReplacement>>(emptyList())
    val history: StateFlow<List<AggregateReplacement>> = _history.asStateFlow()

    // ============================================================
    // UI-состояние
    // ============================================================

    private val _aggregateSortMode = MutableStateFlow(AggregateSortMode.NONE)
    val aggregateSortMode: StateFlow<AggregateSortMode> = _aggregateSortMode.asStateFlow()

    private val _aggregateViewMode = MutableStateFlow(AggregateViewMode.ROWS)
    val aggregateViewMode: StateFlow<AggregateViewMode> = _aggregateViewMode.asStateFlow()

    private val _oilSortMode = MutableStateFlow(AggregateSortMode.NONE)
    val oilSortMode: StateFlow<AggregateSortMode> = _oilSortMode.asStateFlow()

    private val _oilViewMode = MutableStateFlow(AggregateViewMode.ROWS)
    val oilViewMode: StateFlow<AggregateViewMode> = _oilViewMode.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            database.profileDao().observeById(profileId).collect { profile ->
                _profile.value = profile
                if (profile != null) {
                    parseFromProfile(profile)
                }
            }
        }
    }

    private fun parseFromProfile(profile: ProfileEntity) {
        _assignments.value = try {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(profile.aggregateAssignmentsJson, mapType) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }

        _resources.value = recalculateResourceSne(try {
            val listType = object : TypeToken<List<ResourceItem>>() {}.type
            gson.fromJson<List<ResourceItem>>(profile.resourcesJson, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }, profile.fuselageSne)

        _groups.value = try {
            val listType = object : TypeToken<List<AggregateGroup>>() {}.type
            gson.fromJson<List<AggregateGroup>>(profile.aggregateGroupsJson, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        _oils.value = try {
            val listType = object : TypeToken<List<OilItem>>() {}.type
            gson.fromJson<List<OilItem>>(profile.oilsJson, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        _oilGroups.value = try {
            val listType = object : TypeToken<List<AggregateGroup>>() {}.type
            gson.fromJson<List<AggregateGroup>>(profile.oilGroupsJson, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        _history.value = try {
            val listType = object : TypeToken<List<AggregateReplacement>>() {}.type
            gson.fromJson<List<AggregateReplacement>>(profile.aggregateHistoryJson, listType) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        _aggregateSortMode.value = try {
            AggregateSortMode.valueOf(profile.aggregateSortMode)
        } catch (_: Exception) { AggregateSortMode.NONE }

        _aggregateViewMode.value = try {
            AggregateViewMode.valueOf(profile.aggregateViewMode)
        } catch (_: Exception) { AggregateViewMode.ROWS }

        _oilSortMode.value = try {
            AggregateSortMode.valueOf(profile.oilSortMode)
        } catch (_: Exception) { AggregateSortMode.NONE }

        _oilViewMode.value = try {
            AggregateViewMode.valueOf(profile.oilViewMode)
        } catch (_: Exception) { AggregateViewMode.ROWS }
    }

    // ============================================================
    // ASSIGNMENTS
    // ============================================================

    fun saveAssignments(map: Map<String, String>, onSuccess: () -> Unit = {}) = aggregateOperation(onSuccess) {
        AggregateManager(database).assign(profileId, map)
    }

    fun replaceAggregate(type: String, newNumber: String, reason: String, installationDate: java.time.LocalDate = java.time.LocalDate.now(), onSuccess: () -> Unit = {}) = aggregateOperation(onSuccess) {
        AggregateManager(database).replace(profileId, type, newNumber, reason, installationDate)
    }

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()
    private var aggregateBusy = false
    fun clearOperationError() { _operationError.value = null }

    private fun aggregateOperation(onSuccess: () -> Unit, block: suspend () -> Unit) {
        if (aggregateBusy) return
        aggregateBusy = true
        viewModelScope.launch {
            try { block(); onSuccess() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _operationError.value = e.message ?: tr(R.string.msg_could_not_save_components_e871ac6) }
            finally { aggregateBusy = false }
        }
    }

    private fun mutateProfile(change: (ProfileEntity) -> ProfileEntity) {
        viewModelScope.launch {
            database.withTransaction {
                val latest = database.profileDao().getById(profileId) ?: return@withTransaction
                database.profileDao().update(change(latest).copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun saveResources() {
        val json = gson.toJson(_resources.value)
        mutateProfile { it.copy(resourcesJson = json, resourcesUpdatedAt = System.currentTimeMillis()) }
    }
    private fun saveGroups() {
        val json = gson.toJson(_groups.value)
        mutateProfile { it.copy(aggregateGroupsJson = json) }
    }
    private fun saveResourcesAndGroups() {
        val resources = gson.toJson(_resources.value)
        val groups = gson.toJson(_groups.value)
        mutateProfile { it.copy(resourcesJson = resources, aggregateGroupsJson = groups, resourcesUpdatedAt = System.currentTimeMillis()) }
    }
    private fun saveOils() {
        val json = gson.toJson(_oils.value)
        mutateProfile { it.copy(oilsJson = json, resourcesUpdatedAt = System.currentTimeMillis()) }
    }
    private fun saveOilGroups() {
        val json = gson.toJson(_oilGroups.value)
        mutateProfile { it.copy(oilGroupsJson = json) }
    }
    private fun saveOilsAndGroups() {
        val oils = gson.toJson(_oils.value)
        val groups = gson.toJson(_oilGroups.value)
        mutateProfile { it.copy(oilsJson = oils, oilGroupsJson = groups, resourcesUpdatedAt = System.currentTimeMillis()) }
    }
    // ============================================================
    // RESOURCES — пересчёт sne
    // ============================================================

    private fun recalculateResourceSne(
        list: List<ResourceItem>,
        sneFuselage: Int
    ): List<ResourceItem> {
        if (list.isEmpty()) return list
        return list.map { item ->
            if (item.name == "Фюзеляж") {
                item.copy(sne = sneFuselage)
            } else {
                val sne = sneFuselage - item.installedAtSne1 + item.installedAtSne2
                item.copy(sne = sne)
            }
        }
    }

    // ============================================================
    // RESOURCES — CRUD
    // ============================================================

    fun ensureFuselageExists() {
        viewModelScope.launch {
            val profile = database.profileDao().getById(profileId) ?: return@launch
            val sneFuselage = profile.fuselageSne
            val currentList = _resources.value

            val listWithFuselage: List<ResourceItem> = when {
                currentList.isEmpty() -> listOf(
                    ResourceItem(
                        name = "Фюзеляж",
                        sne = sneFuselage,
                        extendedTo1 = 1500 * 60,
                        yearsResource = "20"
                    )
                )
                currentList.first().name != "Фюзеляж" -> listOf(
                    ResourceItem(
                        name = "Фюзеляж",
                        sne = sneFuselage,
                        extendedTo1 = 1500 * 60,
                        yearsResource = "20"
                    )
                ) + currentList
                else -> currentList
            }

            val recalculated = recalculateResourceSne(listWithFuselage, sneFuselage)
            if (recalculated != _resources.value) {
                _resources.value = recalculated
                saveResources()
            }
        }
    }

    fun updateResource(index: Int, item: ResourceItem) {
        val current = _resources.value.toMutableList()
        if (index in current.indices) {
            current[index] = item
            val sneFuselage = _profile.value?.fuselageSne ?: 0
            _resources.value = recalculateResourceSne(current, sneFuselage)
            saveResources()
        }
    }

    fun addResource() {
        val newItem = ResourceItem(
            name = tr(R.string.msg_new_component_749b0ba),
            extendedTo1 = 1500 * 60,
            yearsResource = "20"
        )
        val current = _resources.value + newItem
        val sneFuselage = _profile.value?.fuselageSne ?: 0
        _resources.value = recalculateResourceSne(current, sneFuselage)
        saveResources()
    }

    fun removeResource(item: ResourceItem) {
        _resources.value = _resources.value - item
        saveResources()
    }

    // ============================================================
    // ГРУППЫ АГРЕГАТОВ — CRUD
    // ============================================================

    fun createGroup(name: String, itemIndices: List<Int>): String {
        val groupId = UUID.randomUUID().toString()
        val nextOrder = (_groups.value.maxOfOrNull { it.order } ?: -1) + 1
        val newGroup = AggregateGroup(
            id = groupId,
            name = name.trim().ifBlank { tr(R.string.msg_group_ae8ad7b) },
            order = nextOrder,
            pinned = false,
            collapsed = false
        )
        val current = _resources.value.toMutableList()
        val filteredIndices = itemIndices.filter { it > 0 && it < current.size }
        filteredIndices.forEach { idx ->
            current[idx] = current[idx].copy(groupId = groupId)
        }
        _groups.value = _groups.value + newGroup
        _resources.value = current
        saveResourcesAndGroups()
        return groupId
    }

    fun renameGroup(groupId: String, newName: String) {
        if (groupId == PINNED_GROUP_ID) return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        _groups.value = _groups.value.map {
            if (it.id == groupId) it.copy(name = trimmed) else it
        }
        saveGroups()
    }

    fun deleteGroup(groupId: String, deleteItems: Boolean) {
        if (groupId == PINNED_GROUP_ID) return
        val currentResources = _resources.value
        val newResources = if (deleteItems) {
            currentResources.filter { it.groupId != groupId }
        } else {
            currentResources.map {
                if (it.groupId == groupId) it.copy(groupId = "") else it
            }
        }
        _groups.value = _groups.value.filter { it.id != groupId }
        _resources.value = recalculateResourceSne(
            newResources,
            _profile.value?.fuselageSne ?: 0
        )
        saveResourcesAndGroups()
    }

    fun toggleGroupPinned(groupId: String) {
        if (groupId == PINNED_GROUP_ID) return
        _groups.value = _groups.value.map {
            if (it.id == groupId) it.copy(pinned = !it.pinned) else it
        }
        saveGroups()
    }

    fun toggleGroupCollapsed(groupId: String) {
        _groups.value = _groups.value.map {
            if (it.id == groupId) it.copy(collapsed = !it.collapsed) else it
        }
        saveGroups()
    }

    fun reorderGroups(newOrder: List<AggregateGroup>) {
        _groups.value = newOrder.mapIndexed { idx, g -> g.copy(order = idx) }
        saveGroups()
    }

    // ============================================================
    // ЗАКРЕПЛЕНИЕ СТРОК АГРЕГАТОВ
    // ============================================================

    fun pinItems(indices: List<Int>) {
        val current = _resources.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return

        val inRoot = validIndices.filter { current[it].groupId.isEmpty() }
        val inGroups = validIndices.filter { current[it].groupId.isNotEmpty() }

        if (inRoot.isNotEmpty()) {
            val pinnedGroupId = ensurePinnedGroupExists()
            inRoot.forEach { idx ->
                current[idx] = current[idx].copy(groupId = pinnedGroupId, pinned = true)
            }
            val fuselageIdx = current.indexOfFirst { it.name == "Фюзеляж" }
            if (fuselageIdx >= 0 && current[fuselageIdx].groupId != pinnedGroupId) {
                current[fuselageIdx] = current[fuselageIdx].copy(
                    groupId = pinnedGroupId,
                    pinned = true
                )
            }
        }
        inGroups.forEach { idx ->
            current[idx] = current[idx].copy(pinned = true)
        }
        _resources.value = current
        saveResourcesAndGroups()
    }

    fun unpinItems(indices: List<Int>) {
        val current = _resources.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return

        validIndices.forEach { idx ->
            val item = current[idx]
            if (item.groupId == PINNED_GROUP_ID) {
                current[idx] = item.copy(groupId = "", pinned = false)
            } else {
                current[idx] = item.copy(pinned = false)
            }
        }

        val pinnedStillHasItems = current.any { it.groupId == PINNED_GROUP_ID }
        if (!pinnedStillHasItems) {
            current.forEachIndexed { i, item ->
                if (item.groupId == PINNED_GROUP_ID) {
                    current[i] = item.copy(groupId = "", pinned = false)
                }
            }
            _groups.value = _groups.value.filter { it.id != PINNED_GROUP_ID }
        }

        _resources.value = current
        saveResourcesAndGroups()
    }

    private fun ensurePinnedGroupExists(): String {
        val existing = _groups.value.firstOrNull { it.id == PINNED_GROUP_ID }
        if (existing != null) return PINNED_GROUP_ID

        val minOrder = (_groups.value.minOfOrNull { it.order } ?: 1) - 1
        val pinnedGroup = AggregateGroup(
            id = PINNED_GROUP_ID,
            name = tr(R.string.msg_pinned_4ea5e79),
            order = minOrder,
            pinned = true,
            collapsed = false
        )
        _groups.value = listOf(pinnedGroup) + _groups.value
        return PINNED_GROUP_ID
    }

    fun deleteItems(indices: List<Int>) {
        val current = _resources.value
        val validIndices = indices.filter { it >= 0 && it < current.size }.toSet()
        val newList = current.filterIndexed { idx, item ->
            if (item.name == "Фюзеляж") return@filterIndexed true
            idx !in validIndices
        }
        _resources.value = recalculateResourceSne(
            newList,
            _profile.value?.fuselageSne ?: 0
        )
        val remainingGroupIds = _resources.value.map { it.groupId }.filter { it.isNotEmpty() }.toSet()
        _groups.value = _groups.value.filter {
            it.id == PINNED_GROUP_ID || it.id in remainingGroupIds
        }
        saveResourcesAndGroups()
    }

    fun removeFromGroup(indices: List<Int>) {
        val current = _resources.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return
        validIndices.forEach { idx ->
            current[idx] = current[idx].copy(groupId = "", pinned = false)
        }
        _resources.value = current
        val remainingGroupIds = _resources.value.map { it.groupId }.filter { it.isNotEmpty() }.toSet()
        _groups.value = _groups.value.filter {
            it.id == PINNED_GROUP_ID || it.id in remainingGroupIds
        }
        saveResourcesAndGroups()
    }

    fun moveItem(from: Int, to: Int) {
        val current = _resources.value.toMutableList()
        if (from !in current.indices) return
        if (to !in 0..current.size) return
        if (from == to) return

        val item = current.removeAt(from)
        val insertAt = if (to > from) to - 1 else to
        current.add(insertAt.coerceIn(0, current.size), item)

        val minIdx = minOf(from, insertAt)
        val maxIdx = maxOf(from, insertAt)
        for (i in minIdx..maxIdx) {
            current[i] = current[i].copy(order = i)
        }
        _resources.value = current
        saveResources()
    }

    // ============================================================
    // UI-СОСТОЯНИЕ (sortMode / viewMode)
    // ============================================================

    fun setAggregateSortMode(mode: AggregateSortMode) {
        _aggregateSortMode.value = mode
        saveUiState()
    }

    fun setAggregateViewMode(mode: AggregateViewMode) {
        _aggregateViewMode.value = mode
        saveUiState()
    }

    fun setOilSortMode(mode: AggregateSortMode) {
        _oilSortMode.value = mode
        saveUiState()
    }

    fun setOilViewMode(mode: AggregateViewMode) {
        _oilViewMode.value = mode
        saveUiState()
    }

    private fun saveUiState() {
        val aggregatesSort = _aggregateSortMode.value.name
        val aggregatesView = _aggregateViewMode.value.name
        val oilsSort = _oilSortMode.value.name
        val oilsView = _oilViewMode.value.name
        mutateProfile { it.copy(aggregateSortMode = aggregatesSort, aggregateViewMode = aggregatesView,
            oilSortMode = oilsSort, oilViewMode = oilsView) }
    }
    // ============================================================
    // OILS — CRUD
    // ============================================================

    fun addOil() {
        val newItem = OilItem(
            name = tr(R.string.msg_new_oil_465a698),
            term = "1"
        )
        _oils.value = _oils.value + newItem
        saveOils()
    }

    fun updateOil(index: Int, item: OilItem) {
        val current = _oils.value.toMutableList()
        if (index in current.indices) {
            current[index] = item
            _oils.value = current
            saveOils()
        }
    }

    fun removeOil(item: OilItem) {
        _oils.value = _oils.value - item
        saveOils()
    }

    fun updateOils(list: List<OilItem>) {
        _oils.value = list
        saveOils()
    }

    // ============================================================
    // ГРУППЫ МАСЕЛ — CRUD
    // ============================================================

    fun createOilGroup(name: String, itemIndices: List<Int>): String {
        val groupId = UUID.randomUUID().toString()
        val nextOrder = (_oilGroups.value.maxOfOrNull { it.order } ?: -1) + 1
        val newGroup = AggregateGroup(
            id = groupId,
            name = name.trim().ifBlank { tr(R.string.msg_group_ae8ad7b) },
            order = nextOrder,
            pinned = false,
            collapsed = false
        )
        val current = _oils.value.toMutableList()
        val filteredIndices = itemIndices.filter { it in current.indices }
        filteredIndices.forEach { idx ->
            current[idx] = current[idx].copy(groupId = groupId)
        }
        _oilGroups.value = _oilGroups.value + newGroup
        _oils.value = current
        saveOilsAndGroups()
        return groupId
    }

    fun renameOilGroup(groupId: String, newName: String) {
        if (groupId == PINNED_GROUP_ID) return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        _oilGroups.value = _oilGroups.value.map {
            if (it.id == groupId) it.copy(name = trimmed) else it
        }
        saveOilGroups()
    }

    fun deleteOilGroup(groupId: String, deleteItems: Boolean) {
        if (groupId == PINNED_GROUP_ID) return
        val currentOils = _oils.value
        val newOils = if (deleteItems) {
            currentOils.filter { it.groupId != groupId }
        } else {
            currentOils.map {
                if (it.groupId == groupId) it.copy(groupId = "") else it
            }
        }
        _oilGroups.value = _oilGroups.value.filter { it.id != groupId }
        _oils.value = newOils
        saveOilsAndGroups()
    }

    fun toggleOilGroupCollapsed(groupId: String) {
        _oilGroups.value = _oilGroups.value.map {
            if (it.id == groupId) it.copy(collapsed = !it.collapsed) else it
        }
        saveOilGroups()
    }

    // ============================================================
    // ЗАКРЕПЛЕНИЕ МАСЕЛ
    // ============================================================

    fun pinOils(indices: List<Int>) {
        val current = _oils.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return

        val inRoot = validIndices.filter { current[it].groupId.isEmpty() }
        val inGroups = validIndices.filter { current[it].groupId.isNotEmpty() }

        if (inRoot.isNotEmpty()) {
            val pinnedGroupId = ensureOilPinnedGroupExists()
            inRoot.forEach { idx ->
                current[idx] = current[idx].copy(groupId = pinnedGroupId, pinned = true)
            }
        }
        inGroups.forEach { idx ->
            current[idx] = current[idx].copy(pinned = true)
        }
        _oils.value = current
        saveOilsAndGroups()
    }

    fun unpinOils(indices: List<Int>) {
        val current = _oils.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return

        validIndices.forEach { idx ->
            val item = current[idx]
            if (item.groupId == PINNED_GROUP_ID) {
                current[idx] = item.copy(groupId = "", pinned = false)
            } else {
                current[idx] = item.copy(pinned = false)
            }
        }

        val pinnedStillHasItems = current.any { it.groupId == PINNED_GROUP_ID }
        if (!pinnedStillHasItems) {
            current.forEachIndexed { i, item ->
                if (item.groupId == PINNED_GROUP_ID) {
                    current[i] = item.copy(groupId = "", pinned = false)
                }
            }
            _oilGroups.value = _oilGroups.value.filter { it.id != PINNED_GROUP_ID }
        }

        _oils.value = current
        saveOilsAndGroups()
    }

    private fun ensureOilPinnedGroupExists(): String {
        val existing = _oilGroups.value.firstOrNull { it.id == PINNED_GROUP_ID }
        if (existing != null) return PINNED_GROUP_ID

        val minOrder = (_oilGroups.value.minOfOrNull { it.order } ?: 1) - 1
        val pinnedGroup = AggregateGroup(
            id = PINNED_GROUP_ID,
            name = tr(R.string.msg_pinned_4ea5e79),
            order = minOrder,
            pinned = true,
            collapsed = false
        )
        _oilGroups.value = listOf(pinnedGroup) + _oilGroups.value
        return PINNED_GROUP_ID
    }

    fun deleteOils(indices: List<Int>) {
        val current = _oils.value
        val validIndices = indices.filter { it >= 0 && it < current.size }.toSet()
        _oils.value = current.filterIndexed { idx, _ -> idx !in validIndices }

        val remainingGroupIds = _oils.value.map { it.groupId }.filter { it.isNotEmpty() }.toSet()
        _oilGroups.value = _oilGroups.value.filter {
            it.id == PINNED_GROUP_ID || it.id in remainingGroupIds
        }
        saveOilsAndGroups()
    }

    fun removeOilsFromGroup(indices: List<Int>) {
        val current = _oils.value.toMutableList()
        val validIndices = indices.filter { it >= 0 && it < current.size }
        if (validIndices.isEmpty()) return
        validIndices.forEach { idx ->
            current[idx] = current[idx].copy(groupId = "", pinned = false)
        }
        _oils.value = current
        val remainingGroupIds = _oils.value.map { it.groupId }.filter { it.isNotEmpty() }.toSet()
        _oilGroups.value = _oilGroups.value.filter {
            it.id == PINNED_GROUP_ID || it.id in remainingGroupIds
        }
        saveOilsAndGroups()
    }

    fun moveOil(from: Int, to: Int) {
        val current = _oils.value.toMutableList()
        if (from !in current.indices) return
        if (to !in 0..current.size) return
        if (from == to) return

        val item = current.removeAt(from)
        val insertAt = if (to > from) to - 1 else to
        current.add(insertAt.coerceIn(0, current.size), item)

        val minIdx = minOf(from, insertAt)
        val maxIdx = maxOf(from, insertAt)
        for (i in minIdx..maxIdx) {
            current[i] = current[i].copy(order = i)
        }
        _oils.value = current
        saveOils()
    }

    // ============================================================
    // СНЭ ФЮЗЕЛЯЖА
    // ============================================================

    fun setFuselageSne(sne: Int) {
        val profile = _profile.value ?: return
        viewModelScope.launch {
            val updated = profile.copy(
                fuselageSne = sne,
                updatedAt = System.currentTimeMillis()
            )
            database.profileDao().update(updated)
            _profile.value = updated
        }
    }
}
