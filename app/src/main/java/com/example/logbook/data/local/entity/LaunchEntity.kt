package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "launches",
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class LaunchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val order: Int = 0,
    val vsu: Int = 0,               // ВСУ, минуты
    val zemlya: Int = 0,            // Земля, минуты
    val vozduh: Int = 0,            // Воздух, минуты
    val tp: Int = 0,                // ТП, минуты
    val samolet: Int = 0,           // Сам., посадки

    // Заправка
    val zapravkaLiters: Int = 0,        // Литры, целое
    val zapravkaDensity: Double = 0.0,    // Плотность, дробное
    val zapravkaKg: Double = 0.0,       // Кг, рассчитывается
    val vedCek: String = ""             // Ведомость/чек, текст
) {
    val type: String
        get() = when {
            vozduh > 0 -> "полет"
            tp > 0 -> "ТП"
            zemlya > 0 -> "проба"
            vsu > 0 -> "запуск"
            zapravkaLiters > 0 -> "дозаправка"
            else -> "—"
        }

    // Рассчитанные килограммы
    val calculatedKg: Double
        get() = if (zapravkaLiters > 0 && zapravkaDensity > 0) {
            zapravkaLiters * zapravkaDensity
        } else 0.0
}