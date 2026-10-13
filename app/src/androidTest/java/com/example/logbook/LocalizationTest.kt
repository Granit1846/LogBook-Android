package com.example.logbook

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.logbook.data.exchange.SyncStore
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.example.logbook.data.model.*
import com.example.logbook.localization.I18n
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID
import java.time.LocalDate

class LocalizationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() { I18n.initialize(context);I18n.changeLanguage("ru") }
    @After fun reset() { I18n.changeLanguage("ru") }
    @Test fun everyEnglishResourceFormatsAndLanguagePersists() {
        I18n.changeLanguage("en")
        R.string::class.java.fields.filter { it.name.startsWith("msg_") }.forEach { field ->
            val text=I18n.tr(field.getInt(null), *Array<Any>(8) { "value" })
            assertFalse("Untranslated ${field.name}: $text",text.contains(Regex("[А-Яа-яЁё]")) && text != "Русский")
            assertFalse("Unexpanded format ${field.name}",text.contains(Regex("%[0-9]+\\\$s")))
        }
        assertEquals("en",context.getSharedPreferences("app_locale",Context.MODE_PRIVATE).getString("language",null))
        assertEquals("Received",I18n.display("Получено"))
        assertEquals("Service life exhausted",ResourceWarningLevel.EXPIRED.label)
        I18n.changeLanguage("ru")
        assertEquals("Ресурс исчерпан",ResourceWarningLevel.EXPIRED.label)
    }
    @Test fun releaseNotesFollowSelectedLanguage() {
        val release=com.example.logbook.updates.ReleaseManifest.parse("""{"version":"1.1.1","versionCode":3,"notes":"Правки","notesEn":"Changes"}""")
        assertEquals("Правки",release.localizedNotes)
        I18n.changeLanguage("en");assertEquals("Changes",release.localizedNotes)
    }
    @Test fun changingLanguageDoesNotChangeExchangeOrCanonicalAirframe()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val profile=ProfileEntity(name="Настройки",boardNumber="04008",fuselageSne=100,
                resourcesJson="""[{"name":"Фюзеляж","sne":0,"extendedTo1":200}]""")
            val id=db.profileDao().insert(profile)
            val day=db.dayDao().insert(DayEntity(profileId=id,date="11.10.2026",crew="Петров"))
            val launch=LaunchEntity(dayId=day,vozduh=10);db.launchDao().insert(launch)
            val store=SyncStore(context,db);val russian=store.snapshot()
            I18n.changeLanguage("en")
            assertTrue(SyncStore.same(russian,store.snapshot()))
            assertEquals("полет",launch.type);assertEquals("Настройки",db.profileDao().getById(id)!!.name)
            assertTrue(ResourceWarnings.forProfile(profile,LocalDate.of(2026,10,11)).single().text.contains("1:40"))
        } finally { db.close() }
    }
    @Test fun migration26To27KeepsBookAndPendingUpload()=runBlocking {
        val name="migration-${UUID.randomUUID()}.db"
        var db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            db.profileDao().insert(ProfileEntity(name="Сохранённый борт"))
            db.syncStateDao().save(SyncStateEntity(endpoint="cloud",accountId="user",pendingId="pending-id",pendingRevision=8,pendingJson="{}"))
            db.close()
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("ALTER TABLE sync_state DROP COLUMN checkpointsJson")
                it.version=26
            }
            db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_26_27).build()
            assertEquals("Сохранённый борт",db.profileDao().getAll().single().name)
            val saved=db.syncStateDao().get()!!
            assertEquals("pending-id",saved.pendingId);assertEquals(8L,saved.pendingRevision);assertEquals("{}",saved.checkpointsJson)
        } finally { db.close();context.deleteDatabase(name) }
    }
}
