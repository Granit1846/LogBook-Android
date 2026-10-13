package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ta14_start",
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
data class Ta14StartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,

    val date: String = "",
    val launchCount: Int = 0,
    val col3: Int = 0,
    val colVremya: Int = 0,
    val col5: Int = 0,
    val colSNE: Int = 0,
    val col7: Int = 0,
    val col8: Int = 0,
    val colSNE2: Int = 0,
    val launchCount2: Int = 0,
    val col11: Int = 0
)