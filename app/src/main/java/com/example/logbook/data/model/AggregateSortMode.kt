package com.example.logbook.data.model

/**
 * Режим сортировки строк внутри таблицы «Агрегаты».
 *
 * Применяется глобально ко всем строкам (внутри групп и в корне).
 */
enum class AggregateSortMode {
    NONE,                    // порядок ручной (order)
    BY_REMAINING_HOURS,      // по остатку в часах (calcRemaining1)
    BY_REMAINING_YEARS,      // по остатку в годах (calcRemaining2) — по возрастанию даты
    BY_ISSUE_DATE            // по дате выпуска — по возрастанию
}