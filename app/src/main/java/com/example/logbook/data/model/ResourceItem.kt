package com.example.logbook.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Агрегат (строка таблицы «Агрегаты»).
 *
 * Группировка: groupId указывает на AggregateGroup.id.
 *   "" — строка вне группы (в корне).
 *
 * Закрепление: pinned = true — строка поднимается вверх
 *   (в корне → в группу «Закреплённые»; внутри группы → вверх группы).
 */
data class ResourceItem(
    val name: String = "",
    val sne: Int = 0,
    val extendedTo1: Int = 0,
    val yearsResource: String = "",
    val issueDate: String = "",
    val extendedTo2: String = "",
    val remaining1: Int = 0,
    val remaining2: String = "",
    val installedAtSne1: Int = 0,
    val installedAtSne2: Int = 0,
    val coefficient: String = "",

    // === Группировка / закрепление ===
    val groupId: String = "",      // "" — вне группы
    val pinned: Boolean = false,   // закреплено
    val order: Int = 0,            // порядок внутри группы / корня (не путать с порядком в списке)
    val emptyFields: Set<String> = setOf("sne", "extendedTo1", "remaining1", "installedAtSne1", "installedAtSne2")
) {
    /** Остаток чч:мм = ExtendedTo1 − SNE */
    fun calcRemaining1(): Int = extendedTo1 - sne

    /** Остаток гг:мм = (Дата выпуска + Ресурс_лет) − Сегодня */
    fun calcRemaining2(today: java.time.LocalDate = java.time.LocalDate.now()): String {
        val issue = ResourceDates.parse(issueDate) ?: return ""
        val days = ResourceDates.duration(yearsResource, 365) ?: return ""
        val expiry = runCatching { issue.plusDays(days) }.getOrNull() ?: return ""
        return ResourceDates.remaining(expiry, today)
    }
}