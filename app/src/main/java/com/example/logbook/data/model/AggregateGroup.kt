package com.example.logbook.data.model

import kotlinx.serialization.Serializable

/**
 * Группа агрегатов.
 *
 * Хранится в ProfileEntity.aggregateGroupsJson — JSON-массив объектов этого класса.
 *
 * ВАЖНО: для групп, отображаемых в UI, используется поле `collapsed`.
 * Оно сохраняется в JSON, потому что пользователь хочет видеть одну и ту же
 * свёрнутость между сессиями.
 */
@Serializable
data class AggregateGroup(
    val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val pinned: Boolean = false,
    val collapsed: Boolean = false
)

/**
 * Специальный ID группы «Закреплённые» — автоматически создаётся,
 * когда пользователь закрепляет первую строку в корне.
 * Хранится вместе с остальными группами, но его нельзя удалить или переименовать.
 */
const val PINNED_GROUP_ID = "__pinned__"