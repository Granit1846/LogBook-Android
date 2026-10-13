package com.example.logbook.data.local

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.room.withTransaction
import com.example.logbook.data.local.entity.*
import com.example.logbook.data.model.AggregateReplacement
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

/** Archives and assignments are committed together; serial numbers are shared between profiles. */
class AggregateManager(private val db: AppDatabase, private val today: LocalDate = LocalDate.now()) {
    private val gson = Gson()

    suspend fun replace(profileId: Long, type: String, number: String, reason: String, installationDate: LocalDate = today) {
        require(type in TYPES && number.isNotBlank()) { tr(R.string.msg_enter_the_component_type_and_serial_number_5f71382) }
        assign(profileId, mapOf(type to number.trim()), reason, installationDate)
    }

    suspend fun assign(profileId: Long, requested: Map<String, String>, reason: String = tr(R.string.msg_component_setup_c7ec580), installationDate: LocalDate = today) = db.withTransaction {
        require(requested.keys.all { it in TYPES || it == "Chast1" }) { tr(R.string.msg_unknown_component_type_a7c1b8b) }
        val profile = checkNotNull(db.profileDao().getById(profileId)) { tr(R.string.msg_profile_not_found_0e520dc) }
        val old = numbers(profile.aggregateAssignmentsJson)
        val next = old + requested.mapValues { it.value.trim() }
        val assigned = next.filterKeys { it in TYPES }.filterValues { it.isNotBlank() }
        require(assigned.map { key(it.key, it.value) }.distinct().size == assigned.size) {
            tr(R.string.msg_one_engine_cannot_be_assigned_to_both_left_and_19f61bb)
        }
        db.profileDao().getAll().filter { it.id != profileId }.forEach { other ->
            numbers(other.aggregateAssignmentsJson).filterKeys { it in TYPES }.forEach { (type, number) ->
                require(number.isBlank() || assigned.none { key(it.key, it.value) == key(type, number) }) {
                    tr(R.string.msg_component_1_s_is_already_installed_in_profile__e561ead, number, other.name)
                }
            }
        }
        val changes = TYPES.filter { (old[it] ?: "") != (next[it] ?: "") }
        // Capture all removed aggregates before loading any replacement (also handles an engine-side swap).
        changes.forEach { type -> old[type]?.takeIf { it.isNotBlank() }?.let { number ->
            val canonical = canonical(type)
            val existing = db.aggregateArchiveDao().get(canonical, number)
            db.aggregateArchiveDao().save(AggregateArchiveEntity(id = existing?.id ?: 0,
                type = canonical, number = number, formularJson = snapshot(profileId, type, existing?.formularJson), savedAt = System.currentTimeMillis()))
        } }
        val installed = numbers(profile.aggregateInstalledAtJson).toMutableMap()
        val historyType = object : TypeToken<List<AggregateReplacement>>() {}.type
        val history = gson.fromJson<List<AggregateReplacement>>(profile.aggregateHistoryJson, historyType).orEmpty().toMutableList()
        changes.forEach { type ->
            val previous = old[type].orEmpty()
            val number = next[type].orEmpty()
            val archive = if (number.isNotBlank()) db.aggregateArchiveDao().get(canonical(type), number) else null
            if (previous.isNotBlank() || archive != null) {
                load(profileId, type, archive?.formularJson)
                installed[type] = installationDate.toString()
            }
            // Initial assignment to an existing unnumbered formular preserves its rows and baseline.
            if (previous.isNotBlank()) history += AggregateReplacement(type, previous, number, reason.trim(), System.currentTimeMillis(), installationDate.toString())
        }
        db.profileDao().update(profile.copy(aggregateAssignmentsJson = gson.toJson(next),
            aggregateInstalledAtJson = gson.toJson(installed), aggregateHistoryJson = gson.toJson(history),
            hasBeenSynced = false, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun snapshot(profile: Long, type: String, previousJson: String? = null): String {
        val (start, records) = when (type) {
            "Vr80" -> db.vr80StartDao().getByProfile(profile) to db.vr80Dao().getByProfile(profile)
            "Ta14" -> db.ta14StartDao().getByProfile(profile) to db.ta14Dao().getByProfile(profile)
            "Vk250001" -> db.vk250001StartDao().getByProfile(profile) to db.vk250001Dao().getByProfile(profile)
            "Vk250002" -> db.vk250002StartDao().getByProfile(profile) to db.vk250002Dao().getByProfile(profile)
            else -> error(tr(R.string.msg_unknown_component_0a1d7d0))
        }
        return (previousJson?.let { JsonParser.parseString(it).asJsonObject.deepCopy() } ?: JsonObject()).apply {
            add("start", gson.toJsonTree(start)); add("records", gson.toJsonTree(records))
        }.toString()
    }

    private suspend fun load(profile: Long, type: String, json: String?) {
        val data = json?.let { JsonParser.parseString(it).asJsonObject }
        fun <T> start(clazz: Class<T>): T? = data?.get("start")?.takeUnless { it.isJsonNull }?.let { gson.fromJson(it, clazz) }
        fun <T> rows(clazz: Class<T>): List<T> = data?.getAsJsonArray("records")?.map { gson.fromJson(it, clazz) }.orEmpty()
        when (type) {
            "Vr80" -> {
                db.vr80Dao().deleteAllByProfile(profile); db.vr80StartDao().deleteByProfile(profile)
                db.vr80StartDao().insert((start(Vr80StartEntity::class.java) ?: Vr80StartEntity(profileId = profile)).copy(id = 0, profileId = profile))
                rows(Vr80Entity::class.java).forEach { db.vr80Dao().insert(it.copy(id = 0, profileId = profile, isHistorical = true)) }
            }
            "Ta14" -> {
                db.ta14Dao().deleteAllByProfile(profile); db.ta14StartDao().deleteByProfile(profile)
                db.ta14StartDao().insert((start(Ta14StartEntity::class.java) ?: Ta14StartEntity(profileId = profile)).copy(id = 0, profileId = profile))
                rows(Ta14Entity::class.java).forEach { db.ta14Dao().insert(it.copy(id = 0, profileId = profile, isHistorical = true)) }
            }
            "Vk250001" -> {
                db.vk250001Dao().deleteAllByProfile(profile); db.vk250001StartDao().deleteByProfile(profile)
                db.vk250001StartDao().insert((start(Vk250001StartEntity::class.java) ?: Vk250001StartEntity(profileId = profile)).copy(id = 0, profileId = profile))
                rows(Vk250001Entity::class.java).forEach { db.vk250001Dao().insert(it.copy(id = 0, profileId = profile, isHistorical = true)) }
            }
            "Vk250002" -> {
                db.vk250002Dao().deleteAllByProfile(profile); db.vk250002StartDao().deleteByProfile(profile)
                db.vk250002StartDao().insert((start(Vk250002StartEntity::class.java) ?: Vk250002StartEntity(profileId = profile)).copy(id = 0, profileId = profile))
                rows(Vk250002Entity::class.java).forEach { db.vk250002Dao().insert(it.copy(id = 0, profileId = profile, isHistorical = true)) }
            }
        }
    }

    companion object {
        val TYPES = listOf("Vr80", "Ta14", "Vk250001", "Vk250002")
        private fun canonical(type: String) = if (type.startsWith("Vk25000")) "Vk2500" else type
        private fun key(type: String, number: String) = canonical(type) to number
        fun numbers(json: String): Map<String, String> = Gson().fromJson(json,
            object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
    }
}
