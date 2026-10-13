package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Ta14StartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Ta14StartDao {

    @Query("SELECT * FROM ta14_start WHERE profileId = :profileId LIMIT 1")
    fun observeByProfile(profileId: Long): Flow<Ta14StartEntity?>

    @Query("SELECT * FROM ta14_start WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: Long): Ta14StartEntity?

    @Insert
    suspend fun insert(start: Ta14StartEntity): Long

    @Update
    suspend fun update(start: Ta14StartEntity)

    @Query("DELETE FROM ta14_start WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}