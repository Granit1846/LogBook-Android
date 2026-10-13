package com.example.logbook.data.local.dao

import androidx.room.*
import com.example.logbook.data.local.entity.DesktopImportEntity

@Dao
interface DesktopImportDao {
    @Query("SELECT * FROM desktop_imports WHERE sourceKey = :key LIMIT 1")
    suspend fun get(key: String): DesktopImportEntity?
    @Insert suspend fun insert(receipt: DesktopImportEntity)
}
