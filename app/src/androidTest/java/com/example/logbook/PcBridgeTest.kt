package com.example.logbook

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.*
import com.example.logbook.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PcBridgeTest {
    @Test fun actualCSharpPacketIsAcceptedAndAndroidReturnKeepsItsIdentity()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<Context>();val dir=File(app.cacheDir,"pc-bridge-${UUID.randomUUID()}").apply {mkdirs()}
        val context=object: ContextWrapper(app) {override fun getFilesDir()=dir;override fun getApplicationContext(): Context=this}
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val packet=InstrumentationRegistry.getInstrumentation().context.assets.open("pc-packet.json").bufferedReader().use {it.readText()}
            val store=SyncStore(context,db);store.download(packet,store.snapshot(),1,"test","account")
            assertEquals(2,db.profileDao().getAll().size)
            assertTrue(SyncStore.same(packet,store.snapshot()))
            val first=db.profileDao().getAll().first();val day=db.dayDao().getByDate(first.id,"05.10.2026")
            val sql=db.openHelper.writableDatabase
            sql.execSQL("UPDATE launches SET zapravkaLiters=150, zapravkaDensity=0.8, zapravkaKg=120.0, vedCek='чек Android' WHERE id=(SELECT min(id) FROM launches)")
            File(app.filesDir,"pc-bridge-return.json").writeText(store.snapshot())
        } finally {db.close();dir.deleteRecursively()}
    }
    @Test fun packetEditedOnPcAfterAndroidRoundTripIsAccepted()=runBlocking {
        val assets=InstrumentationRegistry.getInstrumentation().context.assets
        val app=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(app,AppDatabase::class.java).build()
        try {
            val packet=assets.open("pc-return.json").bufferedReader().use {it.readText()};val store=SyncStore(app,db)
            store.download(packet,store.snapshot(),3,"test","account")
            assertTrue(SyncStore.same(packet,store.snapshot()))
            db.openHelper.readableDatabase.query("SELECT count(*) FROM days WHERE crew='Экипаж ПК'").use {it.moveToFirst();assertEquals(1,it.getInt(0))}
            db.openHelper.readableDatabase.query("SELECT zapravkaLiters,vedCek FROM launches WHERE zapravkaLiters>0").use {assertTrue(it.moveToFirst());assertEquals(150,it.getInt(0));assertEquals("чек Android",it.getString(1))}
        } finally {db.close()}
    }
}
