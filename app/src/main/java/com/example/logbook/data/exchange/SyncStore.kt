package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.withTransaction
import com.example.logbook.data.backup.BackupManager
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.google.gson.*
import java.util.UUID

data class SharedProfilePacket(val profileId: String,val name: String,val boardNumber: String,val payload: String)

/** One coherent checkpoint. SQL IDs never leave the device; the wire uses stable UUIDs. */
class SyncStore(private val context: Context, private val db: AppDatabase) : SyncLocal {
    companion object {
        val TABLES = listOf("profiles","days","launches","snk_values","chast1_start","chast1_records",
            "vr80_start","vr80_records","ta14_start","ta14_records","vk250001_start","vk250001_records",
            "vk250002_start","vk250002_records","aggregate_archives","desktop_imports")
        private val LOCAL_PROFILE = setOf("selectedTab","lastOpenedDate","createdAt","updatedAt","hasBeenSynced",
            "aggregateSortMode","aggregateViewMode","oilSortMode","oilViewMode")
        fun same(a: String, b: String): Boolean = a.isNotBlank() && b.isNotBlank() && normalized(a) == normalized(b)
        private fun normalized(value: String): JsonElement = JsonParser.parseString(value).also { root ->
            root.asJsonObject.getAsJsonObject("tables")?.getAsJsonArray("profiles")?.forEach { item ->
                val profile = item.asJsonObject
                if (!profile.has("maintenanceWorksJson")) profile.addProperty("maintenanceWorksJson", "[]")
                if (!profile.has("defectTasksJson")) profile.addProperty("defectTasksJson", "[]")
            }
        }
    }
    private val syncDir = java.io.File(context.filesDir, "sync_data").apply { mkdirs() }

    private fun sanitizeKey(key: String): String =
        key.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private fun baseFile(endpoint: String, account: String): java.io.File =
        java.io.File(syncDir, "base_${sanitizeKey(endpoint)}_${sanitizeKey(account)}.json")

    private fun pendingFile(endpoint: String, account: String): java.io.File =
        java.io.File(syncDir, "pending_${sanitizeKey(endpoint)}_${sanitizeKey(account)}.json")

    private fun identityFile(): java.io.File =
        java.io.File(syncDir, "identities.json")

