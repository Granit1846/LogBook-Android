package com.example.logbook.data.model

import android.content.Context
import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.notifications.DefectNotifications
import java.time.LocalDate
import java.util.UUID

class MaintenanceRepository(private val context: Context, private val db: AppDatabase) {
    suspend fun addWork(profileId: Long, kind: String, date: LocalDate, counterMinutes: Int, description: String) {
        require(kind in MaintenanceJournal.kinds)
        require(counterMinutes >= 0)
        require(description.length <= 2000)
        db.withTransaction {
            val profile = db.profileDao().getById(profileId) ?: error("Профиль не найден")
            val updated = MaintenanceJournal.works(profile) + MaintenanceWork(
                kind = kind, date = date.toString(), counterMinutes = counterMinutes,
                description = description.trim())
            db.profileDao().updateMaintenanceWorks(profileId, MaintenanceJournal.encodeWorks(updated))
        }
    }

    suspend fun addTask(profileId: Long, description: String, workId: String = "") {
        require(description.trim().isNotEmpty() && description.length <= 2000)
        db.withTransaction {
            val profile = db.profileDao().getById(profileId) ?: error("Профиль не найден")
            require(workId.isBlank() || MaintenanceJournal.works(profile).any { it.id == workId })
            val updated = MaintenanceJournal.tasks(profile) + DefectTask(description = description.trim(), workId = workId)
            db.profileDao().updateDefectTasks(profileId, MaintenanceJournal.encodeTasks(updated))
        }
    }

    suspend fun completeTask(profileId: Long, taskId: String, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            val profile = db.profileDao().getById(profileId) ?: error("Профиль не найден")
            val current = MaintenanceJournal.tasks(profile)
            require(current.any { it.id == taskId && it.completedAt == 0L })
            val updated = current.map { if (it.id == taskId) it.copy(completedAt = now) else it }
            db.profileDao().updateDefectTasks(profileId, MaintenanceJournal.encodeTasks(updated))
        }
    }

    suspend fun clearHistory(profileId: Long) {
        db.withTransaction {
            val profile = db.profileDao().getById(profileId) ?: error("Профиль не найден")
            val works = MaintenanceJournal.works(profile)
            // Сохраняем последние активные базовые регламентные работы (введенные данные)
            val activeWorks = MaintenanceJournal.kinds.mapNotNull { kind ->
                val candidates = works.filter { it.kind == kind }
                if (kind in listOf(MaintenanceJournal.HOURS_50, MaintenanceJournal.HOURS_100, MaintenanceJournal.HOURS_300)) {
                    candidates.maxWithOrNull(compareBy<MaintenanceWork> { it.counterMinutes }.thenBy { it.createdAt })
                } else {
                    candidates.maxWithOrNull(compareBy<MaintenanceWork> { LocalDate.parse(it.date) }.thenBy { it.createdAt })
                }
            }.distinctBy { it.id }
            db.profileDao().updateMaintenanceWorks(profileId, MaintenanceJournal.encodeWorks(activeWorks))
            val openTasks = MaintenanceJournal.tasks(profile).filter { it.completedAt == 0L }
            db.profileDao().updateDefectTasks(profileId, MaintenanceJournal.encodeTasks(openTasks))
        }
    }

    companion object {
        fun validate(profile: ProfileEntity) {
            val works = MaintenanceJournal.works(profile)
            val tasks = MaintenanceJournal.tasks(profile)
            works.forEach {
                require(it.kind.isBlank() || it.kind in MaintenanceJournal.kinds)
                if (it.date.isNotBlank()) {
                    runCatching { LocalDate.parse(it.date) }
                }
                require(it.counterMinutes >= 0 && it.description.length <= 2000)
            }
            tasks.forEach {
                require(it.description.length <= 2000)
                require(it.completedAt >= 0L)
            }
        }
    }
}
