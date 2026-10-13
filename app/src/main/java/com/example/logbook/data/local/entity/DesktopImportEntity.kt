package com.example.logbook.data.local.entity

import androidx.room.*

@Entity(tableName = "desktop_imports", foreignKeys = [ForeignKey(entity = ProfileEntity::class,
    parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sourceKey"], unique = true), Index(value = ["profileId"], unique = true)])
data class DesktopImportEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long, val sourceKey: String, val sharedId: String, val sourceHash: String,
    val originalJson: String, val importedAt: Long = System.currentTimeMillis())
