package com.example.logbook.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

internal object ResourceDates {
    private val shortDate = DateTimeFormatterBuilder().appendPattern("dd.MM.")
        .appendValueReduced(ChronoField.YEAR, 2, 2, 2000).toFormatter().withResolverStyle(ResolverStyle.STRICT)
    fun parse(value: String): LocalDate? = runCatching {
        LocalDate.parse(value.trim(), if (value.trim().length == 8) shortDate
            else DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
    }.getOrNull()

    fun duration(value: String, factor: Int): Long? = runCatching {
        val years = BigDecimal(value.trim().replace(',', '.'))
        require(years.signum() >= 0)
        years.multiply(BigDecimal.valueOf(factor.toLong())).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()

    fun remaining(expiry: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, expiry)
        if (days < 30) return "$days дн."
        val months = (expiry.year - today.year) * 12 + expiry.monthValue - today.monthValue -
            if (expiry.dayOfMonth < today.dayOfMonth) 1 else 0
        return "${months / 12}г.${months % 12}мес."
    }
}
