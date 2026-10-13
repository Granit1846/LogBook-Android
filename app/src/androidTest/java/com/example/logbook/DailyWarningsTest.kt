package com.example.logbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.*
import com.example.logbook.notifications.DailyWarnings
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DailyWarningsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var prefs: android.content.SharedPreferences
    private val today = LocalDate.of(2026, 10, 10)
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        prefs = context.getSharedPreferences("warning-test-${UUID.randomUUID()}", Context.MODE_PRIVATE)
    }
    @After fun cleanup() { db.close(); prefs.edit().clear().commit() }
    private suspend fun seed() {
        db.profileDao().insert(ProfileEntity(name = "Первый", fuselageSne = 600,
            resourcesJson = Gson().toJson(listOf(ResourceItem(name = "Фюзеляж", sne = 0, extendedTo1 = 610)))))
        db.profileDao().insert(ProfileEntity(name = "Второй", fuselageSne = 100,
            oilsJson = Gson().toJson(listOf(OilItem(name = "Редуктор", coefficient = 90)))))
    }
    @Test fun oneDailyDigestAcrossRestartsDismissalsAndConcurrentChecks() = runBlocking {
        seed()
        val deliveries = mutableListOf<List<String>>()
        fun notifier() = DailyWarnings(db, prefs, { true }, { deliveries.add(it) })
        assertTrue(notifier().check(today))
        assertEquals(2, deliveries.single().size)
        assertTrue(deliveries.single()[0].contains("Первый"))
        assertTrue(deliveries.single()[1].contains("Второй"))
        assertFalse(notifier().check(today))
        coroutineScope { List(5) { async { notifier().check(today.plusDays(1)) } }.awaitAll() }
        assertEquals(2, deliveries.size)
    }
    @Test fun deniedPermissionAndDeliveryFailureDoNotConsumeTheDay() = runBlocking {
        seed()
        assertFalse(DailyWarnings(db, prefs, { false }, { fail("Permission denied") }).check(today))
        try { DailyWarnings(db, prefs, { true }, { error("test error") }).check(today); fail("No error") }
        catch (_: IllegalStateException) { }
        assertNull(prefs.getString("last_day", null))
        assertTrue(DailyWarnings(db, prefs, { true }, {}).check(today))
    }
    @Test fun noWarningsAllowsNotificationWhenResourceChangesLater() = runBlocking {
        val notifier = DailyWarnings(db, prefs, { true }, {})
        assertFalse(notifier.check(today))
        seed()
        assertTrue(notifier.check(today))
    }
    @Test fun realAndroidNotificationContainsDigestAndDoesNotReturnAfterDismissal() = runBlocking {
        seed()
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val testId = 702
        val notifier = DailyWarnings(db, prefs, { true }, { DailyWarnings.postNotification(context, it, testId) })
        try {
            assertTrue(notifier.check(today))
            withTimeout(3000) { while (manager.activeNotifications.none { it.id == testId }) delay(50) }
            val posted = manager.activeNotifications.first { it.id == testId }.notification
            assertEquals("Сегодня: предупреждения LogBook", posted.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertTrue(posted.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT).toString().contains("Второй"))
            assertEquals(DailyWarnings.CHANNEL, posted.channelId)
            assertNotNull(posted.smallIcon)
            manager.cancel(testId)
            withTimeout(3000) { while (manager.activeNotifications.any { it.id == testId }) delay(50) }
            assertFalse(notifier.check(today))
            assertTrue(manager.activeNotifications.none { it.id == testId })
        } finally { manager.cancel(testId) }
    }

    @Test fun profileWarningsUseFreshSneAndBothResourceTabs() {
        val profile = ProfileEntity(name = "Борт", fuselageSne = 100,
            resourcesJson = Gson().toJson(listOf(ResourceItem(name = "Агрегат", installedAtSne1 = 20, installedAtSne2 = 10, sne = 0, extendedTo1 = 80))),
            oilsJson = Gson().toJson(listOf(OilItem(name = "Масло", coefficient = 100, byYears = "09.10.25", term = "1"))))
        val warnings = ResourceWarnings.forProfile(profile, today)
        assertEquals(3, warnings.size)
        assertEquals(2, warnings.count { it.level == ResourceWarningLevel.EXPIRED })
        assertTrue(warnings.any { it.text.startsWith("Агрегаты") })
        assertTrue(warnings.any { it.text.startsWith("Масла") })
    }
}
