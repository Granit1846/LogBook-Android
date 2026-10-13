package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Ta14Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface Ta14Dao {

    @Query("SELECT * FROM ta14_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeByProfile(profileId: Long): Flow<List<Ta14Entity>>

    @Query("SELECT * FROM ta14_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByProfile(profileId: Long): List<Ta14Entity>

    @Query("SELECT * FROM ta14_records WHERE profileId = :profileId AND date = :date AND isHistorical = 0 LIMIT 1")
    suspend fun getByDate(profileId: Long, date: String): Ta14Entity?

    @Query("DELETE FROM ta14_records WHERE profileId = :profileId AND date = :date AND isTotal = 0")
    suspend fun deleteRowsByDate(profileId: Long, date: String)

    @Insert
    suspend fun insert(record: Ta14Entity): Long

    @Update
    suspend fun update(record: Ta14Entity)

    @Delete
    suspend fun delete(record: Ta14Entity)

    @Query("DELETE FROM ta14_records WHERE profileId = :profileId")
    suspend fun deleteAllByProfile(profileId: Long)
}
