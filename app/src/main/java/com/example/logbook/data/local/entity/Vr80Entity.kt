package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vr80_records",
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
data class Vr80Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val isHistorical: Boolean = false,

    val date: String = "",
    val sortOrder: Long = 0,

    val launchCount: Int = 0,
    val groundHours: Int = 0,
    val col4: Int = 0,
    val col5: Int = 0,
    val col6: Int = 0,
    val col7: Int = 0,
    val col8: Int = 0,
    val col9: Int = 0,
    val col24: Int = 0,
    val colSNE: Int = 0,
    val col26: Int = 0,
    val col27: Int = 0,
    val col28: String = "",

    val isTotal: Boolean = false,
    val totalMonth: String = "",
    val needsManualRecalc: Boolean = false
)
