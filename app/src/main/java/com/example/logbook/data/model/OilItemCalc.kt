package com.example.logbook.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Расчёт остатков по маслу.
 *
 * Remaining1 (чч:мм): ByHours + Coefficient − fuselageSne
 *   - ByHours      — СНЭ при заливке (минуты)
 *   - Coefficient  — ресурс масла в часах (минуты)
 *   - fuselageSne  — последнее значение СНЭ из Части 1 (минуты)
 *
 * Remaining2 (гг:мм или дн.): ByYears + Term (в месяцах) − сегодня
 *   - ByYears  — дата заливки (dd.MM.yy)
 *   - Term     — ресурс в годах ("1", "2", "0,5" и т.д.)
 */
fun OilItem.calcRemaining1(fuselageSneMin: Int): Int {
    if (byHours == 0 && coefficient == 0) return 0
    return byHours + coefficient - fuselageSneMin
}

fun OilItem.calcRemaining2(today: java.time.LocalDate = java.time.LocalDate.now()): String {
    val filled = ResourceDates.parse(byYears) ?: return ""
    val months = ResourceDates.duration(term, 12) ?: return ""
    val expiry = runCatching { filled.plusMonths(months) }.getOrNull() ?: return ""
    return ResourceDates.remaining(expiry, today)
}