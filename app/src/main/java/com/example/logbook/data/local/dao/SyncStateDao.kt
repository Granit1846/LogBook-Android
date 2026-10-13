package com.example.logbook.data.local.dao
import androidx.room.*
import com.example.logbook.data.local.entity.SyncStateEntity
@Dao interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id=1") suspend fun get(): SyncStateEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(state: SyncStateEntity)
}
