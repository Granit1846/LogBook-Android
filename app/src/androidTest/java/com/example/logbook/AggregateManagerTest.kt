package com.example.logbook

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.*
import com.example.logbook.data.local.entity.*
import com.example.logbook.data.local.recalc.*
import com.example.logbook.data.model.*
import com.example.logbook.ui.tabs.resources.ResourcesViewModel
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AggregateManagerTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 10)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var diskName: String? = null
    private val gson = Gson()

    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanup() { db.close(); diskName?.let { context.deleteDatabase(it) } }
    private fun manager() = AggregateManager(db, today)

    private suspend fun seed(): Long {
        val profile = db.profileDao().insert(ProfileEntity(name = "Первый борт", fuselageSne = 100))
        db.chast1StartDao().insert(Chast1StartEntity(profileId = profile, colSNE = 100))
        db.vr80StartDao().insert(Vr80StartEntity(profileId = profile, colSNE = 100, col24 = 50))
        db.ta14StartDao().insert(Ta14StartEntity(profileId = profile, colSNE = 200))
        db.vk250001StartDao().insert(Vk250001StartEntity(profileId = profile, colSNE = 300, coeffA = 0.29, coeffH = 0.029))
        db.vk250002StartDao().insert(Vk250002StartEntity(profileId = profile, colSNE = 400, coeffA = 0.31, coeffH = 0.031))
        db.vr80Dao().insert(Vr80Entity(profileId = profile, date = "09.10.2026", groundHours = 5, col7 = 10, colSNE = 111, col24 = 61))
        db.ta14Dao().insert(Ta14Entity(profileId = profile, date = "09.10.2026", colVremya = 10, colSNE = 210))
        db.vk250001Dao().insert(Vk250001Entity(profileId = profile, date = "09.10.2026", colSNE = 333, b = 1.005, h = 1.2))
        db.vk250002Dao().insert(Vk250002Entity(profileId = profile, date = "09.10.2026", colSNE = 444, b = 2.005, h = 2.3))
        manager().assign(profile, mapOf("Vr80" to "ВР-1", "Ta14" to "ТА-1", "Vk250001" to "Д-1", "Vk250002" to "Д-2"))
        return profile
    }

    @Test fun replacementsArchiveAndRestoreAllFourFormulars() = runBlocking {
        val profile = seed()
        val original = AggregateManager.numbers(db.profileDao().getById(profile)!!.aggregateAssignmentsJson)
        AggregateManager.TYPES.forEach { manager().replace(profile, it, "Новый-$it", "Ремонт") }
        assertTrue(db.vr80Dao().getByProfile(profile).isEmpty())
        assertTrue(db.ta14Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250001Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250002Dao().getByProfile(profile).isEmpty())
        assertEquals(0, db.vr80StartDao().getByProfile(profile)!!.colSNE)
        AggregateManager.TYPES.forEach { manager().replace(profile, it, original.getValue(it), "Возврат") }
        assertEquals(111, db.vr80Dao().getByProfile(profile).single().colSNE)
        assertEquals(210, db.ta14Dao().getByProfile(profile).single().colSNE)
        assertEquals(333, db.vk250001Dao().getByProfile(profile).single().colSNE)
        assertEquals(444, db.vk250002Dao().getByProfile(profile).single().colSNE)
        assertEquals(0.29, db.vk250001StartDao().getByProfile(profile)!!.coeffA, 0.0)
        assertEquals(0.031, db.vk250002StartDao().getByProfile(profile)!!.coeffH, 0.0)
        assertTrue(db.vr80Dao().getByProfile(profile).single().isHistorical)
        assertEquals(8, JsonParser.parseString(db.profileDao().getById(profile)!!.aggregateHistoryJson).asJsonArray.size())
    }

    @Test fun priorDaysNeverAccrueToReplacementAndTodayDoes() = runBlocking {
        val profile = seed()
        manager().replace(profile, "Vr80", "ВР-2", "Замена")
        suspend fun day(date: String) {
            val id = db.dayDao().insert(DayEntity(profileId = profile, date = date))
            val launch = LaunchEntity(dayId = id, vozduh = 20)
            db.launchDao().insert(launch)
            DayWriter(db).writeDay(profile, date, listOf(launch), null)
        }
        day("08.10.2026")
        assertTrue(db.vr80Dao().getByProfile(profile).isEmpty())
        day("10.10.2026")
        assertEquals(20, db.vr80Dao().getByDate(profile, "10.10.2026")!!.colSNE)
    }

    @Test fun everyReplacementWritesInstallationDayAndFollowingDay() = runBlocking {
        val profile = seed()
        AggregateManager.TYPES.forEach { manager().replace(profile, it, "Новый-$it", "Замена") }
        for (date in listOf("10.10.2026", "11.10.2026")) {
            val id = db.dayDao().insert(DayEntity(profileId = profile, date = date))
            val launch = LaunchEntity(dayId = id, vozduh = 20, zemlya = 5, vsu = 7)
            db.launchDao().insert(launch)
            val result = DayWriter(db).writeDay(profile, date, listOf(launch), null)
            assertEquals(DayWriter.FormularType.entries.toSet(), result.writtenFormulars)
            val days = if (date == "10.10.2026") 1 else 2
            assertEquals(21 * days, db.vr80Dao().getByDate(profile, date)!!.colSNE)
            assertEquals(7 * days, db.ta14Dao().getByDate(profile, date)!!.colSNE)
            assertEquals(21 * days, db.vk250001Dao().getByDate(profile, date)!!.colSNE)
            assertEquals(21 * days, db.vk250002Dao().getByDate(profile, date)!!.colSNE)
            // Closing the same day again must not duplicate or double count.
            DayWriter(db).writeDay(profile, date, listOf(launch), null)
            assertEquals(21 * days, db.vr80Dao().getByDate(profile, date)!!.colSNE)
        }
    }

    @Test fun returnedAggregateAcceptsActiveRowWithSameDateAsArchive() = runBlocking {
        val profile = seed()
        db.vr80Dao().insert(Vr80Entity(profileId = profile, date = "10.10.2026", col7 = 5, colSNE = 116, col24 = 66))
        manager().replace(profile, "Vr80", "ВР-2", "Снятие")
        manager().replace(profile, "Vr80", "ВР-1", "Возврат")
        val id = db.dayDao().insert(DayEntity(profileId = profile, date = "10.10.2026"))
        val launch = LaunchEntity(dayId = id, vozduh = 20)
        db.launchDao().insert(launch)
        DayWriter(db).writeDay(profile, "10.10.2026", listOf(launch), null)
        assertEquals(136, db.vr80Dao().getByDate(profile, "10.10.2026")!!.colSNE)
        assertEquals(116, db.vr80Dao().getByProfile(profile).first { it.date == "10.10.2026" && it.isHistorical }.colSNE)
    }

    @Test fun selectedPastInstallationDateControlsAllFourFormularsAndPreservesArchive() = runBlocking {
        val profile = seed()
        val installation = LocalDate.of(2026, 9, 2)
        AggregateManager.TYPES.forEach { type ->
            manager().replace(profile, type, "Другой-$type", "Замена задним числом", installation)
        }
        val savedProfile = db.profileDao().getById(profile)!!
        assertTrue(AggregateManager.numbers(savedProfile.aggregateInstalledAtJson).values.all { it == "2026-09-02" })
        assertTrue(savedProfile.aggregateHistoryJson.contains("2026-09-02"))
        val archiveBefore = db.aggregateArchiveDao().get("Vr80", "ВР-1")!!.formularJson
        for (date in listOf("01.09.2026", "02.09.2026", "03.09.2026")) {
            val id = db.dayDao().insert(DayEntity(profileId = profile, date = date))
            val launch = LaunchEntity(dayId = id, vozduh = 20, vsu = 7)
            db.launchDao().insert(launch)
            val result = DayWriter(db).writeDay(profile, date, listOf(launch), null)
            if (date == "01.09.2026") {
                assertEquals(setOf(DayWriter.FormularType.CHAST1), result.writtenFormulars)
                assertNull(db.vr80Dao().getByDate(profile, date))
                assertNull(db.ta14Dao().getByDate(profile, date))
                assertNull(db.vk250001Dao().getByDate(profile, date))
                assertNull(db.vk250002Dao().getByDate(profile, date))
            } else {
                val days = if (date == "02.09.2026") 1 else 2
                assertEquals(DayWriter.FormularType.entries.toSet(), result.writtenFormulars)
                assertEquals(20 * days, db.vr80Dao().getByDate(profile, date)!!.colSNE)
                assertEquals(7 * days, db.ta14Dao().getByDate(profile, date)!!.colSNE)
                assertEquals(20 * days, db.vk250001Dao().getByDate(profile, date)!!.colSNE)
                assertEquals(20 * days, db.vk250002Dao().getByDate(profile, date)!!.colSNE)
            }
        }
        assertEquals(archiveBefore, db.aggregateArchiveDao().get("Vr80", "ВР-1")!!.formularJson)
    }

    @Test fun engineCanMoveBetweenSidesAndBoardsWithoutChangingItsHistory() = runBlocking {
        val first = seed()
        manager().replace(first, "Vk250001", "Д-3", "Снят")
        val second = db.profileDao().insert(ProfileEntity(name = "Второй борт"))
        manager().assign(second, mapOf("Vk250002" to "Д-1"))
        assertEquals(0.29, db.vk250002StartDao().getByProfile(second)!!.coeffA, 0.0)
        // A same-date flight on the new board must not rewrite the imported historical row.
        val oldDay = db.dayDao().insert(DayEntity(profileId = second, date = "09.10.2026"))
        db.launchDao().insert(LaunchEntity(dayId = oldDay, vozduh = 500))
        Vk250002Recalc(db, second).recalculateAll()
        val historical = db.vk250002Dao().getByProfile(second).first { !it.isTotal }
        assertEquals(333, historical.colSNE)
        assertEquals(1.005, historical.b, 0.0)
        val newDay = db.dayDao().insert(DayEntity(profileId = second, date = "10.10.2026"))
        val launch = LaunchEntity(dayId = newDay, vozduh = 50)
        db.launchDao().insert(launch)
        DayWriter(db).writeDay(second, "10.10.2026", listOf(launch), null)
        assertEquals(348, db.vk250002Dao().getByDate(second, "10.10.2026")!!.colSNE)
    }

    @Test fun duplicatesAndUnchangedNumbersCannotCorruptAssignments() = runBlocking {
        val first = seed()
        val before = db.profileDao().getById(first)!!
        manager().replace(first, "Vr80", "ВР-1", "Не замена")
        assertEquals(before.aggregateHistoryJson, db.profileDao().getById(first)!!.aggregateHistoryJson)
        val second = db.profileDao().insert(ProfileEntity(name = "Второй"))
        try { manager().assign(second, mapOf("Vk250001" to "Д-1")); fail("Duplicate placement accepted") }
        catch (_: IllegalArgumentException) { }
        try { manager().replace(first, "Vk250001", "Д-2", "Дубликат"); fail("Duplicate engine sides accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals(before.aggregateAssignmentsJson, db.profileDao().getById(first)!!.aggregateAssignmentsJson)
        assertEquals("{}", db.profileDao().getById(second)!!.aggregateAssignmentsJson)
    }

    @Test fun replacementFailureRollsBackArchiveFormularAndHistory() = runBlocking {
        val profile = seed()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_assignment BEFORE UPDATE ON profiles BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { manager().replace(profile, "Vr80", "ВР-2", "Замена"); fail("Should roll back") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(111, db.vr80Dao().getByProfile(profile).single().colSNE)
        assertNull(db.aggregateArchiveDao().get("Vr80", "ВР-1"))
        assertEquals("[]", db.profileDao().getById(profile)!!.aggregateHistoryJson)
    }

    @Test fun calendarTermsAreStrictAndUseMathematicalRounding() {
        val date = LocalDate.of(2026, 2, 28)
        assertEquals("0 дн.", OilItem(byYears = "28.02.26", term = "0.125").calcRemaining2(LocalDate.of(2026, 4, 28)))
        assertEquals("0 дн.", ResourceItem(issueDate = "28.02.25", yearsResource = "1").calcRemaining2(date))
        assertEquals("", OilItem(byYears = "31.04.26").calcRemaining2(date))
        assertEquals("", ResourceItem(issueDate = "29.02.25", yearsResource = "1").calcRemaining2(date))
        assertEquals("-31 дн.", OilItem(byYears = "28.02.26", term = "0").calcRemaining2(LocalDate.of(2026, 3, 31)))
        assertEquals(-10, OilItem(byHours = 100, coefficient = 50).calcRemaining1(160))
    }

    @Test fun resourcesAndOilsEditsPreserveOtherProfileFields() = runBlocking {
        val id = db.profileDao().insert(ProfileEntity(name = "Борт", fuselageSne = 100,
            resourcesJson = gson.toJson(listOf(ResourceItem(name = "Фюзеляж"), ResourceItem(name = "Агрегат", installedAtSne1 = 50, installedAtSne2 = 10)))))
        val aggregates = withContext(Dispatchers.Main) { ResourcesViewModel(db, id) }
        val oils = withContext(Dispatchers.Main) { ResourcesViewModel(db, id) }
        withTimeout(3000) { aggregates.profile.first { it != null }; oils.profile.first { it != null } }
        withContext(Dispatchers.Main) {
            oils.updateOils(listOf(OilItem(name = "Новое масло", byHours = 100)))
            aggregates.updateResource(1, aggregates.resources.value[1].copy(extendedTo1 = 1000))
        }
        withTimeout(3000) {
            db.profileDao().observeById(id).first {
                it != null && it.oilsJson.contains("Новое масло") && it.resourcesJson.contains("1000")
            }
        }
        db.profileDao().update(db.profileDao().getById(id)!!.copy(fuselageSne = 120, aggregateAssignmentsJson = "{\"Vr80\":\"123\"}"))
        withTimeout(3000) { aggregates.resources.first { it.getOrNull(1)?.sne == 80 } }
        assertEquals("123", AggregateManager.numbers(db.profileDao().getById(id)!!.aggregateAssignmentsJson)["Vr80"])
        withContext(Dispatchers.Main) { androidx.lifecycle.ViewModelStore().apply { put("a", aggregates); put("o", oils); clear() } }
        Unit
    }

    @Test fun migration23To25PreservesExistingProfilesAndFormulars() = runBlocking {
        db.close()
        val name = "aggregate-migration-${UUID.randomUUID()}.db"
        diskName = name
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val profile = seed()
        db.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
            old.execSQL("ALTER TABLE profiles DROP COLUMN aggregateInstalledAtJson")
            listOf("vr80_records", "ta14_records", "vk250001_records", "vk250002_records").forEach { old.execSQL("ALTER TABLE `$it` DROP COLUMN isHistorical") }
            old.execSQL("DROP TABLE aggregate_archives")
            old.execSQL("DROP TABLE desktop_imports");old.execSQL("DROP TABLE sync_state")
            old.execSQL("UPDATE room_master_table SET identity_hash = '29dfb6c7912ef2a00845e0bbdeb2b17d' WHERE id = 42")
            old.version = 23
        }
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25, AppDatabase.MIGRATION_25_26).build()
        assertEquals("Первый борт", db.profileDao().getById(profile)!!.name)
        assertEquals(111, db.vr80Dao().getByProfile(profile).single().colSNE)
        assertEquals(0.29, db.vk250001StartDao().getByProfile(profile)!!.coeffA, 0.0)
        assertEquals("{}", db.profileDao().getById(profile)!!.aggregateInstalledAtJson)
    }
}
