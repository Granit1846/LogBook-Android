package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.Vr80Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface Vr80Dao {

    @Query("SELECT * FROM vr80_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun observeByProfile(profileId: Long): Flow<List<Vr80Entity>>

    @Query("SELECT * FROM vr80_records WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByProfile(profileId: Long): List<Vr80Entity>

    @Query("SELECT * FROM vr80_records WHERE profileId = :profileId AND date = :date AND isHistorical = 0 LIMIT 1")
    suspend fun getByDate(profileId: Long, date: String): Vr80Entity?

    @Query("DELETE FROM vr80_records WHERE profileId = :profileId AND date = :date AND isTotal = 0")
    suspend fun deleteRowsByDate(profileId: Long, date: String)

    @Insert
    suspend fun insert(record: Vr80Entity): Long

    @Update
    suspend fun update(record: Vr80Entity)

    @Delete
    suspend fun delete(record: Vr80Entity)

    @Query("DELETE FROM vr80_records WHERE profileId = :profileId")
    suspend fun deleteAllByProfile(profileId: Long)
}
