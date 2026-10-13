package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vk250001_start",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"], unique = true)]
)
data class Vk250001StartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,

    val date: String = "",
    val i: Int = 0,
    val g: Double = 0.0,
    val colSNE: Int = 0,
    val pprA: Int = 0,
    val col8: Double = 0.0,
    val b: Double = 0.0,
    val col10: Double = 0.0,
    val col11: Double = 0.0,
    val col12: Double = 0.0,
    val d: Double = 0.0,
    val e: Double = 0.0,
    val h: Double = 0.0,
    val pprH: Double = 0.0,

    // Коэффициенты
    val coeffA: Double = 0.0,
    val coeffH: Double = 0.0
)