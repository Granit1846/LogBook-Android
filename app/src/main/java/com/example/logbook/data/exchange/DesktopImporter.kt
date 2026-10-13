package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import androidx.room.withTransaction
import com.example.logbook.data.backup.BackupManager
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.InputStream
import java.security.MessageDigest

data class DesktopProfilePreview(val key: String, val name: String, val board: String,
    val days: Int, val rows: Int, val warnings: List<String>, val blocked: Boolean)
class PreparedDesktopImport internal constructor(internal val profiles: List<DesktopProfile>, internal val archives: List<DesktopArchive>,
    val preview: List<DesktopProfilePreview>, val sourceHash: String) {
    val archiveCount: Int get() = archives.size
}

class DesktopImporter(context: Context, private val db: AppDatabase) {
    private val backup = BackupManager(context, db)
    suspend fun prepare(input: InputStream): PreparedDesktopImport = withContext(Dispatchers.IO) {
        val bytes = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer); if (count < 0) break
            require(bytes.size().toLong() + count <= 64 * 1024 * 1024) { tr(R.string.msg_the_file_exceeds_64_mb_dd935ee) }
            bytes.write(buffer,0,count)
        }
        val raw = bytes.toByteArray()
        val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(raw)).toString().removePrefix("\uFEFF")
        val root = JsonParser.parseString(text).asJsonObject
        require(root.has("Profiles") && root.get("Profiles").isJsonObject && root.has("Aggregates") && root.get("Aggregates").isJsonObject) {
            tr(R.string.msg_choose_work_data_json_from_the_pc_application_56d0a2b)
        }
        val profiles = DesktopData.obj(root,"Profiles").entrySet().map { (key, value) ->
            try { DesktopData.profile(key,value.asJsonObject) }
            catch (error: Exception) { throw IllegalArgumentException(tr(R.string.msg_profile_1_s_2_s_19cf40d, key, error.message),error) }
        }
        require(profiles.isNotEmpty()) { tr(R.string.msg_the_file_contains_no_profiles_f4bb21e) }
        require(profiles.map { it.sourceKey }.distinct().size == profiles.size) { tr(R.string.msg_the_file_contains_multiple_profiles_for_one_ai_5b502a8) }
        val archives = DesktopData.obj(root,"Aggregates").entrySet().map { (key, value) ->
            try { DesktopData.archive(value.asJsonObject) }
            catch (error: Exception) { throw IllegalArgumentException(tr(R.string.msg_archive_1_s_2_s_169340a, key, error.message),error) }
        }
        require(archives.map { it.type to it.number }.distinct().size == archives.size) { tr(R.string.msg_duplicate_component_archive_ab877da) }
        val existing = db.profileDao().getAll()
        val preview = profiles.map { p -> DesktopProfilePreview(p.sourceKey,p.profile.name,p.profile.boardNumber,
            p.days.size,p.rows.values.sumOf { it.size },p.warnings,
            conflicts(p,existing) || db.desktopImportDao().get(p.sourceKey) != null) }
        val hash = MessageDigest.getInstance("SHA-256").digest(raw).joinToString("") { "%02x".format(it) }
        PreparedDesktopImport(profiles,archives,preview,hash)
    }
    private fun assigned(profile: ProfileEntity): Set<Pair<String,String>> = com.example.logbook.data.local.AggregateManager.numbers(profile.aggregateAssignmentsJson)
        .filter { it.key != "Chast1" && it.value.isNotBlank() }.map { (type,number) -> (if (type.startsWith("Vk")) "Vk2500" else type) to number }.toSet()
    private fun conflicts(p: DesktopProfile, existing: List<ProfileEntity>): Boolean = existing.any {
        it.name == p.profile.name || assigned(it).intersect(assigned(p.profile)).isNotEmpty() || (p.profile.boardNumber.isNotBlank() &&
            (it.boardNumber == p.profile.boardNumber || runCatching { com.example.logbook.data.local.AggregateManager.numbers(it.aggregateAssignmentsJson)["Chast1"] == p.profile.boardNumber }.getOrDefault(false)))
    }

    suspend fun apply(prepared: PreparedDesktopImport, selected: Set<String>): Int = withContext(Dispatchers.IO + NonCancellable) {
        require(selected.isNotEmpty()) { tr(R.string.msg_choose_profiles_cb6e3a2) }
        require(selected.all { key -> prepared.profiles.any { it.sourceKey == key } }) { tr(R.string.msg_unknown_profile_393c829) }
        val chosen = prepared.profiles.filter { it.sourceKey in selected }
        db.withTransaction {
            val existing = db.profileDao().getAll()
            require(chosen.all { !conflicts(it,existing) && db.desktopImportDao().get(it.sourceKey) == null }) {
                tr(R.string.msg_the_profile_already_exists_current_data_was_pr_ffad327)
            }
            val assignments = chosen.flatMap { assigned(it.profile) }
            require(assignments.distinct().size == assignments.size) { tr(R.string.msg_one_component_is_assigned_to_multiple_imported_065e9b2) }
            backup.preserveBeforeChange()
            chosen.forEach { p ->
                val id = db.profileDao().insert(p.profile)
                p.starts.forEach { (type, data) -> insertFormular(id,type,data,true) }
                p.rows.forEach { (type, rows) -> rows.forEach { insertFormular(id,type,it,false) } }
                p.days.forEach { d ->
                    val dayId = db.dayDao().insert(d.day.copy(profileId=id))
                    d.launches.forEach { db.launchDao().insert(it.copy(dayId=dayId)) }
                    db.snkDao().insert(d.snk.copy(dayId=dayId))
                }
                db.desktopImportDao().insert(DesktopImportEntity(profileId=id,sourceKey=p.sourceKey,sharedId=p.sharedId,
                    sourceHash=prepared.sourceHash,originalJson=p.original.toString()))
            }
            prepared.archives.forEach { archive ->
                if (db.aggregateArchiveDao().get(archive.type,archive.number) == null)
                    db.aggregateArchiveDao().save(AggregateArchiveEntity(type=archive.type,number=archive.number,formularJson=archive.json,savedAt=System.currentTimeMillis()))
            }
            chosen.size
        }
    }

    private suspend fun insertFormular(profile: Long, type: String, raw: com.google.gson.JsonObject, start: Boolean) {
        val value = raw.deepCopy().apply { addProperty("profileId",profile) }
        val gson = DesktopData.gson
        when (type) {
            "Chast1" -> if (start) db.chast1StartDao().insert(gson.fromJson(value,Chast1StartEntity::class.java)) else db.chast1Dao().insert(gson.fromJson(value,Chast1Entity::class.java))
            "Vr80" -> if (start) db.vr80StartDao().insert(gson.fromJson(value,Vr80StartEntity::class.java)) else db.vr80Dao().insert(gson.fromJson(value,Vr80Entity::class.java))
            "Ta14" -> if (start) db.ta14StartDao().insert(gson.fromJson(value,Ta14StartEntity::class.java)) else db.ta14Dao().insert(gson.fromJson(value,Ta14Entity::class.java))
            "Vk250001" -> if (start) db.vk250001StartDao().insert(gson.fromJson(value,Vk250001StartEntity::class.java)) else db.vk250001Dao().insert(gson.fromJson(value,Vk250001Entity::class.java))
            "Vk250002" -> if (start) db.vk250002StartDao().insert(gson.fromJson(value,Vk250002StartEntity::class.java)) else db.vk250002Dao().insert(gson.fromJson(value,Vk250002Entity::class.java))
        }
    }
}
