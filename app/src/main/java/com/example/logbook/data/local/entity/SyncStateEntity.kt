package com.example.logbook.data.local.entity
import androidx.room.*

@Entity(tableName="sync_state")
data class SyncStateEntity(@PrimaryKey val id: Int = 1, val endpoint: String = "", val accountId: String = "",
    val identityJson: String = "{}", val baseRevision: Long = 0, val baseJson: String = "",
    val pendingId: String = "", val pendingJson: String = "", val pendingRevision: Long = 0, val lastSuccess: Long = 0, @ColumnInfo(defaultValue = "'{}'") val checkpointsJson: String = "{}")
