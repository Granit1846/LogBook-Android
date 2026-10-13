package com.example.logbook.data.model

/**
 * Режим отображения таблицы «Агрегаты».
 *
 * ROWS         — только строки (все колонки).
 * ROWS_COMPACT — строки с краткой информацией (только имя + остаток).
 * CARDS        — карточки (имя + ключевые поля крупным шрифтом).
 */
enum class AggregateViewMode {
    ROWS,
    ROWS_COMPACT,
    CARDS
}