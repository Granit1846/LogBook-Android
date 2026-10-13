package com.example.logbook.ui.util

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

// ============================================================
// Время
// ============================================================

fun formatTimeInput(raw: String): String {
    val normalized = raw.replace(',', ':').replace('.', ':')
    val cleaned = normalized.filter { it.isDigit() || it == ':' }

    val firstColon = cleaned.indexOf(':')
    if (firstColon == -1) return cleaned   // ← "5" → "5", ок

    val hours = cleaned.substring(0, firstColon).filter { it.isDigit() }
    val minutes = cleaned.substring(firstColon + 1).filter { it.isDigit() }.take(2)

    return "$hours:$minutes"
}

fun parseTimeInput(input: String): Int? {
    if (input.isBlank()) return 0
    if (!input.contains(':')) {
        val h = input.toIntOrNull() ?: return null
        return if (h < 0) null else h * 60
    }
    val parts = input.split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null

    val mStr = parts[1]
    val m = when (mStr.length) {
        0 -> 0
        1 -> mStr.toIntOrNull()?.times(10) ?: return null
        else -> mStr.toIntOrNull() ?: return null
    }
    if (h < 0 || m !in 0..59) return null
    return h * 60 + m
}

fun minutesToTime(minutes: Int): String {
    if (minutes == 0) return ""
    val sign = if (minutes < 0) "-" else ""
    val abs = kotlin.math.abs(minutes)
    val h = abs / 60
    val m = abs % 60
    return "$sign$h:${m.toString().padStart(2, '0')}"
}

// ============================================================
// Дата
// ============================================================

fun formatDateInput(raw: String): String {
    val normalized = raw.replace(',', '.')
    val digits = normalized.filter { it.isDigit() }.take(6)

    val sb = StringBuilder()
    for ((i, ch) in digits.withIndex()) {
        sb.append(ch)
        if ((i == 1 || i == 3) && i < digits.lastIndex) sb.append('.')
    }
    return sb.toString()
}

fun parseDateInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.length < 6) return ""

    val dd = digits.substring(0, 2).toIntOrNull() ?: return ""
    val mm = digits.substring(2, 4).toIntOrNull() ?: return ""
    val yy = digits.substring(4, 6)

    if (dd < 1 || dd > 31) return tr(R.string.msg_error_72aecd9)
    if (mm < 1 || mm > 12) return tr(R.string.msg_error_72aecd9)

    return "%02d.%02d.20%s".format(dd, mm, yy)
}

fun displayDate(dbDate: String): String {
    if (dbDate == "Итого") return com.example.logbook.localization.I18n.display(dbDate)
    val parts = dbDate.split(".")
    if (parts.size == 3 && parts[2].length == 4) {
        return "${parts[0]}.${parts[1]}.${parts[2].substring(2)}"
    }
    return dbDate
}
