package com.example.logbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.logbook.data.local.entity.AggregateArchiveEntity

@Dao
interface AggregateArchiveDao {
    @Query("SELECT * FROM aggregate_archives WHERE type = :type AND number = :number LIMIT 1")
    suspend fun get(type: String, number: String): AggregateArchiveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(archive: AggregateArchiveEntity)
}
