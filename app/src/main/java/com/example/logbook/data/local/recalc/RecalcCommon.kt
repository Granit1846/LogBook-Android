package com.example.logbook.data.local.recalc

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs
import kotlin.math.roundToInt
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Общие утилиты для всех Recalc-сервисов формуляров.
 *
 * Формат даты в формулярах — dd.MM.yyyy (как хранится в *Entity).
 * Формат отображения — dd.MM.yy (через displayDate в ui/util).
 * Формат totalMonth — "yyyy-MM" (например "2026-09").
 * Формат sortOrder — Long: обычные строки = epochDay × 2 (чётное),
 *                    итоговые = epochDay(последний день месяца) × 2 + 1 (нечётное).
 */
internal object RecalcCommon {

    private val DATE_FMT_DOT_4 = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val DATE_FMT_DOT_2 = DateTimeFormatter.ofPattern("dd.MM.yy")

    // ============================================================
    // Время (чч:мм ↔ минуты)
    // ============================================================

    fun minutesToHM(totalMinutes: Int): String {
        val sign = if (totalMinutes < 0) "-" else ""
        val abs = abs(totalMinutes)
        val h = abs / 60
        val m = abs % 60
        return "$sign$h:${m.toString().padStart(2, '0')}"
    }

    fun parseHM(value: String?): Int {
        if (value.isNullOrBlank()) return 0
        val v = value.trim()
        val sign = if (v.startsWith("-")) -1 else 1
        val body = v.removePrefix("-")
        val parts = body.split(':')
        if (parts.size != 2) return 0
        val h = parts[0].toIntOrNull() ?: return 0
        val m = parts[1].toIntOrNull() ?: return 0
        return sign * (h * 60 + m)
    }

    fun parseIntOrZero(value: String?): Int {
        if (value.isNullOrBlank()) return 0
        return value.trim().toIntOrNull() ?: 0
    }

    // ============================================================
    // Даты
    // ============================================================

    fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val v = value.trim()
        return try {
            if (v.count { it == '.' } != 2) return null
            val parts = v.split('.')
            if (parts.size != 3) return null
            val dd = parts[0].toIntOrNull() ?: return null
            val mm = parts[1].toIntOrNull() ?: return null
            val yyRaw = parts[2]
            val year = when (yyRaw.length) {
                2 -> 2000 + (yyRaw.toIntOrNull() ?: return null)
                4 -> yyRaw.toIntOrNull() ?: return null
                else -> return null
            }
            LocalDate.of(year, mm, dd)
        } catch (_: DateTimeParseException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun formatFormularDate(date: LocalDate): String = DATE_FMT_DOT_4.format(date)

    fun formatDisplayDate(date: LocalDate): String = DATE_FMT_DOT_2.format(date)

    // ============================================================
    // Месяц / итоговые строки
    // ============================================================

    fun monthKey(year: Int, month: Int): String =
        "%04d-%02d".format(year, month)

    fun parseMonthKey(key: String?): Pair<Int, Int>? {
        if (key.isNullOrBlank()) return null
        val parts = key.split('-')
        if (parts.size != 2) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        return y to m
    }

    fun totalLabel(): String = "Итого"

    fun isTotalForMonth(
        isTotal: Boolean,
        totalMonth: String?,
        year: Int,
        month: Int
    ): Boolean {
        if (!isTotal) return false
        val parsed = parseMonthKey(totalMonth) ?: return false
        return parsed.first == year && parsed.second == month
    }

    // ============================================================
    // sortOrder
    // ============================================================

    /**
     * Порядок обычной строки: epochDay × 2 (чётное).
     * Всегда меньше, чем итог того же дня (если бы он был).
     * Меньше, чем итог месяца (который равен посл.день × 2 + 1).
     */
    fun regularSortOrder(date: LocalDate): Long = date.toEpochDay() * 2L

    /**
     * Порядок итоговой строки месяца M: (последний день M) × 2 + 1 (нечётное).
     * Больше последнего дня месяца, меньше первого дня следующего месяца.
     */
    fun totalSortOrder(ym: YearMonth): Long =
        ym.atEndOfMonth().toEpochDay() * 2L + 1L

    // ============================================================
    // Расчёты налёта
    // ============================================================

    /**
     * Сначала суммируем землю всех запусков дня, затем учитываем 20%.
     * Округляем до целой минуты: дробная часть < 0.5 вниз, >= 0.5 вверх.
     * Накопительные значения и месячные итоги складывают округлённые дневные значения.
     */
    fun groundMinutesFromZemlya(sumZemlya: Int): Int =
        (sumZemlya * 0.2).roundToInt()

    fun naletMinutes(sumZemlya: Int, sumVozduh: Int): Int =
        groundMinutesFromZemlya(sumZemlya) + sumVozduh

    fun roundedProduct(value: Double, factor: Double): Int =
        BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(factor))
            .setScale(0, RoundingMode.HALF_UP).intValueExact()

    fun roundedSumProduct(base: Double, value: Int, factor: Double, digits: Int): Double =
        BigDecimal.valueOf(base).add(
            BigDecimal.valueOf(value.toLong()).multiply(BigDecimal.valueOf(factor))
        ).setScale(digits, RoundingMode.HALF_UP).toDouble()

    fun roundedDifference(current: Double, previous: Double, digits: Int): Double =
        BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
            .max(BigDecimal.ZERO).setScale(digits, RoundingMode.HALF_UP).toDouble()

    // ============================================================
    // Разницы / форматирование итогов
    // ============================================================

    fun diffHM(curr: String?, prev: String?): Int {
        if (curr.isNullOrBlank() || prev.isNullOrBlank()) return 0
        val c = parseHM(curr)
        val p = parseHM(prev)
        return (c - p).coerceAtLeast(0)
    }

    fun diffInt(curr: String?, prev: String?): Int {
        if (curr.isNullOrBlank() || prev.isNullOrBlank()) return 0
        val c = parseIntOrZero(curr)
        val p = parseIntOrZero(prev)
        return (c - p).coerceAtLeast(0)
    }

    fun totalHM(minutes: Int): String =
        if (minutes > 0) minutesToHM(minutes) else "—"

    fun totalInt(value: Int): String =
        if (value > 0) value.toString() else "—"

    fun optionalHM(minutes: Int, active: Boolean): String = when {
        !active -> ""
        minutes > 0 -> minutesToHM(minutes)
        else -> "—"
    }

    fun optionalInt(value: Int, active: Boolean): String = when {
        !active -> ""
        value > 0 -> value.toString()
        else -> "—"
    }
}
