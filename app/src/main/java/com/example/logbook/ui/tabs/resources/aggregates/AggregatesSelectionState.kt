package com.example.logbook.ui.tabs.resources.aggregates

/**
 * Состояние режима выделения.
 *
 * Хранит:
 *  - флаг режима (можно выйти),
 *  - множество выбранных индексов строк,
 *  - множество выбранных ID групп.
 *
 * Операции: toggle, add, remove, clear, selectAll.
 */
data class AggregatesSelectionState(
    val active: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val selectedGroupIds: Set<String> = emptySet()
) {
    val count: Int get() = selectedIndices.size + selectedGroupIds.size

    /**
     * Выбрана ли одна группа (для показа кнопки «Переименовать»).
     */
    val singleGroupSelected: String?
        get() = if (selectedGroupIds.size == 1 && selectedIndices.isEmpty())
            selectedGroupIds.first() else null

    /**
     * Есть ли хотя бы одна выбранная строка (для показа «Создать группу»).
     */
    val hasItemSelection: Boolean get() = selectedIndices.isNotEmpty()

    fun toggleIndex(index: Int): AggregatesSelectionState {
        val newSet = if (index in selectedIndices) selectedIndices - index
        else selectedIndices + index
        val hasAny = newSet.isNotEmpty() || selectedGroupIds.isNotEmpty()
        return copy(
            active = hasAny,
            selectedIndices = newSet
        )
    }

    fun toggleGroup(groupId: String): AggregatesSelectionState {
        val newSet = if (groupId in selectedGroupIds) selectedGroupIds - groupId
        else selectedGroupIds + groupId
        val hasAny = newSet.isNotEmpty() || selectedIndices.isNotEmpty()
        return copy(
            active = hasAny,
            selectedGroupIds = newSet
        )
    }

    fun addIndices(indices: Collection<Int>): AggregatesSelectionState =
        copy(active = true, selectedIndices = selectedIndices + indices)

    fun addGroups(ids: Collection<String>): AggregatesSelectionState =
        copy(active = true, selectedGroupIds = selectedGroupIds + ids)

    fun clear(): AggregatesSelectionState = AggregatesSelectionState()
}