package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.LaunchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchDao {

    @Query("SELECT * FROM launches WHERE dayId = :dayId ORDER BY `order` ASC, id ASC")
    fun observeByDay(dayId: Long): Flow<List<LaunchEntity>>

    @Query("SELECT * FROM launches WHERE dayId = :dayId ORDER BY `order` ASC, id ASC")
    suspend fun getByDay(dayId: Long): List<LaunchEntity>

    @Insert
    suspend fun insert(launch: LaunchEntity): Long

    @Update
    suspend fun update(launch: LaunchEntity)

    @Delete
    suspend fun delete(launch: LaunchEntity)

    @Query("DELETE FROM launches WHERE dayId = :dayId")
    suspend fun deleteAllByDay(dayId: Long)

    @Query("SELECT MAX(`order`) FROM launches WHERE dayId = :dayId")
    suspend fun maxOrder(dayId: Long): Int?
}