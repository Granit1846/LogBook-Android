package com.example.logbook.data.model

import com.example.logbook.data.local.entity.ProfileEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Serializable
data class MaintenanceWork(
    val id: String = UUID.randomUUID().toString(),
    val kind: String = "OTHER",
    val date: String = "",
    val counterMinutes: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class DefectTask(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val workId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
)

data class WorkDue(
    val kind: String,
    val last: MaintenanceWork?,
    val lower: String?,
    val nominal: String?,
    val upper: String?,
    val state: DueState
)

data class RemainingWindow(val lower: Long, val nominal: Long, val upper: Long,
                           val current: Long, val hourly: Boolean, val third: Int)

enum class DueState { NEEDS_BASELINE, LATER, DUE, OVERDUE }

object MaintenanceJournal {
    const val PERIODIC = "PERIODIC"
    const val HOURS_50 = "HOURS_50"
    const val HOURS_100 = "HOURS_100"
    const val HOURS_300 = "HOURS_300"
    const val REG_12 = "REG_12"
    const val REG_24 = "REG_24"
    const val OTHER = "OTHER"
    val kinds = listOf(PERIODIC, HOURS_50, HOURS_100, HOURS_300, REG_12, REG_24, OTHER)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun normalizeWorks(raw: String): String {
        if (raw.isBlank() || raw == "[]") return "[]"
        return try {
            val element = com.google.gson.JsonParser.parseString(raw)
            if (!element.isJsonArray) return "[]"
            val arr = element.asJsonArray
            val normalized = com.google.gson.JsonArray()
            for (item in arr) {
                if (!item.isJsonObject) continue
                val obj = item.asJsonObject
                val normObj = com.google.gson.JsonObject()

                val id = obj.get("id")?.asString ?: obj.get("Id")?.asString ?: UUID.randomUUID().toString()
                normObj.addProperty("id", id)

                val kind = obj.get("kind")?.asString ?: obj.get("Kind")?.asString ?: OTHER
                normObj.addProperty("kind", kind)

                val date = obj.get("date")?.asString ?: obj.get("Date")?.asString ?: ""
                normObj.addProperty("date", date)

                val counterMinutes = obj.get("counterMinutes")?.asInt ?: obj.get("CounterMinutes")?.asInt ?: 0
                normObj.addProperty("counterMinutes", counterMinutes)

                val desc = obj.get("description")?.asString ?: obj.get("Description")?.asString ?: ""
                normObj.addProperty("description", desc)

                val createdAt = obj.get("createdAt")?.asLong ?: obj.get("CreatedAt")?.asLong ?: System.currentTimeMillis()
                normObj.addProperty("createdAt", createdAt)

                normalized.add(normObj)
            }
            normalized.toString()
        } catch (_: Exception) {
            "[]"
        }
    }

    fun normalizeTasks(raw: String): String {
        if (raw.isBlank() || raw == "[]") return "[]"
        return try {
            val element = com.google.gson.JsonParser.parseString(raw)
            if (!element.isJsonArray) return "[]"
            val arr = element.asJsonArray
            val normalized = com.google.gson.JsonArray()
            for (item in arr) {
                if (!item.isJsonObject) continue
                val obj = item.asJsonObject
                val normObj = com.google.gson.JsonObject()

                val id = obj.get("id")?.asString ?: obj.get("Id")?.asString ?: UUID.randomUUID().toString()
                normObj.addProperty("id", id)

                val desc = obj.get("description")?.asString ?: obj.get("Description")?.asString ?: ""
                normObj.addProperty("description", desc)

                val workId = obj.get("workId")?.asString ?: obj.get("WorkId")?.asString ?: ""
                normObj.addProperty("workId", workId)

                val createdAt = obj.get("createdAt")?.asLong ?: obj.get("CreatedAt")?.asLong ?: System.currentTimeMillis()
                normObj.addProperty("createdAt", createdAt)

                val completedAt = obj.get("completedAt")?.asLong ?: obj.get("CompletedAt")?.asLong ?: 0L
                normObj.addProperty("completedAt", completedAt)

                normalized.add(normObj)
            }
            normalized.toString()
        } catch (_: Exception) {
            "[]"
        }
    }

    fun works(profile: ProfileEntity): List<MaintenanceWork> = try {
        json.decodeFromString(normalizeWorks(profile.maintenanceWorksJson))
    } catch (_: Exception) {
        emptyList()
    }

    fun tasks(profile: ProfileEntity): List<DefectTask> = try {
        json.decodeFromString(normalizeTasks(profile.defectTasksJson))
    } catch (_: Exception) {
        emptyList()
    }

    fun encodeWorks(value: List<MaintenanceWork>): String = json.encodeToString(value)
    fun encodeTasks(value: List<DefectTask>): String = json.encodeToString(value)

    fun openTasks(profile: ProfileEntity): List<DefectTask> = tasks(profile).filter { it.completedAt == 0L }
    fun visibleTasks(profile: ProfileEntity, now: Long): List<DefectTask> = tasks(profile)
        .filter { it.completedAt == 0L || now - it.completedAt < 600_000L }
        .sortedByDescending { it.createdAt }

    fun due(works: List<MaintenanceWork>, counterMinutes: Int, today: LocalDate): List<WorkDue> =
        kinds.filter { it != OTHER }.map { kind ->
            val candidates = works.filter { it.kind == kind || kind == HOURS_50 && it.kind == HOURS_100 }
            val last = if (kind in listOf(HOURS_50, HOURS_100, HOURS_300)) {
                candidates.maxWithOrNull(compareBy<MaintenanceWork> { it.counterMinutes }.thenBy { it.createdAt })
            } else {
                candidates.maxWithOrNull(compareBy<MaintenanceWork> { runCatching { LocalDate.parse(it.date) }.getOrNull() ?: LocalDate.MIN }.thenBy { it.createdAt })
            }
            if (last == null) return@map WorkDue(kind, null, null, null, null, DueState.NEEDS_BASELINE)
            if (kind in listOf(HOURS_50, HOURS_100, HOURS_300)) {
                val offsets = when (kind) {
                    HOURS_50 -> Triple(45, 50, 60)
                    HOURS_100 -> Triple(90, 100, 120)
                    else -> Triple(285, 300, 330)
                }
                val lower = last.counterMinutes.toLong() + offsets.first * 60L
                val nominal = last.counterMinutes.toLong() + offsets.second * 60L
                val upper = last.counterMinutes.toLong() + offsets.third * 60L
                WorkDue(kind, last, formatHours(lower), formatHours(nominal), formatHours(upper),
                    when { counterMinutes.toLong() < lower -> DueState.LATER
                        counterMinutes.toLong() > upper -> DueState.OVERDUE
                        else -> DueState.DUE })
            } else {
                val base = runCatching { LocalDate.parse(last.date) }.getOrNull() ?: today
                val bounds = when (kind) {
                    PERIODIC -> Triple(base.plusDays(15), base.plusDays(20), base.plusDays(25))
                    REG_12 -> Triple(base.plusMonths(12), base.plusMonths(12), base.plusMonths(13))
                    else -> Triple(base.plusMonths(24), base.plusMonths(24), base.plusMonths(26))
                }
                WorkDue(kind, last, bounds.first.toString(), bounds.second.toString(), bounds.third.toString(),
                    when { today.isBefore(bounds.first) -> DueState.LATER
                        today.isAfter(bounds.third) -> DueState.OVERDUE
                        else -> DueState.DUE })
            }
        }

    fun formatHours(minutes: Long): String = "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"

    fun roundHours(minutes: Long): Long = Math.round(minutes / 60.0)

    fun remaining(value: WorkDue, counterMinutes: Int, today: LocalDate): RemainingWindow? {
        val last = value.last ?: return null
        val hourly = value.kind.startsWith("HOURS")
        val elapsed: Long
        val total: Long
        val lower: Long
        val nominal: Long
        val upper: Long
        if (hourly) {
            val offsets = when (value.kind) {
                HOURS_50 -> Triple(45L, 50L, 60L)
                HOURS_100 -> Triple(90L, 100L, 120L)
                else -> Triple(285L, 300L, 330L)
            }
            lower = offsets.first
            nominal = offsets.second
            upper = offsets.third
            elapsed = counterMinutes.toLong() - last.counterMinutes
            total = upper * 60
        } else {
            val start = runCatching { LocalDate.parse(last.date) }.getOrNull() ?: return null
            val lowerDate = runCatching { LocalDate.parse(value.lower ?: return null) }.getOrNull() ?: return null
            val nominalDate = runCatching { LocalDate.parse(value.nominal ?: return null) }.getOrNull() ?: return null
            val upperDate = runCatching { LocalDate.parse(value.upper ?: return null) }.getOrNull() ?: return null
            lower = ChronoUnit.DAYS.between(start, lowerDate)
            nominal = ChronoUnit.DAYS.between(start, nominalDate)
            upper = ChronoUnit.DAYS.between(start, upperDate)
            elapsed = ChronoUnit.DAYS.between(start, today)
            total = upper
        }
        val third = when {
            elapsed * 3 < total -> 0
            elapsed * 3 < total * 2 -> 1
            else -> 2
        }
        return RemainingWindow(lower, nominal, upper, elapsed, hourly, third)
    }
    fun parseHours(text: String): Int? {
        val match = Regex("^(\\d+):([0-5]\\d)$").matchEntire(text.trim()) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLong()
        return (hours * 60 + minutes).takeIf { it <= Int.MAX_VALUE }?.toInt()
    }
}
