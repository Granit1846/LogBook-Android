package com.example.logbook.ui.tabs.resources.oils

import com.example.logbook.data.model.AggregateSortMode
import com.example.logbook.data.model.OilItem
import com.example.logbook.data.model.calcRemaining1
import com.example.logbook.data.model.calcRemaining2

/**
 * Сортировка и фильтрация масел.
 *
 * Сортировки:
 *  - BY_REMAINING_HOURS → по остатку чч:мм (calcRemaining1).
 *  - BY_REMAINING_YEARS → по остатку гг:мм (calcRemaining2).
 *  - BY_ISSUE_DATE      → по дате заливки (byYears).
 */
object OilsSort {

    fun apply(items: List<OilItem>, mode: AggregateSortMode, fuselageSne: Int): List<OilItem> {
        if (mode == AggregateSortMode.NONE) return items

        return when (mode) {
            AggregateSortMode.NONE -> items
            AggregateSortMode.BY_REMAINING_HOURS ->
                items.sortedBy { it.calcRemaining1(fuselageSne) }
            AggregateSortMode.BY_REMAINING_YEARS ->
                items.sortedBy { remaining2ToDays(it.calcRemaining2()) }
            AggregateSortMode.BY_ISSUE_DATE ->
                items.sortedBy { parseDateForSort(it.byYears) }
        }
    }

    fun filter(items: List<OilItem>, query: String): List<OilItem> {
        if (query.isBlank()) return items
        val q = query.trim().lowercase()
        return items.filter { it.name.lowercase().contains(q) }
    }

    private fun remaining2ToDays(value: String): Int {
        if (value.isBlank()) return Int.MAX_VALUE
        val v = value.trim()

        if (v.endsWith("дн.")) {
            val numStr = v.removeSuffix("дн.").trim()
            return numStr.toIntOrNull() ?: Int.MAX_VALUE
        }

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

        if (body.endsWith("г.")) {
            val years = body.removeSuffix("г.").trim().toIntOrNull() ?: return Int.MAX_VALUE
            val totalDays = years * 365
            return if (negative) -totalDays else totalDays
        }

        return v.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun parseDateForSort(value: String): Long {
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