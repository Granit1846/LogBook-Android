package com.example.logbook

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.MaintenanceJournal
import com.example.logbook.data.model.MaintenanceRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MaintenanceRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanup() { manager.cancel(702); db.close() }

    @Test fun workAndDefectRemainAfterReloadAndNotificationClearsOnCompletion() = runBlocking {
        val id = db.profileDao().insert(ProfileEntity(name = "Тестовый борт", fuselageSne = 61_000))
        val repository = MaintenanceRepository(context, db)
        repository.addWork(id, MaintenanceJournal.HOURS_100, LocalDate.of(2026, 10, 1), 60_000, "Последние 100 часов")
        val work = MaintenanceJournal.works(db.profileDao().getById(id)!!).single()
        repository.addTask(id, "Проверить течь", work.id)
        val before = db.profileDao().getById(id)!!
        val task = MaintenanceJournal.openTasks(before).single()
        assertEquals(work.id, task.workId)
        assertEquals("Проверить течь", task.description)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            assertTrue(manager.activeNotifications.any { it.id == 702 && it.notification.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT).toString().contains("Проверить течь") })
        }
        repository.completeTask(id, task.id, 1_000_000L)
        val after = db.profileDao().getById(id)!!
        assertTrue(MaintenanceJournal.openTasks(after).isEmpty())
        assertEquals(1_000_000L, MaintenanceJournal.tasks(after).single().completedAt)
        repeat(20) {
            if (manager.activeNotifications.none { notification -> notification.id == 702 }) return@runBlocking
            Thread.sleep(50)
        }
        assertTrue(manager.activeNotifications.none { it.id == 702 })
    }
}
