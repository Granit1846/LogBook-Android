package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String = "",
    val boardNumber: String = "",

    val selectedTab: Int = -1,
    val lastOpenedDate: String = "",

    // === Ресурсы (JSON) ===
    val aggregateAssignmentsJson: String = "{}",
    val resourcesJson: String = "[]",
    val oilsJson: String = "[]",
    val aggregateHistoryJson: String = "[]",
    val aggregateInstalledAtJson: String = "{}",
    val maintenanceWorksJson: String = "[]",
    val defectTasksJson: String = "[]",

    // === Группы агрегатов (JSON) ===
    // Массив AggregateGroup: [{"id":"g1","name":"Двигатели","order":0,...}]
    val aggregateGroupsJson: String = "[]",
    // === Группы масел (JSON) ===
    // Массив AggregateGroup: [{"id":"g1","name":"Масла 1","order":0,...}]
    val oilGroupsJson: String = "[]",

    val resourcesUpdatedAt: Long = 0,

    val hasBeenSynced: Boolean = false,

    // Служебное
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // === Фюзеляж (СНЭ) ===
    val fuselageSne: Int = 0,
    // === UI-состояние агрегатов / масел ===
    val aggregateSortMode: String = "NONE",
    val aggregateViewMode: String = "ROWS",
    val oilSortMode: String = "NONE",
    val oilViewMode: String = "ROWS",
)
