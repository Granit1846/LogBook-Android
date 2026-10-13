package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chast1_start",
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
data class Chast1StartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,

    val date: String = "",
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
    val mechanic: String = ""
)