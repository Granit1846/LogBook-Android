package com.example.logbook.data.model

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ResourceWarningLevel(private val labelRes: Int) {
    NONE(0), WARNING(R.string.msg_warning_5f5f867), URGENT(R.string.msg_low_remaining_life_a90a923),
    CRITICAL(R.string.msg_critically_low_remaining_life_15197df), EXPIRED(R.string.msg_service_life_exhausted_3de1e91);
    val label: String get() = if (labelRes == 0) "" else tr(labelRes)
}

data class ResourceWarning(val level: ResourceWarningLevel, val text: String)

object ResourceWarnings {
    fun forProfile(profile: com.example.logbook.data.local.entity.ProfileEntity, today: LocalDate): List<ResourceWarning> {
        val gson = com.google.gson.Gson()
        val resources = runCatching {
            gson.fromJson<List<ResourceItem>>(profile.resourcesJson,
                object : com.google.gson.reflect.TypeToken<List<ResourceItem>>() {}.type).orEmpty()
        }.getOrDefault(emptyList()).map { item ->
            item.copy(sne = if (item.name == "Фюзеляж") profile.fuselageSne
                else profile.fuselageSne - item.installedAtSne1 + item.installedAtSne2)
        }
        val oils = runCatching {
            gson.fromJson<List<OilItem>>(profile.oilsJson,
                object : com.google.gson.reflect.TypeToken<List<OilItem>>() {}.type).orEmpty()
        }.getOrDefault(emptyList())
        return (aggregates(resources, today).map { it.copy(text = tr(R.string.msg_components_1_s_39d1e6a, it.text)) } +
            oils(oils, profile.fuselageSne, today).map { it.copy(text = tr(R.string.msg_oils_1_s_0c9777a, it.text)) })
            .sortedByDescending { it.level.ordinal }
    }

    fun aggregateHours(item: ResourceItem): ResourceWarningLevel =
        if (item.extendedTo1 <= 0) ResourceWarningLevel.NONE
        else hours(item.calcRemaining1().toLong(), 100, 50, 25)

    fun oilHours(item: OilItem, fuselageSne: Int): ResourceWarningLevel =
        if (item.coefficient <= 0) ResourceWarningLevel.NONE
        else hours(item.byHours.toLong() + item.coefficient - fuselageSne, 50, 20, 20)

    private fun hours(minutes: Long, warning: Int, urgent: Int, critical: Int) = when {
        minutes < 0 -> ResourceWarningLevel.EXPIRED
        minutes <= critical * 60 -> ResourceWarningLevel.CRITICAL
        minutes <= urgent * 60 -> ResourceWarningLevel.URGENT
        minutes <= warning * 60 -> ResourceWarningLevel.WARNING
        else -> ResourceWarningLevel.NONE
    }

    fun aggregateExpiry(item: ResourceItem): LocalDate? = expiry(item.issueDate, item.yearsResource, false)
    fun oilExpiry(item: OilItem): LocalDate? = expiry(item.byYears, item.term, true)

    private fun expiry(date: String, years: String, months: Boolean): LocalDate? {
        val start = ResourceDates.parse(date) ?: return null
        val duration = ResourceDates.duration(years, if (months) 12 else 365) ?: return null
        return runCatching { if (months) start.plusMonths(duration) else start.plusDays(duration) }.getOrNull()
    }

    fun calendar(expiry: LocalDate?, today: LocalDate, oil: Boolean = false): ResourceWarningLevel {
        if (expiry == null) return ResourceWarningLevel.NONE
        if (expiry < today) return ResourceWarningLevel.EXPIRED
        val months = (expiry.year - today.year) * 12 + expiry.monthValue - today.monthValue -
            if (expiry.dayOfMonth < today.dayOfMonth) 1 else 0
        return when {
            months <= 1 -> ResourceWarningLevel.CRITICAL
            months <= 2 -> if (oil) ResourceWarningLevel.WARNING else ResourceWarningLevel.URGENT
            months <= 3 && !oil -> ResourceWarningLevel.WARNING
            else -> ResourceWarningLevel.NONE
        }
    }

    fun aggregates(items: List<ResourceItem>, today: LocalDate): List<ResourceWarning> = buildList {
        items.filter { it.name.isNotBlank() }.forEach { item ->
            addHours(item.name, aggregateHours(item), item.calcRemaining1().toLong(), false)
            addCalendar(item.name, aggregateExpiry(item), today, false)
        }
    }.sortedByDescending { it.level.ordinal }

    fun oils(items: List<OilItem>, fuselageSne: Int, today: LocalDate): List<ResourceWarning> = buildList {
        items.filter { it.name.isNotBlank() }.forEach { item ->
            addHours(item.name, oilHours(item, fuselageSne), item.byHours.toLong() + item.coefficient - fuselageSne, true)
            addCalendar(item.name, oilExpiry(item), today, true)
        }
    }.sortedByDescending { it.level.ordinal }

    private fun MutableList<ResourceWarning>.addHours(name: String, level: ResourceWarningLevel, minutes: Long, oil: Boolean) {
        if (level == ResourceWarningLevel.NONE) return
        val abs = kotlin.math.abs(minutes)
        val time = "${abs / 60}:${(abs % 60).toString().padStart(2, '0')}"
        val detail = if (minutes < 0) tr(R.string.msg_service_life_exceeded_by_1_s_h_min_757fc70, time)
            else tr(R.string.msg_1_s_h_min_remaining_fd27938, time)
        add(ResourceWarning(level, "$name: $detail.${if (oil && level >= ResourceWarningLevel.CRITICAL) tr(R.string.msg_oil_replacement_required) else ""}"))
    }

    private fun MutableList<ResourceWarning>.addCalendar(name: String, expiry: LocalDate?, today: LocalDate, oil: Boolean) {
        val level = calendar(expiry, today, oil)
        if (level == ResourceWarningLevel.NONE || expiry == null) return
        val days = ChronoUnit.DAYS.between(today, expiry)
        val detail = if (days < 0) tr(R.string.msg_expired_1_s_days_ago_7ef371f, -days) else tr(R.string.msg_1_s_days_until_expiry_d7801c6, days)
        add(ResourceWarning(level, "$name: $detail.${if (oil && level >= ResourceWarningLevel.CRITICAL) tr(R.string.msg_oil_replacement_required) else ""}"))
    }
}
