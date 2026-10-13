package com.example.logbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.logbook.data.backup.BackupManager
import com.example.logbook.data.exchange.*
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class DesktopImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanup() { db.close() }
    private fun realFile(): ByteArray = InstrumentationRegistry.getInstrumentation().context.assets.open("legacy-work-data.json").use { it.readBytes() }
    private suspend fun prepare(bytes: ByteArray = realFile()): PreparedDesktopImport = DesktopImporter(context,db).prepare(ByteArrayInputStream(bytes))
    private suspend fun snapshot(): ByteArray = ByteArrayOutputStream().also { BackupManager(context,db).export(it) }.toByteArray()

    @Test fun realDesktopFileImportsAllDataAndCanRestoreSafetyCopy() = runBlocking {
        val old = db.profileDao().insert(ProfileEntity(name="Существующий",boardNumber="EXISTING"))
        val before = JsonParser.parseString(snapshot().toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("data").get("tables")
        val prepared = prepare()
        assertEquals(2,prepared.preview.size); assertEquals(22,prepared.archiveCount)
        assertTrue(prepared.preview.none { it.blocked })
        assertEquals(2,DesktopImporter(context,db).apply(prepared,prepared.preview.map { it.key }.toSet()))
        assertEquals(3,db.profileDao().getAll().size)
        val p = db.profileDao().getAll().single { it.name == "92" }
        val rows = db.chast1Dao().getByProfile(p.id)
        assertEquals(23,rows.size)
        assertEquals(1173*60+39,rows.first().colSNE)
        assertEquals("02.09.2026",rows.first().date)
        assertTrue(rows.any { it.isTotal && it.totalMonth == "2026-09" })
        assertEquals(22,db.dayDao().countByProfile(p.id))
        val day = db.dayDao().getByDate(p.id,"02.09.2026")!!
        assertTrue(day.isClosed)
        assertEquals(108,db.launchDao().getByDay(day.id).single().vozduh)
        assertEquals(1183*60+15,db.vk250001Dao().getByProfile(p.id).first().colSNE)
        assertEquals(0.736,db.vk250001Dao().getByProfile(p.id).first().b,0.000001)
        assertTrue(db.vr80Dao().getByProfile(p.id).first().isHistorical)
        assertEquals("Существующий",db.profileDao().getById(old)!!.name)
        val receipt = db.desktopImportDao().get("board:04008")!!
        assertEquals(ExchangeIds.legacy("profile","board:04008"),receipt.sharedId)
        assertTrue(receipt.originalJson.contains("SnkValues"))
        assertNotNull(db.aggregateArchiveDao().get("Vk2500","vk12"))
        val backup = BackupManager(context,db)
        val safety = ByteArrayOutputStream().also { backup.exportSafetyCopy(it) }.toByteArray()
        val saved = JsonParser.parseString(safety.toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("data").get("tables")
        assertEquals(before,saved)
        backup.restore(backup.prepare(ByteArrayInputStream(safety)))
        assertEquals(1,db.profileDao().getAll().size)
        assertNull(db.desktopImportDao().get("board:04008"))
    }

    @Test fun repeatedOrConflictingImportCannotOverwriteCurrentProfiles() = runBlocking {
        val importer=DesktopImporter(context,db); val prepared=prepare()
        val keys=prepared.preview.map { it.key }.toSet()
        importer.apply(prepared,keys)
        assertTrue(prepare().preview.all { it.blocked })
        val before=snapshot()
        try { importer.apply(prepared,keys); fail("Repeated import allowed") } catch (_:IllegalArgumentException) {}
        val left=JsonParser.parseString(before.toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("data").get("tables")
        val right=JsonParser.parseString(snapshot().toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("data").get("tables")
        assertEquals(left,right)
    }

    @Test fun invalidDatesTimesAndNonIntegerCountsAreRejectedBeforeWriting() = runBlocking {
        val original=JsonParser.parseString(realFile().toString(Charsets.UTF_8)).asJsonObject
        for ((field,value) in listOf("Date" to "31.02.2026","GroundHours" to "1:99","Landings" to "2,5")) {
            val root=original.deepCopy()
            root.getAsJsonObject("Profiles").entrySet().first().value.asJsonObject.getAsJsonArray("Chast1Records")[0].asJsonObject.addProperty(field,value)
            try { prepare(root.toString().toByteArray(Charsets.UTF_8)); fail("Accepted $field") } catch (_:Exception) {}
            assertTrue(db.profileDao().getAll().isEmpty())
        }
    }

    @Test fun importIdentitiesAndOriginalSourceSurviveBackupRestore() = runBlocking {
        val importer=DesktopImporter(context,db);val prepared=prepare()
        importer.apply(prepared,setOf(prepared.preview.first().key))
        val copy=snapshot();val backup=BackupManager(context,db)
        backup.restore(backup.prepare(ByteArrayInputStream(copy)))
        assertTrue(prepare().preview.first().blocked)
        assertNotNull(db.desktopImportDao().get(prepared.preview.first().key))
    }
    @Test fun failedSecondProfileRollsBackAllImportedRows() = runBlocking {
        db.profileDao().insert(ProfileEntity(name="Старые данные"))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_import BEFORE INSERT ON profiles WHEN NEW.name = '91' BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        val prepared=prepare()
        try { DesktopImporter(context,db).apply(prepared,prepared.preview.map { it.key }.toSet()); fail("Import should fail") }
        catch (_: android.database.sqlite.SQLiteException) {}
        assertEquals(listOf("Старые данные"),db.profileDao().getAll().map { it.name })
        assertNull(db.desktopImportDao().get("board:04008"))
        assertNull(db.aggregateArchiveDao().get("Vk2500","vk12"))
    }

    @Test fun database24MigrationKeepsExistingProfileAndCreatesImportReceipts() = runBlocking {
        val name="migration-import-${java.util.UUID.randomUUID()}.db"
        var disk=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            val profile=disk.profileDao().insert(ProfileEntity(name="Сохранить",boardNumber="123"))
            disk.close()
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE desktop_imports");it.execSQL("DROP TABLE sync_state");it.version=24
            }
            disk=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_24_25, AppDatabase.MIGRATION_25_26).build()
            assertEquals("Сохранить",disk.profileDao().getById(profile)!!.name)
            assertNull(disk.desktopImportDao().get("new"))
            assertEquals(26,disk.openHelper.readableDatabase.version)
        } finally { disk.close();context.deleteDatabase(name) }
    }

    @Test fun aggregateReplacementKeepsImportedSourceAndSharedIdentity() = runBlocking {
        val prepared=prepare()
        DesktopImporter(context,db).apply(prepared,setOf("board:04008"))
        val p=db.profileDao().getAll().single()
        val before=JsonParser.parseString(db.aggregateArchiveDao().get("Vr80","vr1")!!.formularJson).asJsonObject
        com.example.logbook.data.local.AggregateManager(db).replace(p.id,"Vr80","NEW-UNUSED","Проверка")
        val after=JsonParser.parseString(db.aggregateArchiveDao().get("Vr80","vr1")!!.formularJson).asJsonObject
        assertEquals(before.get("legacySource"),after.get("legacySource"))
        assertEquals(before.get("sharedId"),after.get("sharedId"))
        assertEquals(p.id,db.desktopImportDao().get("board:04008")!!.profileId)
    }

}

