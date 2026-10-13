package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Chast1Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface Chast1Dao {

    @Query("SELECT * FROM chast1_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeByProfile(profileId: Long): Flow<List<Chast1Entity>>

    @Query("SELECT * FROM chast1_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByProfile(profileId: Long): List<Chast1Entity>

    @Query("SELECT * FROM chast1_records WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getByDate(profileId: Long, date: String): Chast1Entity?

    @Query("DELETE FROM chast1_records WHERE profileId = :profileId AND date = :date AND isTotal = 0")
    suspend fun deleteRowsByDate(profileId: Long, date: String)

    @Insert
    suspend fun insert(record: Chast1Entity): Long

    @Update
    suspend fun update(record: Chast1Entity)

    @Delete
    suspend fun delete(record: Chast1Entity)

    @Query("DELETE FROM chast1_records WHERE profileId = :profileId")
    suspend fun deleteAllByProfile(profileId: Long)
}
