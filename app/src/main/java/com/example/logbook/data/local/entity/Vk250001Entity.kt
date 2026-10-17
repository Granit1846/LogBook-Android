package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.example.logbook.data.model.Vk2500RecordItem

@Entity(
    tableName = "vk250001_records",
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
data class Vk250001Entity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    override val profileId: Long,
    override val isHistorical: Boolean = false,

    override val date: String = "",
    override val sortOrder: Long = 0,

    override val i: Int = 0,
    override val g: Double = 0.0,
    override val colSNE: Int = 0,
    override val pprA: Int = 0,
    override val col8: Double = 0.0,
    override val b: Double = 0.0,
    override val col10: Double = 0.0,
    override val col11: Double = 0.0,
    override val col12: Double = 0.0,
    override val d: Double = 0.0,
    override val e: Double = 0.0,
    override val h: Double = 0.0,
    override val pprH: Double = 0.0,

    override val isTotal: Boolean = false,
    override val totalMonth: String = "",
    override val needsManualRecalc: Boolean = false
) : Vk2500RecordItem
