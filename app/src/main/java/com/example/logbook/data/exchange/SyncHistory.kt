package com.example.logbook.data.exchange

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class SyncHistoryEntry(val time: Long, val method: String, val direction: String, val result: String, val backup: String = "")

/** Device-local journal; contains no account credentials or transferred records. */
class SyncHistory(context: Context) {
    private val file=AtomicFile(File(context.applicationContext.filesDir,"sync-history.json"))
    companion object { private val lock=Any() }
    fun read(): List<SyncHistoryEntry> = synchronized(lock) {
        if(!file.baseFile.exists()) emptyList() else file.openRead().use {
            Gson().fromJson<List<SyncHistoryEntry>>(it.reader(),object: TypeToken<List<SyncHistoryEntry>>() {}.type)
        }
    }
    fun add(entry: SyncHistoryEntry) = synchronized(lock) {
        val entries=(listOf(entry)+read()).take(200)
        val output=file.startWrite()
        try {output.write(Gson().toJson(entries).toByteArray(Charsets.UTF_8));file.finishWrite(output)}
        catch(error: Exception){file.failWrite(output);throw error}
    }
}
