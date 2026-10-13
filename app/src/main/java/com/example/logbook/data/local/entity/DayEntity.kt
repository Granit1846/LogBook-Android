package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "days",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId", "date"], unique = true),
        Index("profileId")
    ]
)
data class DayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: String,               // dd.MM.yyyy
    val crew: String = "",
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)