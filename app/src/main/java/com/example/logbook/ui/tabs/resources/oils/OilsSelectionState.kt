package com.example.logbook.ui.tabs.resources.oils

/**
 * Состояние режима выделения для масел.
 * Аналогично AggregatesSelectionState.
 */
data class OilsSelectionState(
    val active: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val selectedGroupIds: Set<String> = emptySet()
) {
    val count: Int get() = selectedIndices.size + selectedGroupIds.size

    val singleGroupSelected: String?
        get() = if (selectedGroupIds.size == 1 && selectedIndices.isEmpty())
            selectedGroupIds.first() else null

    val hasItemSelection: Boolean get() = selectedIndices.isNotEmpty()

    fun toggleIndex(index: Int): OilsSelectionState {
        val newSet = if (index in selectedIndices) selectedIndices - index
        else selectedIndices + index
        val hasAny = newSet.isNotEmpty() || selectedGroupIds.isNotEmpty()
        return copy(active = hasAny, selectedIndices = newSet)
    }

    fun toggleGroup(groupId: String): OilsSelectionState {
        val newSet = if (groupId in selectedGroupIds) selectedGroupIds - groupId
        else selectedGroupIds + groupId
        val hasAny = newSet.isNotEmpty() || selectedIndices.isNotEmpty()
        return copy(active = hasAny, selectedGroupIds = newSet)
    }

    fun addIndices(indices: Collection<Int>): OilsSelectionState =
        copy(active = true, selectedIndices = selectedIndices + indices)

    fun addGroups(ids: Collection<String>): OilsSelectionState =
        copy(active = true, selectedGroupIds = selectedGroupIds + ids)

    fun clear(): OilsSelectionState = OilsSelectionState()
}