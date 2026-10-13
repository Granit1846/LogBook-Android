package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Vk250002StartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Vk250002StartDao {

    @Query("SELECT * FROM vk250002_start WHERE profileId = :profileId LIMIT 1")
    fun observeByProfile(profileId: Long): Flow<Vk250002StartEntity?>

    @Query("SELECT * FROM vk250002_start WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: Long): Vk250002StartEntity?

    @Insert
    suspend fun insert(start: Vk250002StartEntity): Long

    @Update
    suspend fun update(start: Vk250002StartEntity)

    @Query("DELETE FROM vk250002_start WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}