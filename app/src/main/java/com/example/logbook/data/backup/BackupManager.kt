package com.example.logbook.data.backup

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.AtomicFile
import androidx.room.Room
import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/** Logical snapshot: includes committed WAL changes and preserves manual formular edits. */
class BackupManager(context: Context, private val database: AppDatabase) {
    private val context = context.applicationContext
    private val zoom = this.context.getSharedPreferences("table_zoom", Context.MODE_PRIVATE)
    private val safetyFile = AtomicFile(File(this.context.filesDir, "backups/before-restore.json"))
    private val gson = Gson()

    class PreparedBackup internal constructor(
        internal val payload: JsonObject,
        val createdAt: Long,
        val profiles: Int,
        val days: Int
    )

    fun hasSafetyCopy(): Boolean = safetyFile.baseFile.exists()

    suspend fun export(output: OutputStream) = withContext(Dispatchers.IO) {
        output.write(snapshot())
        output.flush()
    }

    suspend fun exportSafetyCopy(output: OutputStream) = withContext(Dispatchers.IO) {
        safetyFile.openRead().use { it.copyTo(output) }
        output.flush()
    }

    suspend fun prepare(input: InputStream): PreparedBackup = withContext(Dispatchers.IO) {
        val bytes = input.readBytesLimited()
        val root = try { JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject }
        catch (e: Exception) { throw IllegalArgumentException(tr(R.string.msg_could_not_read_the_backup_file_869ea6b), e) }
        require(root.get("format")?.asString == "AndroidLogBook") { tr(R.string.msg_this_is_not_an_android_logbook_backup_e6335f0) }
        require(root.get("version")?.asInt == 1) { tr(R.string.msg_this_backup_version_is_not_supported_84392cc) }
        val original = root.getAsJsonObject("data") ?: error(tr(R.string.msg_the_backup_contains_no_data_57ce094))
        val payload = original.deepCopy()
        require(root.get("sha256")?.asString == checksum(gson.toJson(payload).toByteArray(Charsets.UTF_8))) {
            tr(R.string.msg_the_file_is_damaged_checksum_mismatch_f31ccfb)
        }
        if (payload.get("databaseVersion")?.asInt == 23 && database.openHelper.readableDatabase.version >= 24) {
            val tables = payload.getAsJsonObject("tables")
            require(tables.keySet() == TABLES.filter { it != "aggregate_archives" && it != "desktop_imports" && it != "sync_state" }.toSet()) { tr(R.string.msg_incomplete_set_of_tables_9e06539) }
            tables.getAsJsonArray("profiles").forEach { it.asJsonObject.addProperty("aggregateInstalledAtJson", "{}") }
            listOf("vr80_records", "ta14_records", "vk250001_records", "vk250002_records").forEach { table ->
                tables.getAsJsonArray(table).forEach { it.asJsonObject.addProperty("isHistorical", 0) }
            }
            tables.add("aggregate_archives", JsonArray())
            payload.addProperty("databaseVersion", 24)
        }
        if (payload.get("databaseVersion")?.asInt == 24 && database.openHelper.readableDatabase.version >= 25) {
            val tables = payload.getAsJsonObject("tables")
            require(tables.keySet() == TABLES.filter { it != "desktop_imports" && it != "sync_state" }.toSet()) { tr(R.string.msg_incomplete_set_of_tables_9e06539) }
            tables.add("desktop_imports", JsonArray())
            payload.addProperty("databaseVersion", 25)
        }
        if (payload.get("databaseVersion")?.asInt == 25 && database.openHelper.readableDatabase.version >= 26) {
            val tables = payload.getAsJsonObject("tables")
            require(tables.keySet() == TABLES.filter { it != "sync_state" }.toSet()) { tr(R.string.msg_incomplete_set_of_tables_9e06539) }
            tables.add("sync_state", JsonArray()); payload.addProperty("databaseVersion", 26)
        }
        if (payload.get("databaseVersion")?.asInt == 26 && database.openHelper.readableDatabase.version >= 27) {
            payload.getAsJsonObject("tables").getAsJsonArray("sync_state").forEach {
                it.asJsonObject.addProperty("checkpointsJson", "{}")
            }
            payload.addProperty("databaseVersion", 27)
        }
        require(payload.get("databaseVersion")?.asInt == database.openHelper.readableDatabase.version) {
            tr(R.string.msg_the_backup_was_created_for_another_database_ve_ec05c04)
        }
        validateZoom(payload.getAsJsonObject("zoom"))
        // Validate columns, types, required values, uniqueness and links in a separate database.
        val checkDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            checkDb.withTransaction { replaceTables(checkDb, payload.getAsJsonObject("tables")) }
        } catch (e: Exception) {
            throw IllegalArgumentException(tr(R.string.msg_invalid_backup_data_current_data_was_preserved_e6b5e13), e)
        } finally { checkDb.close() }
        val tables = payload.getAsJsonObject("tables")
        PreparedBackup(payload, payload.get("createdAt").asLong,
            tables.getAsJsonArray("profiles").size(), tables.getAsJsonArray("days").size())
    }

    suspend fun restore(backup: PreparedBackup) = withContext(Dispatchers.IO + NonCancellable) {
        val oldZoom = zoom.all.mapValues { (_, value) -> (value as Number).toFloat() }
        try {
            database.withTransaction {
                // Keep the same write lock from the safety snapshot through replacement.
                // A failed safety write aborts before touching the current database.
                val previous = snapshot()
                val stream = safetyFile.startWrite()
                try { stream.write(previous); safetyFile.finishWrite(stream) }
                catch (e: Exception) { safetyFile.failWrite(stream); throw e }
                replaceTables(database, backup.payload.getAsJsonObject("tables"))
                writeZoom(backup.payload.getAsJsonObject("zoom").entrySet().associate {
                    it.key to it.value.asFloat
                })
            }
        } catch (e: Exception) {
            writeZoom(oldZoom)
            throw e
        }
    }

    suspend fun preserveBeforeChange() = withContext(Dispatchers.IO) {
        val bytes = snapshot()
        val stream = safetyFile.startWrite()
        try { stream.write(bytes); safetyFile.finishWrite(stream) }
        catch (e: Exception) { safetyFile.failWrite(stream); throw e }
    }

    suspend fun preserveBeforeSync(): String = withContext(Dispatchers.IO) {
        val name="before-sync-${System.currentTimeMillis()}-${java.util.UUID.randomUUID()}.json"
        val target=AtomicFile(File(context.filesDir,"backups/sync/$name"))
        val bytes=snapshot()
        val output=target.startWrite()
        try {output.write(bytes);target.finishWrite(output)}
        catch(error: Exception){target.failWrite(output);throw error}
        preserveBeforeChange()
        pruneSyncBackups(target.baseFile.parentFile)
        name
    }

    // Each download keeps a full copy; only the newest ones are worth the space. Manual backups live elsewhere.
    private fun pruneSyncBackups(directory: File?) {
        directory?.listFiles { file -> file.name.startsWith("before-sync-") && file.name.endsWith(".json") }
            ?.sortedByDescending { it.name.removePrefix("before-sync-").substringBefore('-').toLongOrNull() ?: 0L }
            ?.drop(MAX_SYNC_BACKUPS)
            ?.forEach { it.delete() }
    }

    private suspend fun snapshot(): ByteArray {
        val payload = database.withTransaction {
            val db = database.openHelper.readableDatabase
            JsonObject().apply {
                addProperty("databaseVersion", db.version)
                addProperty("createdAt", System.currentTimeMillis())
                add("tables", JsonObject().apply {
                    TABLES.forEach { table ->
                        val rows = JsonArray()
                        db.query("SELECT * FROM `$table` ORDER BY id").use { cursor ->
                            while (cursor.moveToNext()) {
                                val row = JsonObject()
                                cursor.columnNames.forEachIndexed { index, name ->
                                    when (cursor.getType(index)) {
                                        Cursor.FIELD_TYPE_NULL -> row.add(name, JsonNull.INSTANCE)
                                        Cursor.FIELD_TYPE_INTEGER -> row.addProperty(name, cursor.getLong(index))
                                        Cursor.FIELD_TYPE_FLOAT -> row.addProperty(name, cursor.getDouble(index))
                                        Cursor.FIELD_TYPE_STRING -> row.addProperty(name, cursor.getString(index))
                                        else -> error(tr(R.string.msg_unsupported_data_type_5ebf3b6))
                                    }
                                }
                                rows.add(row)
                            }
                        }
                        add(table, rows)
                    }
                })
                add("zoom", JsonObject().apply {
                    zoom.all.toSortedMap().forEach { (key, value) -> addProperty(key, (value as Number).toFloat()) }
                })
            }
        }
        return gson.toJson(JsonObject().apply {
            addProperty("format", "AndroidLogBook")
            addProperty("version", 1)
            addProperty("sha256", checksum(gson.toJson(payload).toByteArray(Charsets.UTF_8)))
            add("data", payload)
        }).toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_BYTES) { tr(R.string.msg_the_backup_exceeds_the_64_mb_limit_c48508c) }
        }
    }

    private fun replaceTables(target: AppDatabase, tables: JsonObject?) {
        require(tables != null && tables.keySet() == TABLES.toSet()) { tr(R.string.msg_incomplete_set_of_tables_9e06539) }
        val db = target.openHelper.writableDatabase
        TABLES.reversed().forEach { db.execSQL("DELETE FROM `$it`") }
        // Restore autoincrement counters from imported IDs, independent of the replaced data.
        db.execSQL("DELETE FROM sqlite_sequence")
        TABLES.forEach { table ->
            val columns = mutableMapOf<String, Pair<String, Boolean>>()
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                while (cursor.moveToNext()) {
                    columns[cursor.getString(1)] = cursor.getString(2) to (cursor.getInt(3) != 0)
                }
            }
            val insertSql = "INSERT INTO `$table` (${columns.keys.joinToString(",") { "`$it`" }}) " +
                "VALUES (${columns.keys.joinToString(",") { "?" }})"
            tables.getAsJsonArray(table).forEach { element ->
                val row = element.asJsonObject
                require(row.keySet() == columns.keys) { tr(R.string.msg_invalid_fields_in_table_1_s_bcd4f5c, table) }
                val values = ContentValues()
                columns.forEach { (name, column) ->
                    val value = row.get(name)
                    if (value.isJsonNull) {
                        require(!column.second) { tr(R.string.msg_required_field_1_s_is_missing_f7f767f, name) }
                        values.putNull(name)
                    } else {
                        val primitive = value.asJsonPrimitive
                        when (column.first) {
                            "INTEGER" -> {
                                require(primitive.isNumber)
                                values.put(name, primitive.asBigDecimal.longValueExact())
                            }
                            "REAL" -> {
                                require(primitive.isNumber && primitive.asDouble.isFinite())
                                values.put(name, primitive.asDouble)
                            }
                            "TEXT" -> { require(primitive.isString); values.put(name, primitive.asString) }
                            else -> error(tr(R.string.msg_unsupported_field_1_s_1e70ab5, name))
                        }
                    }
                }
                // Quote column names: LaunchEntity contains the SQL keyword `order`.
                db.execSQL(insertSql, columns.keys.map { values.get(it) }.toTypedArray())
            }
        }
        db.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { tr(R.string.msg_data_relationships_are_invalid_9049aea) } }
    }

    private fun validateZoom(values: JsonObject?) {
        require(values != null) { tr(R.string.msg_the_backup_contains_no_zoom_settings_207da79) }
        values.entrySet().forEach { (key, value) ->
            require(key.matches(Regex("profile_[0-9]+_[A-Za-z0-9_]+")) &&
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber &&
                value.asFloat.isFinite() && value.asFloat in 0.5f..3f) { tr(R.string.msg_invalid_zoom_scale_fb89ff7) }
        }
    }

    private fun writeZoom(values: Map<String, Float>) {
        val editor = zoom.edit().clear()
        values.forEach { (key, value) -> editor.putFloat(key, value) }
        check(editor.commit()) { tr(R.string.msg_could_not_save_zoom_settings_3d0493b) }
    }

    private fun InputStream.readBytesLimited(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= MAX_BYTES) { tr(R.string.msg_the_file_exceeds_the_64_mb_limit_92b7286) }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun checksum(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val MAX_BYTES = 64 * 1024 * 1024
        private const val MAX_SYNC_BACKUPS = 5
        // Parent tables precede their children; Room's internal metadata is never imported.
        private val TABLES = listOf("profiles", "app_settings", "days", "launches", "snk_values",
            "chast1_start", "chast1_records", "vr80_start", "vr80_records", "ta14_start", "ta14_records",
            "vk250001_start", "vk250001_records", "vk250002_start", "vk250002_records", "aggregate_archives", "desktop_imports", "sync_state")
    }
}
