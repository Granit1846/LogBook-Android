package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chast1_records",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("sortOrder")]
)
data class Chast1Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,

    // Дата в формате dd.MM.yyyy
    val date: String = "",

    // Порядок сортировки: epochDay × 2 (чётное) для обычных,
    // epochDay(последний день месяца) × 2 + 1 для итогов
    val sortOrder: Long = 0,

    val flightType: String = "",

    val groundHours: Int = 0,
    val flightDuration: Int = 0,

    val col5: Int = 0,
    val colSNE: Int = 0,

    val col7: Int = 0,
    val col8: Int = 0,
    val col9: Int = 0,
    val landings: Int = 0,
    val col11: Int = 0,
    val landings2: Int = 0,

    val pilot: String = "",
    val mechanic: String = "",

    val isTotal: Boolean = false,
    val totalMonth: String = "",
    val needsManualRecalc: Boolean = false
)