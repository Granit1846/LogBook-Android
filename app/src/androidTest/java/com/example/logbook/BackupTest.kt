package com.example.logbook

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.backup.BackupManager
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var prefs: SharedPreferences
    private var diskDatabase: String? = null

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        directory = File(app.cacheDir, "backup-test-$id").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("test-$id-$name", mode)
        }
        prefs = context.getSharedPreferences("table_zoom", Context.MODE_PRIVATE)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        manager = BackupManager(context, db)
    }

    @After fun cleanup() {
        db.close()
        diskDatabase?.let { context.deleteDatabase(it) }
        prefs.edit().clear().commit()
        directory.deleteRecursively()
    }

    private suspend fun seed(): Long {
        val profile = db.profileDao().insert(ProfileEntity(name = "Борт №1", boardNumber = "123",
            resourcesJson = "[{\"serial\":\"Д-123\"}]", oilsJson = "[{\"name\":\"Масло\"}]",
            aggregateHistoryJson = "[{\"date\":\"01.10.2026\"}]", aggregateAssignmentsJson = "{\"left\":\"Д-123\"}",
            maintenanceWorksJson = com.example.logbook.data.model.MaintenanceJournal.encodeWorks(listOf(
                com.example.logbook.data.model.MaintenanceWork(kind = "HOURS_100", date = "2026-10-01", counterMinutes = 60000, description = "100 ч"))),
            defectTasksJson = com.example.logbook.data.model.MaintenanceJournal.encodeTasks(listOf(
                com.example.logbook.data.model.DefectTask(description = "Проверить течь")))))
        db.profileDao().insert(ProfileEntity(name = "Борт №2"))
        db.appSettingsDao().insert(AppSettingsEntity(theme = "Dark", supabaseUrl = "https://example.test", supabaseKey = "test-key"))
        db.aggregateArchiveDao().save(AggregateArchiveEntity(type = "Vr80", number = "ВР-123",
            formularJson = "{\"start\":null,\"records\":[]}", savedAt = 12345))
        db.chast1StartDao().insert(Chast1StartEntity(profileId = profile, colSNE = 100))
        db.vr80StartDao().insert(Vr80StartEntity(profileId = profile, colSNE = 200))
        db.ta14StartDao().insert(Ta14StartEntity(profileId = profile, colSNE = 300))
        db.vk250001StartDao().insert(Vk250001StartEntity(profileId = profile, coeffA = 0.29))
        db.vk250002StartDao().insert(Vk250002StartEntity(profileId = profile, coeffA = 0.31))
        val day = db.dayDao().insert(DayEntity(profileId = profile, date = "10.10.2026", crew = "Иванов", isClosed = true))
        db.launchDao().insert(LaunchEntity(dayId = day, vozduh = 35, zemlya = 5, vsu = 3))
        db.snkDao().insert(SnkEntity(dayId = day, hLeft = 1.15, bRight = 1.005))
        db.chast1Dao().insert(Chast1Entity(profileId = profile, date = "10.10.2026", pilot = "Ручное изменение", colSNE = 999))
        db.vr80Dao().insert(Vr80Entity(profileId = profile, colSNE = 888))
        db.ta14Dao().insert(Ta14Entity(profileId = profile, colSNE = 777))
        db.vk250001Dao().insert(Vk250001Entity(profileId = profile, h = 1.2, b = 1.005, isTotal = true))
        db.vk250002Dao().insert(Vk250002Entity(profileId = profile, h = 2.3))
        prefs.edit().putFloat("profile_${profile}_chast1", 1.8f).putFloat("profile_${profile}_oils", 0.6f).commit()
        return profile
    }

    private suspend fun snapshot(): ByteArray = ByteArrayOutputStream().also { manager.export(it) }.toByteArray()
    private fun payload(bytes: ByteArray): JsonObject = JsonParser.parseString(bytes.toString(Charsets.UTF_8))
        .asJsonObject.getAsJsonObject("data")
    private suspend fun expectRejected(bytes: ByteArray) {
        try { manager.prepare(ByteArrayInputStream(bytes)); fail("Invalid backup was accepted") }
        catch (_: Exception) { }
    }
    private fun altered(bytes: ByteArray, change: (JsonObject) -> Unit): ByteArray {
        val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        change(root.getAsJsonObject("data"))
        val content = Gson().toJson(root.getAsJsonObject("data")).toByteArray(Charsets.UTF_8)
        root.addProperty("sha256", MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) })
        return Gson().toJson(root).toByteArray(Charsets.UTF_8)
    }

    @Test fun roundTripPreservesAllTablesManualEditsSettingsAndZoom() = runBlocking {
        seed()
        val before = snapshot()
        val prepared = manager.prepare(ByteArrayInputStream(before))
        assertEquals(2, prepared.profiles)
        assertEquals(1, prepared.days)
        db.clearAllTables()
        db.profileDao().insert(ProfileEntity(name = "Заменяемый профиль"))
        prefs.edit().clear().putFloat("profile_900_oils", 3f).commit()
        manager.restore(prepared)
        val after = payload(snapshot())
        assertEquals(payload(before).get("tables"), after.get("tables"))
        assertEquals(payload(before).get("zoom"), after.get("zoom"))
        assertEquals(2, withTimeout(3000) { db.profileDao().observeAll().first { it.size == 2 } }.size)
        assertEquals(1, com.example.logbook.data.model.MaintenanceJournal.works(db.profileDao().getById(1)!!).size)
        assertEquals(1, com.example.logbook.data.model.MaintenanceJournal.openTasks(db.profileDao().getById(1)!!).size)
        assertTrue(manager.hasSafetyCopy())
    }

    @Test fun diskDatabaseWithWalSurvivesRestoreAndReopen() = runBlocking {
        db.close()
        val name = "backup-test-${UUID.randomUUID()}.db"
        diskDatabase = name
        db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()
        manager = BackupManager(context, db)
        seed()
        val before = snapshot()
        val prepared = manager.prepare(ByteArrayInputStream(before))
        db.clearAllTables()
        manager.restore(prepared)
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        manager = BackupManager(context, db)
        assertEquals(payload(before).get("tables"), payload(snapshot()).get("tables"))
        assertEquals(payload(before).get("zoom"), payload(snapshot()).get("zoom"))
    }

    @Test fun version23BackupIsImportedWithoutLosingExistingData() = runBlocking {
        seed()
        val legacy = altered(snapshot()) {
            it.addProperty("databaseVersion", 23)
            val tables = it.getAsJsonObject("tables")
            tables.remove("aggregate_archives")
            tables.remove("desktop_imports");tables.remove("sync_state")
            tables.getAsJsonArray("profiles").forEach { row -> row.asJsonObject.remove("aggregateInstalledAtJson") }
            listOf("vr80_records", "ta14_records", "vk250001_records", "vk250002_records").forEach { table ->
                tables.getAsJsonArray(table).forEach { row -> row.asJsonObject.remove("isHistorical") }
            }
        }
        manager.restore(manager.prepare(ByteArrayInputStream(legacy)))
        assertEquals("Ручное изменение", db.chast1Dao().getByProfile(1).single().pilot)
        assertEquals(2, db.profileDao().getAll().size)
        assertNull(db.aggregateArchiveDao().get("Vr80", "ВР-123"))
    }

    @Test fun safetyCopyContainsTheDataReplacedByRestore() = runBlocking {
        val profile = seed()
        val prepared = manager.prepare(ByteArrayInputStream(snapshot()))
        val current = db.profileDao().getById(profile)!!
        db.profileDao().update(current.copy(name = "Перед восстановлением"))
        prefs.edit().putFloat("profile_${profile}_chast1", 2.4f).commit()
        val expected = payload(snapshot())
        manager.restore(prepared)
        val safety = ByteArrayOutputStream().also { manager.exportSafetyCopy(it) }.toByteArray()
        assertEquals(expected.get("tables"), payload(safety).get("tables"))
        assertEquals(expected.get("zoom"), payload(safety).get("zoom"))
        manager.restore(manager.prepare(ByteArrayInputStream(safety)))
        assertEquals("Перед восстановлением", db.profileDao().getById(profile)!!.name)
    }

    @Test fun malformedCorruptAndUnsupportedBackupsLeaveDataUntouched() = runBlocking {
        seed()
        val before = snapshot()
        expectRejected("broken json".toByteArray())
        expectRejected(before.toString(Charsets.UTF_8).replace("Борт №1", "Повреждён").toByteArray())
        expectRejected(altered(before) { it.addProperty("databaseVersion", 999) })
        expectRejected(altered(before) { it.getAsJsonObject("tables").remove("days") })
        expectRejected(altered(before) { it.getAsJsonObject("zoom").addProperty("profile_1_oils", 7) })
        assertEquals(payload(before).get("tables"), payload(snapshot()).get("tables"))
        assertFalse(manager.hasSafetyCopy())
    }

    @Test fun invalidLinksAndDuplicateIdsAreRejectedBeforeReplacement() = runBlocking {
        seed()
        val before = snapshot()
        expectRejected(altered(before) {
            it.getAsJsonObject("tables").getAsJsonArray("launches")[0].asJsonObject.addProperty("dayId", 99999)
        })
        expectRejected(altered(before) {
            val profiles = it.getAsJsonObject("tables").getAsJsonArray("profiles")
            profiles.add(profiles[0].deepCopy())
        })
        assertEquals(payload(before).get("tables"), payload(snapshot()).get("tables"))
        assertEquals(payload(before).get("zoom"), payload(snapshot()).get("zoom"))
    }

    @Test fun liveDatabaseFailureRollsBackAllChanges() = runBlocking {
        val profile = seed()
        val prepared = manager.prepare(ByteArrayInputStream(snapshot()))
        db.profileDao().update(db.profileDao().getById(profile)!!.copy(name = "Не потерять"))
        prefs.edit().putFloat("profile_${profile}_chast1", 2.8f).commit()
        val before = payload(snapshot())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON vk250002_records BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { manager.restore(prepared); fail("Injected failure should abort restore") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before.get("tables"), payload(snapshot()).get("tables"))
        assertEquals(before.get("zoom"), payload(snapshot()).get("zoom"))
    }

    @Test fun failedSafetyWritePreventsReplacement() = runBlocking {
        val profile = seed()
        val prepared = manager.prepare(ByteArrayInputStream(snapshot()))
        db.profileDao().update(db.profileDao().getById(profile)!!.copy(name = "Сохранить эти данные"))
        File(directory, "backups").writeText("blocks directory creation")
        try { manager.restore(prepared); fail("Restore should abort if safety copy cannot be written") }
        catch (_: java.io.IOException) { }
        assertEquals("Сохранить эти данные", db.profileDao().getById(profile)!!.name)
    }
    @Test fun version24BackupIsImportedInto25() = runBlocking {
        seed()
        val legacy = altered(snapshot()) {
            it.addProperty("databaseVersion",24)
            it.getAsJsonObject("tables").remove("desktop_imports");it.getAsJsonObject("tables").remove("sync_state")
        }
        manager.restore(manager.prepare(ByteArrayInputStream(legacy)))
        assertEquals(2,db.profileDao().getAll().size)
        assertNull(db.desktopImportDao().get("board:04008"))
    }

    @Test fun version25BackupAndSyncStateRoundTrip() = runBlocking {
        seed()
        db.syncStateDao().save(SyncStateEntity(identityJson="{\"profiles:1\":\"shared-id\"}",pendingId="pending",pendingJson="{}",pendingRevision=3))
        val current=snapshot()
        db.syncStateDao().save(SyncStateEntity())
        manager.restore(manager.prepare(ByteArrayInputStream(current)))
        assertEquals("pending",db.syncStateDao().get()!!.pendingId)
        assertEquals(3L,db.syncStateDao().get()!!.pendingRevision)
        val old=altered(current) { it.addProperty("databaseVersion",25);it.getAsJsonObject("tables").remove("sync_state") }
        manager.restore(manager.prepare(ByteArrayInputStream(old)))
        assertNull(db.syncStateDao().get());assertEquals(2,db.profileDao().getAll().size)
    }

}
