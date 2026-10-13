package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.logbook.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastOpened(): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("UPDATE profiles SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE profiles SET maintenanceWorksJson = :value, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateMaintenanceWorks(id: Long, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE profiles SET defectTasksJson = :value, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateDefectTasks(id: Long, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProfileEntity?>
}
