package com.example.logbook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.SyncStore
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.MaintenanceJournal
import com.example.logbook.data.model.MaintenanceWork
import com.example.logbook.data.model.DefectTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MaintenanceSyncTest {
    @Test fun sharedProfileImportPreservesJournalAndDefects() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val work = MaintenanceWork(kind = MaintenanceJournal.HOURS_100, date = "2026-10-10",
                counterMinutes = 60_000, description = "100-часовые работы")
            val task = DefectTask(id = UUID.randomUUID().toString(), description = "Проверить течь", workId = work.id)
            source.profileDao().insert(ProfileEntity(name = "Борт", boardNumber = "04008",
                maintenanceWorksJson = MaintenanceJournal.encodeWorks(listOf(work)),
                defectTasksJson = MaintenanceJournal.encodeTasks(listOf(task))))
            val packet = SyncStore(context, source).sharedProfilePackets().single().payload
            SyncStore(context, target).importSharedProfiles(listOf(packet))
            val imported = target.profileDao().getAll().single()
            assertEquals(work, MaintenanceJournal.works(imported).single())
            assertEquals(task, MaintenanceJournal.tasks(imported).single())
        } finally {
            source.close()
            target.close()
        }
    }
}
