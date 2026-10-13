package com.example.logbook.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity

data class DayWithLaunches(
    @Embedded val day: DayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val launches: List<LaunchEntity>
)