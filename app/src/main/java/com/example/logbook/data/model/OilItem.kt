package com.example.logbook.data.model

import kotlinx.serialization.Serializable

/**
 * Масло.
 *
 * Группировка: groupId — id группы из ProfileEntity.oilGroupsJson.
 *   "" — вне группы (в корне).
 *
 * Закрепление: pinned = true — поднимается вверх
 *   (в корне → в группу «Закреплённые»; внутри группы → вверх группы).
 */
@Serializable
data class OilItem(
    val name: String = "",              // Где залито
    val byHours: Int = 0,               // СНЭ при заливке, минуты
    val byYears: String = "",           // Дата заливки, dd.MM.yy
    val coefficient: Int = 0,           // Ресурс масла в часах, минуты
    val term: String = "1",             // Ресурс масла в годах

    // === Группировка / закрепление ===
    val groupId: String = "",
    val pinned: Boolean = false,
    val order: Int = 0,
    val emptyFields: Set<String> = setOf("byHours", "coefficient")
)