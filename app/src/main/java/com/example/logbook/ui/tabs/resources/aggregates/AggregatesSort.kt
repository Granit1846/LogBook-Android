package com.example.logbook.ui.tabs.resources.aggregates

import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.ResourceItem

/**
 * Утилиты сортировки и фильтрации агрегатов.
 */
object AggregatesSort {

    fun apply(items: List<ResourceItem>, mode: AggregateSortMode): List<ResourceItem> {
        if (mode == AggregateSortMode.NONE) return items

        val fuselage = items.firstOrNull { it.name == "Фюзеляж" }
        val rest = items.filter { it.name != "Фюзеляж" }

        val sorted = when (mode) {
            AggregateSortMode.NONE -> rest
            AggregateSortMode.BY_REMAINING_HOURS ->
                rest.sortedBy { it.calcRemaining1() }
            AggregateSortMode.BY_REMAINING_YEARS ->
                rest.sortedBy { remaining2ToDays(it.calcRemaining2()) }
            AggregateSortMode.BY_ISSUE_DATE ->
                rest.sortedBy { parseIssueDateForSort(it.issueDate) }
        }

        return if (fuselage != null) listOf(fuselage) + sorted else sorted
    }

    fun filter(items: List<ResourceItem>, query: String): List<ResourceItem> {
        if (query.isBlank()) return items
        val q = query.trim().lowercase()
        return items.filter { it.name.lowercase().contains(q) }
    }

    /**
     * Парсит строку остатка гг:мм ("5г.3мес.", "-2г.0мес.", "12 дн.", "") в число дней.
     * Для сортировки: меньшее значение — раньше.
     * Пустая строка → Int.MAX_VALUE (в конец).
     */
    private fun remaining2ToDays(value: String): Int {
        if (value.isBlank()) return Int.MAX_VALUE
        val v = value.trim()

        // "N дн."
        if (v.endsWith("дн.")) {
            val numStr = v.removeSuffix("дн.").trim()
            val num = numStr.toIntOrNull() ?: return Int.MAX_VALUE
            return num
        }

        // "Xг.Yмес." или "-Xг.Yмес."
        val negative = v.startsWith("-")
        val body = v.removePrefix("-")

        if (body.contains("г.") && body.contains("мес.")) {
            val parts = body.replace("г.", "|").replace("мес.", "").split('|')
            if (parts.size == 2) {
                val years = parts[0].trim().toIntOrNull() ?: 0
                val months = parts[1].trim().toIntOrNull() ?: 0
                val totalDays = years * 365 + months * 30
                return if (negative) -totalDays else totalDays
            }
        }

        // Только годы: "Xг."
        if (body.endsWith("г.")) {
            val years = body.removeSuffix("г.").trim().toIntOrNull() ?: return Int.MAX_VALUE
            val totalDays = years * 365
            return if (negative) -totalDays else totalDays
        }

        // Просто число
        return v.toIntOrNull() ?: Int.MAX_VALUE
    }

    /**
     * Дата выпуска → число для сортировки (YYYYMMDD).
     * Форматы: dd.MM.yy или dd.MM.yyyy.
     * Не удалось — Long.MAX_VALUE (в конец).
     */
    private fun parseIssueDateForSort(value: String): Long {
        if (value.isBlank()) return Long.MAX_VALUE
        val parts = value.split('.')
        if (parts.size != 3) return Long.MAX_VALUE
        return try {
            val dd = parts[0].toInt()
            val mm = parts[1].toInt()
            val yyRaw = parts[2]
            val year = when (yyRaw.length) {
                2 -> 2000 + yyRaw.toInt()
                4 -> yyRaw.toInt()
                else -> return Long.MAX_VALUE
            }
            year * 10000L + mm * 100L + dd
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }
}