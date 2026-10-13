package com.example.logbook.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.logbook.data.local.entity.AggregateArchiveEntity
import com.example.logbook.data.local.dao.AggregateArchiveDao
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.logbook.data.local.dao.Chast1Dao
import com.example.logbook.data.local.dao.Chast1StartDao
import com.example.logbook.data.local.dao.DayDao
import com.example.logbook.data.local.dao.LaunchDao
import com.example.logbook.data.local.dao.ProfileDao
import com.example.logbook.data.local.dao.SnkDao
import com.example.logbook.data.local.dao.Ta14Dao
import com.example.logbook.data.local.dao.Ta14StartDao
import com.example.logbook.data.local.dao.Vk250001Dao
import com.example.logbook.data.local.dao.Vk250001StartDao
import com.example.logbook.data.local.dao.Vk250002Dao
import com.example.logbook.data.local.dao.Vk250002StartDao
import com.example.logbook.data.local.dao.Vr80Dao
import com.example.logbook.data.local.dao.Vr80StartDao
import com.example.logbook.data.local.dao.AppSettingsDao
import com.example.logbook.data.local.entity.AppSettingsEntity
import com.example.logbook.data.local.entity.Chast1Entity
import com.example.logbook.data.local.entity.Chast1StartEntity
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.local.entity.SnkEntity
import com.example.logbook.data.local.entity.Ta14Entity
import com.example.logbook.data.local.entity.Ta14StartEntity
import com.example.logbook.data.local.entity.Vk250001Entity
import com.example.logbook.data.local.entity.Vk250001StartEntity
import com.example.logbook.data.local.entity.Vk250002Entity
import com.example.logbook.data.local.entity.Vk250002StartEntity
import com.example.logbook.data.local.entity.Vr80Entity
import com.example.logbook.data.local.entity.Vr80StartEntity

@Database(
    entities = [
        ProfileEntity::class,
        Chast1Entity::class,
        Chast1StartEntity::class,
        DayEntity::class,
        LaunchEntity::class,
        SnkEntity::class,
        Vr80Entity::class,
        Vr80StartEntity::class,
        Ta14Entity::class,
        Ta14StartEntity::class,
        Vk250001Entity::class,
        Vk250001StartEntity::class,
        Vk250002Entity::class,
        Vk250002StartEntity::class,
        AppSettingsEntity::class,
        AggregateArchiveEntity::class,
        com.example.logbook.data.local.entity.DesktopImportEntity::class,
        com.example.logbook.data.local.entity.SyncStateEntity::class,
    ],
    version = 31,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncStateDao(): com.example.logbook.data.local.dao.SyncStateDao
    abstract fun desktopImportDao(): com.example.logbook.data.local.dao.DesktopImportDao
    abstract fun aggregateArchiveDao(): AggregateArchiveDao
    abstract fun profileDao(): ProfileDao
    abstract fun chast1Dao(): Chast1Dao
    abstract fun chast1StartDao(): Chast1StartDao
    abstract fun dayDao(): DayDao
    abstract fun launchDao(): LaunchDao
    abstract fun snkDao(): SnkDao
    abstract fun vr80Dao(): Vr80Dao
    abstract fun vr80StartDao(): Vr80StartDao
    abstract fun ta14Dao(): Ta14Dao
    abstract fun ta14StartDao(): Ta14StartDao
    abstract fun vk250001Dao(): Vk250001Dao
    abstract fun vk250001StartDao(): Vk250001StartDao
    abstract fun vk250002Dao(): Vk250002Dao
    abstract fun vk250002StartDao(): Vk250002StartDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN syncOnFinishDay INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE sync_state SET pendingId = '', pendingRevision = 0 WHERE pendingJson = ''")
            }
        }
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE sync_state SET baseJson = '', pendingJson = '', checkpointsJson = '{}'")
            }
        }
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN maintenanceWorksJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN defectTasksJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_state ADD COLUMN checkpointsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (id INTEGER NOT NULL PRIMARY KEY, endpoint TEXT NOT NULL, accountId TEXT NOT NULL, identityJson TEXT NOT NULL, baseRevision INTEGER NOT NULL, baseJson TEXT NOT NULL, pendingId TEXT NOT NULL, pendingJson TEXT NOT NULL, pendingRevision INTEGER NOT NULL, lastSuccess INTEGER NOT NULL)")
            }
        }
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS desktop_imports (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId INTEGER NOT NULL, sourceKey TEXT NOT NULL, sharedId TEXT NOT NULL, sourceHash TEXT NOT NULL, originalJson TEXT NOT NULL, importedAt INTEGER NOT NULL, FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_desktop_imports_sourceKey ON desktop_imports(sourceKey)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_desktop_imports_profileId ON desktop_imports(profileId)")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN aggregateInstalledAtJson TEXT NOT NULL DEFAULT '{}'")
                listOf("vr80_records", "ta14_records", "vk250001_records", "vk250002_records").forEach {
                    db.execSQL("ALTER TABLE `$it` ADD COLUMN isHistorical INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS aggregate_archives (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, number TEXT NOT NULL, formularJson TEXT NOT NULL, savedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_aggregate_archives_type_number ON aggregate_archives (type, number)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logbook.db"
                )
                    .addMigrations(MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            runCatching {
                                db.execSQL("UPDATE sync_state SET baseJson = '', pendingJson = '' WHERE baseJson != '' OR pendingJson != ''")
                                db.execSQL("UPDATE sync_state SET pendingId = '', pendingRevision = 0 WHERE pendingJson = ''")
                                db.execSQL("UPDATE sync_state SET checkpointsJson = '{}' WHERE length(checkpointsJson) > 50000")
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
