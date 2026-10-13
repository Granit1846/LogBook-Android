package com.example.logbook

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.*
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SyncTest {
    private lateinit var context: Context
    private lateinit var dir: File
    private lateinit var a: AppDatabase
    private lateinit var b: AppDatabase
    private lateinit var sa: SyncStore
    private lateinit var sb: SyncStore
    private val remote=MemoryRemote()
    private fun engine(store: SyncStore)=SyncEngine(store,remote,"https://test.supabase.co","account")
    @Before fun setup() {
        val app=ApplicationProvider.getApplicationContext<Context>();dir=File(app.cacheDir,"sync-test-${UUID.randomUUID()}").apply {mkdirs()}
        context=object: ContextWrapper(app) { override fun getFilesDir()=dir;override fun getApplicationContext(): Context=this }
        a=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build();b=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        sa=SyncStore(context,a);sb=SyncStore(context,b)
    }
    @After fun cleanup() { a.close();b.close();dir.deleteRecursively() }
    private suspend fun seed(db: AppDatabase): Long {
        val id=db.profileDao().insert(ProfileEntity(name="Борт",boardNumber="04008",selectedTab=3))
        val day=db.dayDao().insert(DayEntity(profileId=id,date="10.10.2026",crew="Петров",isClosed=true))
        db.launchDao().insert(LaunchEntity(dayId=day))
        db.chast1Dao().insert(Chast1Entity(profileId=id,date="10.10.2026"))
        db.vr80Dao().insert(Vr80Entity(profileId=id,date="10.10.2026",col7=15))
        db.ta14Dao().insert(Ta14Entity(profileId=id,date="10.10.2026"))
        db.vk250001Dao().insert(Vk250001Entity(profileId=id,date="10.10.2026",b=0.736))
        db.vk250002Dao().insert(Vk250002Entity(profileId=id,date="10.10.2026"))
        return id
    }
    @Test fun emptyDeviceDownloadsUuidRelationsAndKeepsSettings()=runBlocking {
        seed(a);b.appSettingsDao().insert(AppSettingsEntity(theme="Dark"))
        val removed=b.profileDao().insert(ProfileEntity(name="Удалён"));b.openHelper.writableDatabase.execSQL("DELETE FROM profiles")
        assertNull(engine(sa).synchronize().conflict);assertTrue(engine(sb).synchronize().downloaded)
        val root=JsonParser.parseString(sb.snapshot()).asJsonObject;val profile=b.profileDao().getById(removed+1)!!
        assertEquals("04008",profile.boardNumber);assertEquals(-1,profile.selectedTab)
        assertEquals("Dark",b.appSettingsDao().get()!!.theme)
        assertEquals(1,b.dayDao().countByProfile(profile.id));assertEquals(0.736,b.vk250001Dao().getByProfile(profile.id).single().b,0.0)
        assertTrue(SyncStore.same(sa.snapshot(),root.toString()))
    }
    @Test fun wholeDayDeletionDoesNotReturnFromStaleDevice()=runBlocking {
        val id=seed(a);engine(sa).synchronize();engine(sb).synchronize()
        a.withTransaction { listOf("launches","days","chast1_records","vr80_records","ta14_records","vk250001_records","vk250002_records").forEach {a.openHelper.writableDatabase.execSQL("DELETE FROM `$it`")} }
        engine(sa).synchronize();assertTrue(engine(sb).synchronize().downloaded)
        assertEquals(0,b.dayDao().countByProfile(id));assertTrue(SyncStore.same(sa.snapshot(),sb.snapshot()));assertEquals(2L,remote.revision)
    }
    @Test fun sharedProfileImportKeepsUnselectedLocalProfiles()=runBlocking {
        seed(a);val localId=b.profileDao().insert(ProfileEntity(name="Локальный",boardNumber="LOCAL"))
        val packets=sa.sharedProfilePackets();assertEquals(1,packets.size)
        val backup=sb.importSharedProfiles(listOf(packets.single().payload))
        val profiles=b.profileDao().getAll();assertEquals(2,profiles.size)
        assertNotNull(b.profileDao().getById(localId));assertTrue(profiles.any { it.boardNumber=="04008" })
        assertEquals(1,b.dayDao().countByProfile(profiles.single { it.boardNumber=="04008" }.id))
        assertTrue(File(dir,"backups/sync/$backup").exists())
    }
    @Test fun simultaneousEditsRequireChoiceAndServerChoiceCreatesBackup()=runBlocking {
        val id=seed(a);engine(sa).synchronize();engine(sb).synchronize()
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="Android A"))
        b.profileDao().update(b.profileDao().getById(id)!!.copy(name="Android B"))
        engine(sa).synchronize();val conflict=engine(sb).synchronize().conflict!!
        assertEquals("Android B",b.profileDao().getById(id)!!.name)
        assertTrue(engine(sb).resolve(conflict,false).downloaded)
        assertEquals("Android A",b.profileDao().getById(id)!!.name);assertTrue(File(dir,"backups/before-restore.json").exists())
    }
    @Test fun lostAcknowledgementRetriesSameChangeAfterRestartWithoutDuplicateRevision()=runBlocking {
        val id=seed(a);remote.loseReply=true
        try { engine(sa).synchronize();fail("Expected connection failure") } catch(_: IOException) {}
        val pending=sa.state().pendingId;assertTrue(pending.isNotBlank());assertEquals(1L,remote.revision)
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="Новая правка"))
        engine(sa).synchronize();assertEquals(2L,remote.revision);assertEquals(pending,remote.calls[1]);assertEquals("",sa.state().pendingId)
        assertEquals("Новая правка",a.profileDao().getById(id)!!.name)
    }
    @Test fun freshServerWriteDuringConflictResolutionRequiresNewReview()=runBlocking {
        val id=seed(a);engine(sa).synchronize();engine(sb).synchronize()
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="A"));b.profileDao().update(b.profileDao().getById(id)!!.copy(name="B"))
        engine(sa).synchronize();val conflict=engine(sb).synchronize().conflict!!
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="A2"));engine(sa).synchronize()
        val fresh=engine(sb).resolve(conflict,true).conflict!!;assertEquals(3L,fresh.revision);assertEquals("B",b.profileDao().getById(id)!!.name)
        engine(sb).resolve(fresh,true);assertEquals(4L,remote.revision)
    }
    @Test fun malformedPacketAndConcurrentLocalEditCannotOverwriteData()=runBlocking {
        val id=seed(a);val original=sa.snapshot();val malformed=JsonParser.parseString(original).asJsonObject
        malformed.getAsJsonObject("tables").getAsJsonArray("profiles")[0].asJsonObject.remove("name")
        try { sa.download(malformed.toString(),original,1,"test","account");fail("Malformed packet accepted") } catch(_: IllegalArgumentException) {}
        assertTrue(SyncStore.same(original,sa.snapshot()))
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="Новая правка"))
        try { sa.download(original,original,1,"test","account");fail("Concurrent edit overwritten") } catch(_: IllegalArgumentException) {}
        assertEquals("Новая правка",a.profileDao().getById(id)!!.name)
    }
    @Test fun editsDuringUploadRemainLocalAndAreSentNextTime()=runBlocking {
        val id=seed(a)
        remote.beforeReply={ a.profileDao().update(a.profileDao().getById(id)!!.copy(name="Во время отправки")) }
        val result=engine(sa).synchronize();assertTrue(result.message.contains("Новые локальные"));assertEquals(1L,remote.revision)
        remote.beforeReply=null;engine(sa).synchronize();assertEquals(2L,remote.revision);assertTrue(SyncStore.same(remote.payload!!,sa.snapshot()))
    }
    @Test fun pendingCloudUploadSurvivesLanSwitchAndRestart()=runBlocking {
        val id=seed(a);remote.loseReply=true
        try { engine(sa).synchronize();fail("Missing lost response") } catch (_:IOException) {}
        val pending=sa.state().pendingId
        val wire=sa.snapshot()
        val lan=MemoryRemote()
        assertNull(SyncEngine(sa,lan,"lan:pc-one","pc-one").synchronize().conflict)
        assertTrue(SyncStore.same(wire,sa.snapshot()))
        assertEquals(pending,sa.checkpoint("https://test.supabase.co","account").pendingId)
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="После Wi-Fi"))
        assertNull(engine(SyncStore(context,a)).synchronize().conflict)
        assertEquals(pending,remote.calls[1]);assertEquals(2L,remote.revision)
        assertEquals(1L,sa.checkpoint("lan:pc-one","pc-one").baseRevision)
        assertTrue(SyncStore.same(remote.payload!!,sa.snapshot()))
    }
    @Test fun legacyCheckpointAndSeparateAccountsRetainPendingChanges()=runBlocking {
        seed(a);val wire=sa.snapshot()
        sa.save(sa.state().copy(endpoint="cloud",accountId="first",baseRevision=7,baseJson=wire,
            pendingId="legacy-pending",pendingJson=wire,pendingRevision=7,lastSuccess=123))
        val identities=sa.state().identityJson
        val second=sa.checkpoint("cloud","second")
        assertEquals(0L,second.baseRevision)
        sa.saveCheckpoint(second.copy(baseRevision=2,baseJson=wire,lastSuccess=456))
        val legacy=SyncStore(context,a).checkpoint("cloud","first")
        assertEquals("legacy-pending",legacy.pendingId);assertEquals(7L,legacy.baseRevision)
        assertEquals(123L,legacy.lastSuccess);assertEquals(identities,legacy.identityJson)
        assertEquals(456L,sa.checkpoint("cloud","second").lastSuccess)
        assertTrue(SyncStore.same(wire,sa.snapshot()))
    }
    @Test fun downloadPreservesOtherPeerCheckpoint()=runBlocking {
        seed(a);engine(sa).synchronize();engine(sb).synchronize()
        val cloudBase=sb.checkpoint("https://test.supabase.co","account").baseJson
        val id=a.profileDao().getAll().single().id
        a.profileDao().update(a.profileDao().getById(id)!!.copy(name="Другая версия"))
        val local=sb.snapshot();sb.download(sa.snapshot(),local,8,"lan:other","other")
        assertEquals(cloudBase,sb.checkpoint("https://test.supabase.co","account").baseJson)
        assertEquals(8L,sb.checkpoint("lan:other","other").baseRevision)
        assertTrue(engine(sb).synchronize().conflict==null)
        assertEquals(2L,remote.revision)
    }
    private class MemoryRemote: SyncRemote {
        var revision=0L;var payload: String?=null;var loseReply=false;var beforeReply: (suspend () -> Unit)?=null
        val calls=mutableListOf<String>();private val accepted=mutableMapOf<String,Pair<String,Long>>()
        override suspend fun pull()=RemoteCheckpoint(revision,payload)
        override suspend fun commit(change: String,expected: Long,payload: String): CommitReply {
            calls.add(change);accepted[change]?.let {require(it.first==payload);return CommitReply(true,it.second)}
            if(expected!=revision) return CommitReply(false,revision)
            revision++;this.payload=payload;accepted[change]=payload to revision;beforeReply?.invoke()
            if(loseReply) {loseReply=false;throw IOException("Lost response")}
            return CommitReply(true,revision)
        }
    }
}
