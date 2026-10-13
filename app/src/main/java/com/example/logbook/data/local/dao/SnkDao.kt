package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.SnkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnkDao {

    @Query("SELECT * FROM snk_values WHERE dayId = :dayId LIMIT 1")
    fun observeByDay(dayId: Long): Flow<SnkEntity?>

    @Query("SELECT * FROM snk_values WHERE dayId = :dayId LIMIT 1")
    suspend fun getByDay(dayId: Long): SnkEntity?

    @Insert
    suspend fun insert(snk: SnkEntity): Long

    @Update
    suspend fun update(snk: SnkEntity)

    @Query("DELETE FROM snk_values WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)
}