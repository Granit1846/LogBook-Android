package com.example.logbook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.*
import com.google.gson.JsonParser
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@RunWith(AndroidJUnit4::class)
class SyncNetworkTest {
    @Test fun projectAuthIsReachableAndAnonymousRpcIsDenied() {
        val url=URL(SyncDefaults.URL+"/auth/v1/settings").openConnection() as HttpsURLConnection
        try {
            url.connectTimeout=15000;url.readTimeout=15000;url.setRequestProperty("apikey",SyncDefaults.KEY)
            assertEquals(200,url.responseCode)
            val response=url.inputStream.bufferedReader().use {it.readText()}
            assertTrue(JsonParser.parseString(response).asJsonObject.has("external"))
        } finally {url.disconnect()}
        val rpc=URL(SyncDefaults.URL+"/rest/v1/rpc/logbook_sync_pull_v1").openConnection() as HttpsURLConnection
        try {
            rpc.requestMethod="POST";rpc.doOutput=true;rpc.connectTimeout=15000;rpc.readTimeout=15000
            rpc.setRequestProperty("apikey",SyncDefaults.KEY);rpc.setRequestProperty("Content-Type","application/json")
            rpc.outputStream.use {it.write("{}".toByteArray())}
            assertTrue(rpc.responseCode==401 || rpc.responseCode==403)
        } finally {rpc.disconnect()}
    }
    @Test fun authTokensAreEncryptedAndUnsafeClientKeysAreRejected() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val suffix=java.util.UUID.randomUUID().toString()
        val wrapped=object: android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String,mode: Int)=super.getSharedPreferences("sync-network-$suffix-$name",mode)
        }
        val auth=SyncAuth(wrapped)
        val session=SyncSession(SyncDefaults.URL,"user","email@example.test","access-token","refresh-token",1234)
        try {
            auth.write(session);assertEquals(session,auth.read())
            val saved=wrapped.getSharedPreferences("sync_auth",Context.MODE_PRIVATE).getString("session","")!!
            assertFalse(saved.contains("access-token"));assertFalse(saved.contains("refresh-token"))
            try {SupabaseSync(SyncDefaults.URL,"sb_secret_not-a-client-key",auth);fail("Secret key accepted")} catch(_: IllegalArgumentException) {}
            try {SupabaseSync("http://example.test",SyncDefaults.KEY,auth);fail("Plain HTTP accepted")} catch(_: IllegalArgumentException) {}
        } finally {auth.clear()}
    }
}
