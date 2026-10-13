package com.example.logbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,

    val language: String = "ru",
    val theme: String = "System",
    val notifications: Boolean = true,

    val supabaseUrl: String = "",
    val supabaseKey: String = "",

    val autoSave: Boolean = true,
    val autoCheckUpdates: Boolean = true,

    // Zoom формуляров (per-formular)
    val chast1Zoom: Float = 1f,
    val vr80Zoom: Float = 1f,
    val ta14Zoom: Float = 1f,
    val vk01Zoom: Float = 1f,
    val vk02Zoom: Float = 1f,

    // Автоматическая синхронизация после окончания дня
    val syncOnFinishDay: Boolean = false
)