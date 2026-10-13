package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Vr80StartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Vr80StartDao {
    @Query("SELECT * FROM vr80_start WHERE profileId = :profileId LIMIT 1")
    fun observeByProfile(profileId: Long): Flow<Vr80StartEntity?>

    @Query("SELECT * FROM vr80_start WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: Long): Vr80StartEntity?

    @Insert
    suspend fun insert(start: Vr80StartEntity): Long

    @Update
    suspend fun update(start: Vr80StartEntity)

    @Query("DELETE FROM vr80_start WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}