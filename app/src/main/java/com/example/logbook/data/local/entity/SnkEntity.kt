package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snk_values",
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayId"], unique = true)]
)
data class SnkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,

    // Левая половина — ВК-2500-01
    val aLeft: Double = 0.0,
    val bLeft: Double = 0.0,
    val eLeft: Double = 0.0,
    val gLeft: Double = 0.0,
    val hLeft: Double = 0.0,
    val iLeft: Double = 0.0,

    // Правая половина — ВК-2500-02
    val aRight: Double = 0.0,
    val bRight: Double = 0.0,
    val eRight: Double = 0.0,
    val gRight: Double = 0.0,
    val hRight: Double = 0.0,
    val iRight: Double = 0.0
)