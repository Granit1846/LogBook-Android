package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Vk250002Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface Vk250002Dao {

    @Query("SELECT * FROM vk250002_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeByProfile(profileId: Long): Flow<List<Vk250002Entity>>

    @Query("SELECT * FROM vk250002_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByProfile(profileId: Long): List<Vk250002Entity>

    @Query("SELECT * FROM vk250002_records WHERE profileId = :profileId AND date = :date AND isHistorical = 0 LIMIT 1")
    suspend fun getByDate(profileId: Long, date: String): Vk250002Entity?

    @Query("DELETE FROM vk250002_records WHERE profileId = :profileId AND date = :date AND isTotal = 0")
    suspend fun deleteRowsByDate(profileId: Long, date: String)

    @Insert
    suspend fun insert(record: Vk250002Entity): Long

    @Update
    suspend fun update(record: Vk250002Entity)

    @Delete
    suspend fun delete(record: Vk250002Entity)

    @Query("DELETE FROM vk250002_records WHERE profileId = :profileId")
    suspend fun deleteAllByProfile(profileId: Long)
}
