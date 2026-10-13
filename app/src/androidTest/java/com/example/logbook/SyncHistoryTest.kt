package com.example.logbook

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.logbook.data.exchange.*
import com.example.logbook.data.model.*
import com.example.logbook.data.local.AppDatabase
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SyncHistoryTest {
    @Test fun journalPersistsAndBackupFilesDoNotOverwrite()=runBlocking {
        val original=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(original.cacheDir,"history-test-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val context=object: ContextWrapper(original) {
            override fun getFilesDir()=folder
            override fun getApplicationContext()=this
        }
        val db=Room.inMemoryDatabaseBuilder(original,AppDatabase::class.java).build()
        try {
            val store=SyncStore(context,db)
            val packet=store.snapshot()
            val a=store.download(packet,packet,1,"server","account")
            val b=store.download(packet,packet,2,"server","account")
            assertNotEquals(a,b);assertTrue(File(folder,"backups/sync/$a").exists());assertTrue(File(folder,"backups/sync/$b").exists())
            val history=SyncHistory(context)
            repeat(205) { history.add(SyncHistoryEntry(it.toLong(),if(it%2==0) "Wi-Fi" else "Интернет","Получено","Успешно",a)) }
            val reopened=SyncHistory(context).read();assertEquals(200,reopened.size);assertEquals(204L,reopened.first().time);assertEquals(a,reopened.first().backup)
        } finally {db.close();folder.deleteRecursively()}
    }
    @Test fun legacyBlankValuesHavePresenceAndNoWarnings() {
        val gson=Gson()
        val resource=gson.fromJson("{\"name\":\"Пустой агрегат\",\"sne\":0,\"extendedTo1\":0}",ResourceItem::class.java)
        val oil=gson.fromJson("{\"name\":\"Пустое масло\",\"byHours\":0,\"coefficient\":0,\"term\":\"\"}",OilItem::class.java)
        assertTrue("extendedTo1" in resource.emptyFields);assertTrue("coefficient" in oil.emptyFields)
        assertEquals(ResourceWarningLevel.NONE,ResourceWarnings.aggregateHours(resource))
        assertEquals(ResourceWarningLevel.NONE,ResourceWarnings.oilHours(oil,99999))
        assertEquals("",oil.term)
    }
}
