package com.example.logbook.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.SnkEntity

data class DayDetails(
    @Embedded val day: DayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val launches: List<LaunchEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val snk: SnkEntity?
)
