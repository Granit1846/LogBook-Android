package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Chast1StartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Chast1StartDao {

    @Query("SELECT * FROM chast1_start WHERE profileId = :profileId LIMIT 1")
    fun observeByProfile(profileId: Long): Flow<Chast1StartEntity?>

    @Query("SELECT * FROM chast1_start WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: Long): Chast1StartEntity?

    @Insert
    suspend fun insert(start: Chast1StartEntity): Long

    @Update
    suspend fun update(start: Chast1StartEntity)

    @Query("DELETE FROM chast1_start WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}