    private fun writeTextSafe(file: java.io.File, text: String) {
        runCatching {
            val temp = java.io.File(file.parentFile, "${file.name}.tmp")
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    private fun readTextSafe(file: java.io.File): String {
        return if (file.exists()) runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("") else ""
    }

    suspend fun state(): SyncStateEntity {
        val entity = db.syncStateDao().get() ?: SyncStateEntity()
        val idFile = identityFile()
        val identities = if (idFile.exists()) readTextSafe(idFile) else entity.identityJson
        val base = baseFile(entity.endpoint, entity.accountId)
        val pending = pendingFile(entity.endpoint, entity.accountId)
        return entity.copy(
            identityJson = identities.ifBlank { "{}" },
            baseJson = if (base.exists()) readTextSafe(base) else entity.baseJson,
            pendingJson = if (pending.exists()) readTextSafe(pending) else entity.pendingJson
        )
    }

    suspend fun save(state: SyncStateEntity) {
        if (state.identityJson.isNotBlank() && state.identityJson != "{}") {
            writeTextSafe(identityFile(), state.identityJson)
        }
        if (state.baseJson.isNotBlank()) {
            writeTextSafe(baseFile(state.endpoint, state.accountId), state.baseJson)
        }
        if (state.pendingJson.isNotBlank()) {
            writeTextSafe(pendingFile(state.endpoint, state.accountId), state.pendingJson)
        } else if (state.pendingId.isBlank() && state.endpoint.isNotBlank()) {
            pendingFile(state.endpoint, state.accountId).delete()
        }
        db.syncStateDao().save(state.copy(
            identityJson = "{}",
            baseJson = "",
            pendingJson = ""
        ))
    }

    private fun checkpointKey(endpoint: String, account: String) = JsonArray().apply {
        add(endpoint); add(account)
    }.toString()

    override suspend fun checkpoint(endpoint: String, account: String): SyncStateEntity {
        val shared = state()
        val checkpoints = JsonParser.parseString(shared.checkpointsJson).asJsonObject
        val saved = checkpoints.get(checkpointKey(endpoint, account))
        val base = readTextSafe(baseFile(endpoint, account))
        val pending = readTextSafe(pendingFile(endpoint, account))
        val entity = if (saved != null) {
            Gson().fromJson(saved, SyncStateEntity::class.java)
        } else if (shared.endpoint == endpoint && shared.accountId == account) {
            shared
        } else {
            SyncStateEntity(
                endpoint = endpoint,
                accountId = account,
                identityJson = shared.identityJson,
                checkpointsJson = shared.checkpointsJson,
                baseJson = base,
                pendingJson = pending
            )
        }
        val actualPendingJson = pending.ifBlank { entity.pendingJson }
        val isValidPending = actualPendingJson.isNotBlank() && actualPendingJson.contains("\"LogBookExchange\"")
        val actualPendingId = if (isValidPending) entity.pendingId else ""
        val actualPendingRevision = if (isValidPending) entity.pendingRevision else 0L

        return entity.copy(
            identityJson = shared.identityJson,
            checkpointsJson = shared.checkpointsJson,
            baseJson = base.ifBlank { entity.baseJson },
            pendingId = actualPendingId,
            pendingJson = if (isValidPending) actualPendingJson else "",
            pendingRevision = actualPendingRevision
        )
    }

    override suspend fun saveCheckpoint(checkpoint: SyncStateEntity): Unit = db.withTransaction {
        val shared = state()
        val checkpoints = JsonParser.parseString(shared.checkpointsJson).asJsonObject
        if (checkpoint.baseJson.isNotBlank()) {
            writeTextSafe(baseFile(checkpoint.endpoint, checkpoint.accountId), checkpoint.baseJson)
        }
        if (checkpoint.pendingJson.isNotBlank()) {
            writeTextSafe(pendingFile(checkpoint.endpoint, checkpoint.accountId), checkpoint.pendingJson)
        } else {
            pendingFile(checkpoint.endpoint, checkpoint.accountId).delete()
        }

        // Preserve the checkpoint from version 26 on its first switch to another transport.
        if (shared.endpoint.isNotBlank()) {
            val oldKey = checkpointKey(shared.endpoint, shared.accountId)
            if (!checkpoints.has(oldKey)) checkpoints.add(oldKey, Gson().toJsonTree(shared).asJsonObject.apply {
                remove("identityJson"); remove("checkpointsJson"); remove("baseJson"); remove("pendingJson")
            })
        }
        checkpoints.add(checkpointKey(checkpoint.endpoint, checkpoint.accountId), Gson().toJsonTree(checkpoint).asJsonObject.apply {
            remove("identityJson"); remove("checkpointsJson"); remove("baseJson"); remove("pendingJson")
        })
        val toSave = checkpoint.copy(
            identityJson = shared.identityJson,
            checkpointsJson = checkpoints.toString(),
            baseJson = "",
            pendingJson = ""
        )
        db.syncStateDao().save(toSave)
    }
    override suspend fun snapshot(): String = db.withTransaction {
        val state=state(); val identities=JsonParser.parseString(state.identityJson).asJsonObject
        val sql=db.openHelper.readableDatabase
        val tables=JsonObject()
        TABLES.forEach { table ->
            val rows=mutableListOf<JsonObject>()
            sql.query("SELECT * FROM `$table` ORDER BY id").use { cursor ->
                while(cursor.moveToNext()) {
                    val row=JsonObject()
                    cursor.columnNames.forEachIndexed { index,column ->
                        if(table=="profiles" && column in LOCAL_PROFILE) return@forEachIndexed
                        when(cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> row.add(column,JsonNull.INSTANCE)
                            Cursor.FIELD_TYPE_INTEGER -> row.addProperty(column,cursor.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> row.addProperty(column,java.math.BigDecimal.valueOf(cursor.getDouble(index)).toPlainString())
                            Cursor.FIELD_TYPE_STRING -> row.addProperty(column,cursor.getString(index))
                            else -> error(tr(R.string.msg_unsupported_field_1_s_2_s_6757e0a, table, column))
                        }
                    }
                    val localId=row.get("id").asLong
                    fun identity(parent: String, id: Long): String {
                        val key="$parent:$id"
                        if(!identities.has(key)) {
                            val imported = if(parent=="profiles") sql.query("SELECT sharedId FROM desktop_imports WHERE profileId=?",arrayOf(id)).use { if(it.moveToFirst()) it.getString(0) else null } else if(parent=="aggregate_archives") sql.query("SELECT formularJson FROM aggregate_archives WHERE id=?",arrayOf(id)).use { if(it.moveToFirst()) JsonParser.parseString(it.getString(0)).asJsonObject.get("sharedId")?.asString else null } else null
                            identities.addProperty(key,imported ?: UUID.randomUUID().toString())
                        }
                        return identities.get(key).asString
                    }
                    row.addProperty("id",identity(table,localId))
                    if(row.has("profileId")) row.addProperty("profileId",identity("profiles",row.get("profileId").asLong))
                    if(row.has("dayId")) row.addProperty("dayId",identity("days",row.get("dayId").asLong))
                    if(row.has("date")) RecalcCommonDate.parse(row.get("date").asString)?.let { row.addProperty("date",it) }
                    rows.add(row)
                }
            }
            require(rows.map { it.get("id").asString }.distinct().size==rows.size) { tr(R.string.msg_duplicate_identifier_in_1_s_e6c61be, table) }
            tables.add(table,JsonArray().apply { rows.sortedBy { it.get("id").asString }.forEach(::add) })
        }
        save(state.copy(identityJson=identities.toString()))
        JsonObject().apply { addProperty("format","LogBookExchange");addProperty("version",1);add("tables",tables) }.toString()
    }

    /** Builds independent, valid exchange packets so the shared server never has to replace a whole book. */
    suspend fun sharedProfilePackets(): List<SharedProfilePacket> {
        val root=JsonParser.parseString(snapshot()).asJsonObject
        val tables=root.getAsJsonObject("tables")
        return tables.getAsJsonArray("profiles").map { token ->
            val profile=token.asJsonObject;val profileId=profile.get("id").asString
            val dayIds=tables.getAsJsonArray("days").map { it.asJsonObject }
                .filter { it.get("profileId").asString==profileId }.map { it.get("id").asString }.toSet()
            val aggregateNumbers=linkedSetOf<String>()
            runCatching { JsonParser.parseString(profile.get("aggregateAssignmentsJson").asString).asJsonObject.entrySet()
                .mapNotNullTo(aggregateNumbers) { it.value.takeIf(JsonElement::isJsonPrimitive)?.asString?.takeIf(String::isNotBlank) } }
            runCatching { JsonParser.parseString(profile.get("aggregateHistoryJson").asString).asJsonArray.forEach { item ->
                item.asJsonObject.let { history -> listOf("oldNumber","newNumber").forEach { key ->
                    history.get(key)?.takeIf(JsonElement::isJsonPrimitive)?.asString?.takeIf(String::isNotBlank)?.let(aggregateNumbers::add)
                } }
            } }
            val selected=JsonObject()
            TABLES.forEach { table ->
                val rows=tables.getAsJsonArray(table).map { it.asJsonObject }.filter { row -> when {
                    table=="profiles" -> row.get("id").asString==profileId
                    table=="launches" || table=="snk_values" -> row.get("dayId").asString in dayIds
                    table=="aggregate_archives" -> row.get("number").asString in aggregateNumbers
                    row.has("profileId") -> row.get("profileId").asString==profileId
                    else -> false
                } }
                selected.add(table,JsonArray().apply { rows.forEach { add(it.deepCopy()) } })
            }
            val packet=JsonObject().apply { addProperty("format","LogBookExchange");addProperty("version",1);add("tables",selected) }
            SharedProfilePacket(profileId,profile.get("name").asString,profile.get("boardNumber").asString,packet.toString())
        }
    }

    private fun combinedWithProfiles(currentPayload: String,incomingPayloads: List<String>): String {
        val current=JsonParser.parseString(currentPayload).asJsonObject
        val target=current.getAsJsonObject("tables")
        incomingPayloads.forEach { payload ->
            val sourceRoot=JsonParser.parseString(payload).asJsonObject
            require(sourceRoot.get("format")?.asString=="LogBookExchange" && sourceRoot.get("version")?.asInt==1)
            val source=sourceRoot.getAsJsonObject("tables")
            require(source.keySet()==TABLES.toSet())
            val profiles=source.getAsJsonArray("profiles");require(profiles.size()==1)
            val profileId=profiles[0].asJsonObject.get("id").asString
            require(UUID.fromString(profileId).toString()==profileId)
            val oldDayIds=target.getAsJsonArray("days").map { it.asJsonObject }
                .filter { it.get("profileId").asString==profileId }.map { it.get("id").asString }.toSet()
            TABLES.filter { it!="aggregate_archives" }.forEach { table ->
                val kept=target.getAsJsonArray(table).map { it.asJsonObject }.filterNot { row -> when {
                    table=="profiles" -> row.get("id").asString==profileId
                    table=="launches" || table=="snk_values" -> row.get("dayId").asString in oldDayIds
                    row.has("profileId") -> row.get("profileId").asString==profileId
                    else -> false
                } }
                target.add(table,JsonArray().apply { kept.forEach { add(it) } })
            }
            val incomingProfile=profiles[0].asJsonObject.deepCopy()
            val originalName=incomingProfile.get("name").asString
            if(target.getAsJsonArray("profiles").map { it.asJsonObject }.any { it.get("id").asString!=profileId && it.get("name").asString==originalName })
                incomingProfile.addProperty("name","$originalName (${profileId.take(8)})")
            TABLES.filter { it!="aggregate_archives" }.forEach { table ->
                val destination=target.getAsJsonArray(table)
                if(table=="profiles") destination.add(incomingProfile)
                else source.getAsJsonArray(table).forEach { destination.add(it.deepCopy()) }
            }
            source.getAsJsonArray("aggregate_archives").map { it.asJsonObject }.forEach { incoming ->
                val filtered=target.getAsJsonArray("aggregate_archives").map { it.asJsonObject }.filterNot { existing ->
                    existing.get("id").asString==incoming.get("id").asString ||
                        existing.get("type").asString==incoming.get("type").asString && existing.get("number").asString==incoming.get("number").asString
                }
                target.add("aggregate_archives",JsonArray().apply { filtered.forEach { add(it) };add(incoming.deepCopy()) })
            }
        }
        TABLES.forEach { table ->
            target.add(table,JsonArray().apply { target.getAsJsonArray(table).map { it.asJsonObject }
                .sortedBy { it.get("id").asString }.forEach { add(it) } })
        }
        return current.toString()
    }

    /** Imports only selected shared profiles and preserves every other local profile. */
    suspend fun importSharedProfiles(payloads: List<String>): String = db.withTransaction {
        require(payloads.isNotEmpty())
        val currentPayload=snapshot();val combined=combinedWithProfiles(currentPayload,payloads)
        val decoded=decode(combined)
        val test=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try { test.withTransaction { replace(test,decoded) } } finally { test.close() }
        val backup=BackupManager(context,db).preserveBeforeSync()
        replace(db,decoded)
        val current=state();save(current.copy(identityJson=decoded.identities))
        backup
    }
    private object RecalcCommonDate {
        fun parse(value: String) = com.example.logbook.data.local.recalc.RecalcCommon.parseDate(value)?.toString()
        fun local(value: String) = if(value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            com.example.logbook.data.local.recalc.RecalcCommon.formatFormularDate(java.time.LocalDate.parse(value)) else value
    }
    private data class Decoded(val rows: Map<String,List<JsonObject>>,val identities: String)
    private suspend fun decode(payload: String): Decoded {
        require(payload.toByteArray().size <= 64*1024*1024) { tr(R.string.msg_the_packet_exceeds_64_mb_fd0c2a5) }
        val root=JsonParser.parseString(payload).asJsonObject
        require(root.get("format")?.asString=="LogBookExchange" && root.get("version")?.asInt==1) { tr(R.string.msg_unsupported_exchange_format_ca1b035) }
        val tables=root.getAsJsonObject("tables");require(tables.keySet()==TABLES.toSet()) { tr(R.string.msg_incomplete_synchronization_packet_a8d40f9) }
        val identities=JsonParser.parseString(state().identityJson).asJsonObject
        val mappings=mutableMapOf<String,Map<String,Long>>()
        val decoded=mutableMapOf<String,List<JsonObject>>()
        TABLES.forEach { table ->
            val raw=tables.getAsJsonArray(table).map { it.asJsonObject }
            val reverse=identities.entrySet().filter { it.key.startsWith("$table:") }.associate { it.value.asString to it.key.substringAfter(':').toLong() }.toMutableMap()
            val sequence=db.openHelper.readableDatabase.query("SELECT seq FROM sqlite_sequence WHERE name=?",arrayOf(table)).use { if(it.moveToFirst()) it.getLong(0) else 0L }
            var next=maxOf(sequence,reverse.values.maxOrNull() ?: 0,db.openHelper.readableDatabase.query("SELECT coalesce(max(id),0) FROM `$table`").use { it.moveToFirst();it.getLong(0) })
            raw.forEach { row ->
                val uuid=row.get("id").asString;require(UUID.fromString(uuid).toString()==uuid) { tr(R.string.msg_invalid_uuid_e4e31cf) }
                if(uuid !in reverse) { next=Math.addExact(next,1L);reverse[uuid]=next;identities.addProperty("$table:$next",uuid) }
            }
            require(raw.map { it.get("id").asString }.distinct().size==raw.size) { tr(R.string.msg_duplicate_identifier_6c6f05b) }
            mappings[table]=reverse
            decoded[table]=raw.map { wire -> wire.deepCopy().apply {
                addProperty("id",reverse.getValue(wire.get("id").asString))
                if(has("profileId")) addProperty("profileId",mappings.getValue("profiles")[get("profileId").asString] ?: error(tr(R.string.msg_profile_not_found_0e520dc)))
                if(has("dayId")) addProperty("dayId",mappings.getValue("days")[get("dayId").asString] ?: error(tr(R.string.msg_day_not_found_517a3eb)))
                if(has("date")) addProperty("date",RecalcCommonDate.local(get("date").asString))
                if(table=="profiles") {
                    val rawMw = if(has("maintenanceWorksJson")) get("maintenanceWorksJson").asString else "[]"
                    val rawDt = if(has("defectTasksJson")) get("defectTasksJson").asString else "[]"
                    addProperty("maintenanceWorksJson", com.example.logbook.data.model.MaintenanceJournal.normalizeWorks(rawMw))
                    addProperty("defectTasksJson", com.example.logbook.data.model.MaintenanceJournal.normalizeTasks(rawDt))
                    val defaults=Gson().toJsonTree(ProfileEntity()).asJsonObject
                    db.openHelper.readableDatabase.query("SELECT * FROM profiles WHERE id=?",arrayOf(get("id").asLong)).use { c ->
                        val exists=c.moveToFirst()
                        LOCAL_PROFILE.forEach { key ->
                            if(exists) { val index=c.getColumnIndexOrThrow(key)
                                if(c.getType(index)==Cursor.FIELD_TYPE_STRING) addProperty(key,c.getString(index)) else addProperty(key,c.getLong(index))
                            } else { val value=defaults.get(key); if(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) addProperty(key,if(value.asBoolean) 1 else 0) else add(key,value) }
                        }
                    }
                    for(key in listOf("aggregateAssignmentsJson","aggregateInstalledAtJson","resourcesJson","oilsJson","aggregateHistoryJson","aggregateGroupsJson","oilGroupsJson","maintenanceWorksJson","defectTasksJson")) {
                        val value=JsonParser.parseString(get(key).asString)
                        require(if(key.startsWith("aggregateAssignments") || key.startsWith("aggregateInstalled")) value.isJsonObject else value.isJsonArray) { tr(R.string.msg_invalid_field_1_s_e1b7d0d, key) }
                    }
                    com.example.logbook.data.model.MaintenanceRepository.validate(ProfileEntity(
                        maintenanceWorksJson = get("maintenanceWorksJson").asString,
                        defectTasksJson = get("defectTasksJson").asString))
                }
            } }
        }
        return Decoded(decoded,identities.toString())
    }
    private fun replace(target: AppDatabase, decoded: Decoded) {
        val sql=target.openHelper.writableDatabase
        TABLES.reversed().forEach { sql.execSQL("DELETE FROM `$it`") }
        TABLES.forEach { table ->
            val columns=linkedMapOf<String,String>()
            sql.query("PRAGMA table_info(`$table`)").use { while(it.moveToNext()) columns[it.getString(1)]=it.getString(2) }
            val query="INSERT INTO `$table` (${columns.keys.joinToString(",") { "`$it`" }}) VALUES (${columns.keys.joinToString(",") { "?" }})"
            decoded.rows.getValue(table).forEach { row ->
                require(row.keySet()==columns.keys) { tr(R.string.msg_incompatible_fields_in_1_s_0d6a0c9, table) }
                val values=columns.map { (key,type) -> val v=row.get(key);require(!v.isJsonNull) { tr(R.string.msg_empty_field_1_s_2_s_0c25048, table, key) }
                    when(type) {
                        "INTEGER" -> { require(v.asJsonPrimitive.isNumber);v.asBigDecimal.longValueExact() }
                        "REAL" -> java.math.BigDecimal(v.asString).toDouble().also { require(it.isFinite()) }
                        "TEXT" -> { require(v.asJsonPrimitive.isString);v.asString }
                        else -> error(tr(R.string.msg_unknown_type_1_s_bb477b6, type))
                    }
                }.toTypedArray()
                sql.execSQL(query,values)
            }
        }
        sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { tr(R.string.msg_data_relationships_are_invalid_9049aea) } }
    }
    override suspend fun download(payload: String, expectedLocal: String, revision: Long, endpoint: String, account: String): String = db.withTransaction {
        require(same(snapshot(),expectedLocal)) { tr(R.string.msg_data_changed_during_synchronization_retry_the__ebac49d) }
        val decoded=decode(payload)
        val test=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try { test.withTransaction { replace(test,decoded) } } finally { test.close() }
        val backup=BackupManager(context,db).preserveBeforeSync()
        replace(db,decoded)
        val current=state()
        save(current.copy(identityJson=decoded.identities))
        saveCheckpoint(current.copy(endpoint=endpoint,accountId=account,identityJson=decoded.identities,baseRevision=revision,
            baseJson=payload,pendingId="",pendingJson="",pendingRevision=0,lastSuccess=System.currentTimeMillis()))
        backup
    }
}
