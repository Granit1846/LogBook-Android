package com.example.logbook

import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.DayWriter
import com.example.logbook.data.local.recalc.Chast1Recalc
import com.example.logbook.data.local.entity.*
import com.example.logbook.ui.tabs.flights.FlightsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class DayDeletionTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() = db.close()

    private suspend fun createProfile(): Long {
        val profile = db.profileDao().insert(ProfileEntity())
        db.chast1StartDao().insert(Chast1StartEntity(profileId = profile))
        db.vr80StartDao().insert(Vr80StartEntity(profileId = profile))
        db.ta14StartDao().insert(Ta14StartEntity(profileId = profile))
        db.vk250001StartDao().insert(Vk250001StartEntity(profileId = profile))
        db.vk250002StartDao().insert(Vk250002StartEntity(profileId = profile))
        return profile
    }

    private suspend fun writeDay(profile: Long, date: String): Long {
        val day = db.dayDao().insert(DayEntity(profileId = profile, date = date))
        db.launchDao().insert(LaunchEntity(dayId = day, zemlya = 60, vozduh = 60, vsu = 10))
        db.snkDao().insert(SnkEntity(dayId = day))
        DayWriter(db).writeDay(profile, date, db.launchDao().getByDay(day), db.snkDao().getByDay(day))
        return day
    }

    @Test
    fun mixedFlightAndTpUseConsistentRulesInAllFiveFormulars() = runBlocking {
        val profile = createProfile()
        val chastStart = db.chast1StartDao().getByProfile(profile)!!
        db.chast1StartDao().update(chastStart.copy(colSNE = 100, col5 = 50, col7 = 20, landings = 10))
        val vrStart = db.vr80StartDao().getByProfile(profile)!!
        db.vr80StartDao().update(vrStart.copy(colSNE = 100, col24 = 50, col26 = 30, col27 = 33))
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(DayEntity(profileId = profile, date = date, crew = "Экипаж"))
        db.launchDao().insert(LaunchEntity(dayId = dayId, vsu = 4, zemlya = 5, vozduh = 10, samolet = 2))
        db.launchDao().insert(LaunchEntity(dayId = dayId, vsu = 2, tp = 7, zemlya = 100, vozduh = 100))
        val result = DayWriter(db).writeDay(profile, date, db.launchDao().getByDay(dayId), null)
        assertEquals(1, result.errors.size)
        assertEquals(5, result.writtenFormulars.size)
        val chast = db.chast1Dao().getByDate(profile, date)!!
        assertEquals(1, chast.groundHours)
        assertEquals(10, chast.flightDuration)
        assertEquals(111, chast.colSNE)
        assertEquals(61, chast.col5)
        assertEquals(23, chast.col7)
        assertEquals(13, chast.landings)
        assertEquals(2, chast.col8)
        assertEquals(1, chast.col9)
        assertEquals("Экипаж", chast.pilot)
        val vr = db.vr80Dao().getByDate(profile, date)!!
        assertEquals(2, vr.launchCount)
        assertEquals(5, vr.groundHours)
        assertEquals(10, vr.col7)
        assertEquals(7, vr.col6)
        assertEquals(111, vr.colSNE)
        assertEquals(61, vr.col24)
        assertEquals(37, vr.col26)
        assertEquals(40, vr.col27)
        assertEquals(6, db.ta14Dao().getByDate(profile, date)!!.colSNE)
        assertEquals(2, db.ta14Dao().getByDate(profile, date)!!.launchCount)
        assertEquals(11, db.vk250001Dao().getByDate(profile, date)!!.colSNE)
        assertEquals(11, db.vk250002Dao().getByDate(profile, date)!!.colSNE)
        assertEquals(1, db.vk250001Dao().getByDate(profile, date)!!.i)
        assertEquals(1, db.vk250002Dao().getByDate(profile, date)!!.i)
    }

    @Test
    fun snkWithoutLaunchesWritesOnlyTheSpecifiedEngine() = runBlocking {
        val profile = createProfile()
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(DayEntity(profileId = profile, date = date))
        val snk = SnkEntity(dayId = dayId, aLeft = 10.5, hLeft = 1.15, iLeft = 3.0)
        db.snkDao().insert(snk)
        val result = DayWriter(db).writeDay(profile, date, emptyList(), snk)
        assertEquals(setOf(DayWriter.FormularType.VK01), result.writtenFormulars)
        assertEquals(630, db.vk250001Dao().getByDate(profile, date)!!.colSNE)
        assertEquals(1.2, db.vk250001Dao().getByDate(profile, date)!!.h, 0.000001)
        assertNull(db.vk250002Dao().getByDate(profile, date))
        assertNull(db.chast1Dao().getByDate(profile, date))
        assertNull(db.vr80Dao().getByDate(profile, date))
        assertNull(db.ta14Dao().getByDate(profile, date))
    }

    @Test
    fun coefficientsAreCalculatedInDecimalBeforeRounding() = runBlocking {
        val profile = createProfile()
        val left = db.vk250001StartDao().getByProfile(profile)!!
        val right = db.vk250002StartDao().getByProfile(profile)!!
        db.vk250001StartDao().update(left.copy(coeffA = 0.29, coeffH = 0.029, pprH = 0.25))
        db.vk250002StartDao().update(right.copy(coeffA = 0.29, coeffH = 0.029, pprH = 0.25))
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(DayEntity(profileId = profile, date = date))
        db.launchDao().insert(LaunchEntity(dayId = dayId, vozduh = 50))
        DayWriter(db).writeDay(profile, date, db.launchDao().getByDay(dayId), null)
        val leftRow = db.vk250001Dao().getByDate(profile, date)!!
        val rightRow = db.vk250002Dao().getByDate(profile, date)!!
        assertEquals(15, leftRow.colSNE)
        assertEquals(15, rightRow.colSNE)
        assertEquals(1.5, leftRow.h, 0.000001)
        assertEquals(1.5, rightRow.h, 0.000001)
        assertEquals(1.3, leftRow.pprH, 0.000001)
        assertEquals(1.3, rightRow.pprH, 0.000001)
    }

    @Test
    fun monthlyTotalsUsePreviousAvailableMonthAcrossYearAndGap() = runBlocking {
        val profile = createProfile()
        writeDay(profile, "31.12.2025")
        writeDay(profile, "03.02.2026")
        val chast = db.chast1Dao().getByProfile(profile).single { it.totalMonth == "2026-02" }
        assertEquals(72, chast.colSNE)
        assertFalse(chast.needsManualRecalc)
        val vr = db.vr80Dao().getByProfile(profile).single { it.totalMonth == "2026-02" }
        assertEquals(72, vr.colSNE)
        assertFalse(vr.needsManualRecalc)
        val ta = db.ta14Dao().getByProfile(profile).single { it.totalMonth == "2026-02" }
        assertEquals(10, ta.colSNE)
        val left = db.vk250001Dao().getByProfile(profile).single { it.totalMonth == "2026-02" }
        val right = db.vk250002Dao().getByProfile(profile).single { it.totalMonth == "2026-02" }
        assertEquals(72, left.colSNE)
        assertEquals(72, right.colSNE)
        assertEquals(1, left.i)
        assertEquals(1, right.i)
        assertFalse(left.needsManualRecalc)
        assertFalse(right.needsManualRecalc)
        assertTrue(db.vk250001Dao().getByProfile(profile).single { it.totalMonth == "2025-12" }.needsManualRecalc)
    }

    @Test
    fun dailyGroundIsRoundedOnceAndCumulativeValuesRemainConsistent() = runBlocking {
        val profile = createProfile()
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(DayEntity(profileId = profile, date = date))
        repeat(3) { index ->
            db.launchDao().insert(LaunchEntity(dayId = dayId, order = index, zemlya = 2, vozduh = 10))
        }
        val writer = DayWriter(db)
        repeat(2) {
            writer.writeDay(profile, date, db.launchDao().getByDay(dayId), null)
            val row = db.chast1Dao().getByDate(profile, date)!!
            assertEquals(1, row.groundHours)
            assertEquals(30, row.flightDuration)
            assertEquals(31, row.colSNE)
            assertEquals(31, db.vr80Dao().getByDate(profile, date)!!.colSNE)
            assertEquals(31, db.vk250001Dao().getByDate(profile, date)!!.colSNE)
            assertEquals(31, db.vk250002Dao().getByDate(profile, date)!!.colSNE)
        }
        val nextDate = "02.10.2026"
        val nextId = db.dayDao().insert(DayEntity(profileId = profile, date = nextDate))
        db.launchDao().insert(LaunchEntity(dayId = nextId, zemlya = 3))
        writer.writeDay(profile, nextDate, db.launchDao().getByDay(nextId), null)
        assertEquals(32, db.chast1Dao().getByDate(profile, nextDate)!!.colSNE)
        assertEquals(32, db.chast1Dao().getByProfile(profile).single { it.isTotal }.colSNE)
        writer.removeDateFromAllFormulars(profile, date)
        assertEquals(1, db.chast1Dao().getByDate(profile, nextDate)!!.colSNE)
    }

    @Test
    fun vkValuesUseMathematicalDecimalRoundingOnBothEngines() = runBlocking {
        val profile = createProfile()
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(DayEntity(profileId = profile, date = date))
        db.launchDao().insert(LaunchEntity(dayId = dayId, zemlya = 5))
        val snk = SnkEntity(dayId = dayId,
            aLeft = 0.025, aRight = 0.025, iLeft = 2.5, iRight = 2.5,
            hLeft = 1.15, hRight = 1.15, bLeft = 1.2345, bRight = 1.2345,
            eLeft = 1.005, eRight = 1.005, gLeft = 1.004, gRight = 1.004)
        db.snkDao().insert(snk)
        DayWriter(db).writeDay(profile, date, db.launchDao().getByDay(dayId), snk)
        val left = db.vk250001Dao().getByDate(profile, date)!!
        val right = db.vk250002Dao().getByDate(profile, date)!!
        assertEquals(2, left.colSNE)
        assertEquals(2, right.colSNE)
        assertEquals(3, left.i)
        assertEquals(3, right.i)
        assertEquals(1.2, left.h, 0.000001)
        assertEquals(1.2, right.h, 0.000001)
        assertEquals(1.235, left.b, 0.000001)
        assertEquals(1.235, right.b, 0.000001)
        assertEquals(1.01, left.e, 0.000001)
        assertEquals(1.01, right.e, 0.000001)
        assertEquals(1.00, left.g, 0.000001)
        assertEquals(1.00, right.g, 0.000001)
    }

    @Test
    fun loadedPageContainsCompleteDayAndClearsAfterDeletion() = runBlocking {
        val profile = createProfile()
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val dayId = writeDay(profile, date)
        db.dayDao().update(db.dayDao().getById(dayId)!!.copy(crew = "Экипаж"))
        repeat(29) { index ->
            db.launchDao().insert(LaunchEntity(dayId = dayId, order = index + 1, vozduh = 60))
        }
        val store = ViewModelStore()
        val flights = withContext(Dispatchers.Main) {
            FlightsViewModel(db, profile).also { store.put("flights", it) }
        }
        try {
            val page = withTimeout(3_000) { flights.loadedPage.first { it != null }!! }
            assertEquals(date, page.date)
            assertEquals("Экипаж", page.details!!.day.crew)
            assertEquals(30, page.details.launches.size)
            assertTrue(page.details.launches.all { it.dayId == dayId })
            assertEquals(dayId, page.details.snk!!.dayId)
            DayWriter(db).removeDateFromAllFormulars(profile, date)
            val empty = withTimeout(3_000) {
                flights.loadedPage.first { it != null && it.details == null }!!
            }
            assertEquals(date, empty.date)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    @Test
    fun crewTransfersToPilotAndUpdatesWithoutChangingManualRows() = runBlocking {
        val profile = createProfile()
        val other = createProfile()
        val date = "01.10.2026"
        val dayId = db.dayDao().insert(
            DayEntity(profileId = profile, date = date, crew = "Иванов И.И.")
        )
        db.launchDao().insert(LaunchEntity(dayId = dayId, vozduh = 60))
        val writer = DayWriter(db)
        writer.writeDay(profile, date, db.launchDao().getByDay(dayId), null)
        assertEquals("Иванов И.И.", db.chast1Dao().getByDate(profile, date)!!.pilot)

        val row = db.chast1Dao().getByDate(profile, date)!!
        db.chast1Dao().update(row.copy(mechanic = "Техник"))
        db.chast1Dao().insert(
            Chast1Entity(profileId = profile, date = "02.10.2026", pilot = "Ручной ввод")
        )
        val otherDay = writeDay(other, date)
        db.dayDao().update(db.dayDao().getById(otherDay)!!.copy(crew = "Другой экипаж"))

        db.dayDao().update(db.dayDao().getById(dayId)!!.copy(crew = "Петров П.П."))
        writer.writeDay(profile, date, db.launchDao().getByDay(dayId), null)
        assertEquals("Петров П.П.", db.chast1Dao().getByDate(profile, date)!!.pilot)
        assertEquals("Техник", db.chast1Dao().getByDate(profile, date)!!.mechanic)
        assertEquals("Ручной ввод", db.chast1Dao().getByDate(profile, "02.10.2026")!!.pilot)
        assertEquals("", db.chast1Dao().getByDate(other, date)!!.pilot)

        db.dayDao().update(db.dayDao().getById(dayId)!!.copy(crew = ""))
        Chast1Recalc(db, profile).recalculateAll()
        assertEquals("", db.chast1Dao().getByDate(profile, date)!!.pilot)
        assertEquals("Ручной ввод", db.chast1Dao().getByDate(profile, "02.10.2026")!!.pilot)
    }

    @Test
    fun flightsObserveDeletionAndCanAddNewLaunchForSameDate() = runBlocking {
        val profile = createProfile()
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val oldDay = writeDay(profile, date)
        val store = ViewModelStore()
        val flights = withContext(Dispatchers.Main) {
            FlightsViewModel(db, profile).also { store.put("flights", it) }
        }
        try {
            withTimeout(3_000) { flights.currentDay.first { it?.id == oldDay } }
            DayWriter(db).removeDateFromAllFormulars(profile, date)
            withTimeout(3_000) { flights.currentDay.first { it == null } }
            assertNull(db.dayDao().getByDate(profile, date))

            withContext(Dispatchers.Main) { flights.addLaunch() }
            val newDay = withTimeout(3_000) {
                db.dayDao().observeByDate(profile, date).first { it != null }!!
            }
            assertEquals(1, db.launchDao().getByDay(newDay.id).size)
            assertEquals("", newDay.crew)
            assertNull(db.snkDao().getByDay(newDay.id))
            assertTrue(db.chast1Dao().getByProfile(profile).isEmpty())
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    @Test
    fun deleteDayRemovesAllRecordsRecalculatesAndPreservesOtherProfile() = runBlocking {
        val profile = createProfile()
        val other = createProfile()
        val first = "01.10.2026"
        val second = "02.10.2026"
        val deletedDay = writeDay(profile, first)
        writeDay(profile, second)
        val otherDay = writeDay(other, first)
        val otherRecords = db.chast1Dao().getByProfile(other)
        assertEquals(144, db.chast1Dao().getByDate(profile, second)!!.colSNE)

        DayWriter(db).removeDateFromAllFormulars(profile, first)

        assertNull(db.dayDao().getById(deletedDay))
        assertNull(db.snkDao().getByDay(deletedDay))
        assertTrue(db.launchDao().getByDay(deletedDay).isEmpty())
        assertNull(db.chast1Dao().getByDate(profile, first))
        assertNull(db.vr80Dao().getByDate(profile, first))
        assertNull(db.ta14Dao().getByDate(profile, first))
        assertNull(db.vk250001Dao().getByDate(profile, first))
        assertNull(db.vk250002Dao().getByDate(profile, first))
        assertEquals(72, db.chast1Dao().getByDate(profile, second)!!.colSNE)
        assertEquals(72, db.profileDao().getById(profile)!!.fuselageSne)
        assertEquals(otherRecords, db.chast1Dao().getByProfile(other))
        assertNotNull(db.dayDao().getById(otherDay))
        assertNotNull(db.snkDao().getByDay(otherDay))
        assertEquals(1, db.launchDao().getByDay(otherDay).size)
        assertNotNull(db.vr80Dao().getByDate(other, first))
        assertNotNull(db.ta14Dao().getByDate(other, first))
        assertNotNull(db.vk250001Dao().getByDate(other, first))
        assertNotNull(db.vk250002Dao().getByDate(other, first))

        DayWriter(db).removeDateFromAllFormulars(profile, second)
        assertTrue(db.chast1Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vr80Dao().getByProfile(profile).isEmpty())
        assertTrue(db.ta14Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250001Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250002Dao().getByProfile(profile).isEmpty())
        assertEquals(0, db.profileDao().getById(profile)!!.fuselageSne)
        assertEquals(otherRecords, db.chast1Dao().getByProfile(other))
    }

    @Test
    fun deleteImportedHistoricalDayRemovesItFromEveryFormularAndFlights() = runBlocking {
        val profile = createProfile()
        val date = "19.09.2026"
        val dayId = writeDay(profile, date)
        db.vr80Dao().getByDate(profile, date)!!.let { db.vr80Dao().update(it.copy(isHistorical = true)) }
        db.ta14Dao().getByDate(profile, date)!!.let { db.ta14Dao().update(it.copy(isHistorical = true)) }
        db.vk250001Dao().getByDate(profile, date)!!.let { db.vk250001Dao().update(it.copy(isHistorical = true)) }
        db.vk250002Dao().getByDate(profile, date)!!.let { db.vk250002Dao().update(it.copy(isHistorical = true)) }

        DayWriter(db).removeDateFromAllFormulars(profile, date)

        assertNull(db.dayDao().getById(dayId))
        assertTrue(db.launchDao().getByDay(dayId).isEmpty())
        assertTrue(db.chast1Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vr80Dao().getByProfile(profile).isEmpty())
        assertTrue(db.ta14Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250001Dao().getByProfile(profile).isEmpty())
        assertTrue(db.vk250002Dao().getByProfile(profile).isEmpty())
    }

    @Test
    fun deletingHistoricalDateRebuildsItsMonthlyTotals() = runBlocking {
        val profile = createProfile()
        val first = "19.09.2026"
        val second = "20.09.2026"
        writeDay(profile, first)
        writeDay(profile, second)
        db.vr80Dao().getByProfile(profile).filter { !it.isTotal }.forEach {
            db.vr80Dao().update(it.copy(isHistorical = true))
        }
        db.ta14Dao().getByProfile(profile).filter { !it.isTotal }.forEach {
            db.ta14Dao().update(it.copy(isHistorical = true))
        }
        db.vk250001Dao().getByProfile(profile).filter { !it.isTotal }.forEach {
            db.vk250001Dao().update(it.copy(isHistorical = true))
        }
        db.vk250002Dao().getByProfile(profile).filter { !it.isTotal }.forEach {
            db.vk250002Dao().update(it.copy(isHistorical = true))
        }

        DayWriter(db).removeDateFromAllFormulars(profile, first)

        assertEquals(listOf(second), db.vr80Dao().getByProfile(profile).filter { !it.isTotal }.map { it.date })
        assertEquals(1, db.vr80Dao().getByProfile(profile).single { it.isTotal }.launchCount)
        assertEquals(1, db.ta14Dao().getByProfile(profile).single { it.isTotal }.launchCount)
        assertTrue(db.vk250001Dao().getByProfile(profile).single { it.isTotal }.needsManualRecalc)
        assertTrue(db.vk250002Dao().getByProfile(profile).single { it.isTotal }.needsManualRecalc)
    }
}
