package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.example.logbook.data.model.Vk2500StartItem

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
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    override val profileId: Long,

    override val date: String = "",
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

    // Коэффициенты
    override val coeffA: Double = 0.0,
    override val coeffH: Double = 0.0
) : Vk2500StartItem {
    override fun copyWith(
        date: String,
        i: Int,
        g: Double,
        colSNE: Int,
        pprA: Int,
        col8: Double,
        b: Double,
        col10: Double,
        col11: Double,
        col12: Double,
        d: Double,
        e: Double,
        h: Double,
        pprH: Double,
        coeffA: Double,
        coeffH: Double
    ): Vk2500StartItem = copy(
        date = date,
        i = i,
        g = g,
        colSNE = colSNE,
        pprA = pprA,
        col8 = col8,
        b = b,
        col10 = col10,
        col11 = col11,
        col12 = col12,
        d = d,
        e = e,
        h = h,
        pprH = pprH,
        coeffA = coeffA,
        coeffH = coeffH
    )
}