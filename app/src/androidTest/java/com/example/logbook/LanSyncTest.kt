package com.example.logbook

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.exchange.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LanSyncTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private fun pairing()=LanPairing.parse(File(context.filesDir,"lan-test-pairing.json").readText())
    @Test fun actualPcQrDecodesOffline() {
        val bitmap=android.graphics.BitmapFactory.decodeFile(File(context.filesDir,"lan-test-qr.png").path)
        val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        val source=com.google.zxing.RGBLuminanceSource(bitmap.width,bitmap.height,pixels)
        val result=com.google.zxing.qrcode.QRCodeReader().decode(com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source)))
        assertEquals(pairing(),LanPairing.parse(result.text));bitmap.recycle()
    }
    @Test fun pcToAndroidThenAndroidToPcAndSecurity()=runBlocking {
        val pc=pairing();val remote=LanSync(pc)
        val initial=remote.pull();assertTrue(initial.revision>0)
        context.deleteDatabase("lan-verification.db")
        val db=Room.databaseBuilder(context,AppDatabase::class.java,"lan-verification.db").build()
        try {
            val store=SyncStore(context,db);val engine=SyncEngine(store,remote,"lan:${pc.peer}",pc.peer)
            assertTrue(engine.synchronize().downloaded)
            remote.report("received")
            val packet=JsonParser.parseString(store.snapshot()).asJsonObject
            assertEquals(2,packet.getAsJsonObject("tables").getAsJsonArray("profiles").size())
            val dayId=db.openHelper.writableDatabase.query("SELECT id FROM days ORDER BY id LIMIT 1").use { it.moveToFirst();it.getLong(0) }
            val day=db.dayDao().getById(dayId)!!
            db.dayDao().update(day.copy(crew="Wi-Fi Android"))
            assertNull(engine.synchronize().conflict)
            remote.report("matched")
            val uploaded=remote.pull();assertTrue(uploaded.payload!!.contains("Wi-Fi Android"))
            val id=UUID.randomUUID().toString()
            val accepted=remote.commit(id,uploaded.revision,uploaded.payload!!);assertTrue(accepted.accepted)
            assertEquals(accepted,remote.commit(id,uploaded.revision,uploaded.payload!!))
            assertFalse(remote.commit(UUID.randomUUID().toString(),uploaded.revision,uploaded.payload!!).accepted)
            suspend fun rejected(block: suspend ()->Unit) {try {block();fail("Should reject")}catch(e: Exception) {assertTrue(e.message!=null)}}
            rejected {remote.commit(id,uploaded.revision+1,uploaded.payload!!)}
            rejected {LanSync(pc.copy(pin="0".repeat(64))).pull()}
            rejected {LanSync(pc.copy(token="0".repeat(64))).pull()}
            val invalid=JsonParser.parseString(uploaded.payload).asJsonObject;invalid.remove("tables")
            rejected {remote.commit(UUID.randomUUID().toString(),accepted.revision,invalid.toString())}
            assertEquals(accepted.revision,remote.pull().revision)
            // Acknowledges the same packet at its new revision before the subsequent PC edit.
            assertNull(engine.synchronize().conflict)
        } finally {db.close()}
    }
    @Test fun changedPcDownloadsAfterRestart()=runBlocking {
        val pc=pairing();val db=Room.databaseBuilder(context,AppDatabase::class.java,"lan-verification.db").build()
        try {
            val result=SyncEngine(SyncStore(context,db),LanSync(pc),"lan:${pc.peer}",pc.peer).synchronize()
            assertTrue(result.downloaded)
            LanSync(pc).report("received")
            assertTrue(db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM days WHERE crew='Wi-Fi ПК'").use { it.moveToFirst();it.getInt(0)>0 })
        } finally {db.close();context.deleteDatabase("lan-verification.db")}
    }
}
