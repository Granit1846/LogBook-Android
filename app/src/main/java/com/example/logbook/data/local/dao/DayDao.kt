package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.logbook.data.local.relation.DayWithLaunches
import com.example.logbook.data.local.relation.DayDetails
import com.example.logbook.data.local.entity.DayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    @Query("SELECT * FROM days WHERE profileId = :profileId ORDER BY date DESC")
    fun observeByProfile(profileId: Long): Flow<List<DayEntity>>

    @Query("SELECT * FROM days WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getByDate(profileId: Long, date: String): DayEntity?

    @Query("SELECT * FROM days WHERE profileId = :profileId AND date = :date LIMIT 1")
    fun observeByDate(profileId: Long, date: String): Flow<DayEntity?>

    @Transaction
    @Query("SELECT * FROM days WHERE profileId = :profileId AND date = :date LIMIT 1")
    fun observeDetailsByDate(profileId: Long, date: String): Flow<DayDetails?>

    @Query("SELECT * FROM days WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DayEntity?

    @Query("SELECT COUNT(*) FROM days WHERE profileId = :profileId")
    suspend fun countByProfile(profileId: Long): Int

    @Insert
    suspend fun insert(day: DayEntity): Long

    @Update
    suspend fun update(day: DayEntity)

    @Delete
    suspend fun delete(day: DayEntity)

    @Query("UPDATE days SET isClosed = :closed WHERE id = :dayId")
    suspend fun setClosed(dayId: Long, closed: Boolean)

    @Transaction
    @Query("SELECT * FROM days WHERE profileId = :profileId ORDER BY date ASC")
    fun observeAllWithLaunches(profileId: Long): Flow<List<DayWithLaunches>>
}
