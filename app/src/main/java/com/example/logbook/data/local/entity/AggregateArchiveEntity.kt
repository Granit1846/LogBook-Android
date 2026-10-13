package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "aggregate_archives", indices = [Index(value = ["type", "number"], unique = true)])
data class AggregateArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val number: String,
    val formularJson: String,
    val savedAt: Long
)
