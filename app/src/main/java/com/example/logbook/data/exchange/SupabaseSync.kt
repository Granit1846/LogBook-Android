package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

class SyncHttpError(val status: Int, val serverMessage: String? = null): Exception(when {
    serverMessage?.contains("email_not_confirmed", ignoreCase = true) == true ||
    serverMessage?.contains("Email not confirmed", ignoreCase = true) == true ->
        "Email не подтверждён. Перейдите по ссылке из письма для активации аккаунта"
    serverMessage?.contains("invalid_credentials", ignoreCase = true) == true ||
    serverMessage?.contains("Invalid login credentials", ignoreCase = true) == true ->
        "Неверный email или пароль. Если вы ещё не регистрировались, нажмите «Регистрация»"
    serverMessage?.contains("user_already_exists", ignoreCase = true) == true ||
    serverMessage?.contains("User already registered", ignoreCase = true) == true ->
        "Пользователь с таким email уже зарегистрирован. Нажмите «Войти»"
    serverMessage?.contains("email_address_invalid", ignoreCase = true) == true ->
        "Некорректный адрес email"
    serverMessage?.contains("Password should be at least", ignoreCase = true) == true ->
        "Пароль должен содержать не менее 6 символов"
    serverMessage?.contains("refresh_token_not_found", ignoreCase = true) == true ||
    serverMessage?.contains("Invalid Refresh Token", ignoreCase = true) == true ->
        tr(R.string.msg_sign_in_again_ee43ece)
    !serverMessage.isNullOrBlank() -> serverMessage
    status == 401 -> tr(R.string.msg_sign_in_again_ee43ece)
    status == 400 || status == 422 -> tr(R.string.msg_check_your_email_and_password_registration_req_900bfe2)
    status == 403 -> tr(R.string.msg_no_access_to_synchronization_322abe0)
    status == 404 -> tr(R.string.msg_the_synchronization_server_is_not_configured_cd72ac7)
    status == 429 -> tr(R.string.msg_too_many_requests_try_again_later_fc920d5)
    else -> tr(R.string.msg_server_unavailable_http_1_s_retry_synchronizat_1fde0ad, status)
})

data class SyncSession(val endpoint: String,val user: String,val email: String,val access: String,val refresh: String,val expires: Long)
data class SharedProfileInfo(val profileId: String,val name: String,val boardNumber: String,val revision: Long,val updatedAt: Long)

/** Tokens are device-bound and excluded from backup. Passwords are never persisted. */
class SyncAuth(context: Context) {
    private val prefs=context.getSharedPreferences("sync_auth",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("logbook_sync_auth",null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("logbook_sync_auth",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(): SyncSession? = try {
        val value=prefs.getString("session",null)
        if(value==null) null else {
            val parts=value.split(':'); val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
            com.google.gson.Gson().fromJson(String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8),SyncSession::class.java)
        }
    } catch(_: Exception) { clear();null }
    fun write(session: SyncSession) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val bytes=cipher.doFinal(com.google.gson.Gson().toJson(session).toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("session",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(bytes,Base64.NO_WRAP)).commit())
    }
    fun clear() { prefs.edit().clear().commit() }
}

class SupabaseSync(private val endpoint: String,private val publicKey: String,private val auth: SyncAuth,
    private val sharedSpace: Boolean = false): SyncRemote {
    init {
        val url=URL(endpoint)
        require(url.protocol=="https" && url.userInfo==null && url.query==null && url.ref==null && url.path.trim('/').isEmpty()) { tr(R.string.msg_enter_the_supabase_project_s_https_address_eb4206e) }
        val safe=publicKey.startsWith("sb_publishable_") || runCatching {
            JsonParser.parseString(String(Base64.decode(publicKey.split('.')[1],Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8)).asJsonObject.get("role").asString=="anon"
        }.getOrDefault(false)
        require(safe) { tr(R.string.msg_a_public_supabase_key_publishable_or_anon_is_r_ce08f48) }
    }
    private fun request(path: String,body: JsonObject,token: String?=null): JsonObject {
        val connection=URL(endpoint+path).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod="POST";connection.instanceFollowRedirects=false
            connection.connectTimeout=15000;connection.readTimeout=30000;connection.doOutput=true
            connection.setRequestProperty("apikey",publicKey);connection.setRequestProperty("Content-Type","application/json")
            token?.let { connection.setRequestProperty("Authorization","Bearer $it") }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status=connection.responseCode
            if(status !in 200..299) {
                val errorText = runCatching {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                val parsedMsg = errorText?.let {
                    runCatching {
                        val obj = JsonParser.parseString(it).asJsonObject
                        obj.get("error_description")?.asString
                            ?: obj.get("msg")?.asString
                            ?: obj.get("message")?.asString
                            ?: obj.get("error_code")?.asString
                    }.getOrNull()
                }
                throw SyncHttpError(status, parsedMsg ?: errorText)
            }
            val bytes=connection.inputStream.use { input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) { val count=input.read(buffer);if(count<0) break
                    require(output.size()+count<=64*1024*1024) { tr(R.string.msg_the_server_response_exceeds_64_mb_554657f) };output.write(buffer,0,count) }
                output.toByteArray()
            }
            return JsonParser.parseString(String(bytes,Charsets.UTF_8)).asJsonObject
        } finally { connection.disconnect() }
    }
    private fun session(response: JsonObject): SyncSession = SyncSession(endpoint,
        response.getAsJsonObject("user").get("id").asString,response.getAsJsonObject("user").get("email").asString,
        response.get("access_token").asString,response.get("refresh_token").asString,
        System.currentTimeMillis()+response.get("expires_in").asLong*1000).also(auth::write)
    fun signIn(email: String,password: String,register: Boolean): Boolean {
        val response=request(if(register) "/auth/v1/signup" else "/auth/v1/token?grant_type=password",
            JsonObject().apply { addProperty("email",email.trim());addProperty("password",password) })
        if(!response.has("access_token") || response.get("access_token").isJsonNull) return false
        session(response);return true
    }
    private fun token(refresh: Boolean=false): String {
        val current=auth.read() ?: error(tr(R.string.msg_sign_in_to_your_account_4eb58e1))
        require(current.endpoint==endpoint) { tr(R.string.msg_the_server_address_changed_sign_in_again_2a2a9b5) }
        if(!refresh && current.expires>System.currentTimeMillis()+60000) return current.access
        return try {
            session(request("/auth/v1/token?grant_type=refresh_token",JsonObject().apply { addProperty("refresh_token",current.refresh) })).access
        } catch(error: Exception) {
            auth.clear()
            throw SyncHttpError(401, tr(R.string.msg_sign_in_again_ee43ece))
        }
    }
    private fun rpc(name: String,args: JsonObject): JsonObject = try { request("/rest/v1/rpc/$name",args,token()) }
        catch(error: SyncHttpError) { if(error.status!=401) throw error;request("/rest/v1/rpc/$name",args,token(true)) }
    override suspend fun pull(): RemoteCheckpoint {
        val response=rpc(if(sharedSpace) "logbook_shared_sync_pull_v1" else "logbook_sync_pull_v1",JsonObject());val payload=response.get("payload")
        return RemoteCheckpoint(response.get("revision").asLong,if(payload==null || payload.isJsonNull) null else payload.toString())
    }
    override suspend fun commit(change: String,expected: Long,payload: String): CommitReply {
        require(change.isNotBlank()) { tr(R.string.msg_invalid_server_response_3ecf8d7) }
        require(expected >= 0) { tr(R.string.msg_invalid_server_response_3ecf8d7) }
        require(payload.isNotBlank()) { tr(R.string.msg_invalid_server_response_3ecf8d7) }
        val parsedPayload = JsonParser.parseString(payload).asJsonObject
        val response=rpc(if(sharedSpace) "logbook_shared_sync_commit_v1" else "logbook_sync_commit_v1",JsonObject().apply {
            addProperty("p_change_id",change);addProperty("p_expected_revision",expected);add("p_payload",parsedPayload) })
        return CommitReply(response.get("accepted").asBoolean,response.get("revision").asLong)
    }
    suspend fun publishSharedProfiles(profiles: List<SharedProfilePacket>): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        profiles.forEach { profile -> rpc("logbook_shared_profile_publish_v2",JsonObject().apply {
            addProperty("p_profile_id",profile.profileId);addProperty("p_name",profile.name)
            addProperty("p_board_number",profile.boardNumber);add("p_payload",JsonParser.parseString(profile.payload))
        }) }
        profiles.size
    }
    suspend fun sharedProfileCatalog(): List<SharedProfileInfo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        rpc("logbook_shared_profile_catalog_v2",JsonObject()).getAsJsonArray("profiles").map { token ->
            token.asJsonObject.let { SharedProfileInfo(it.get("profileId").asString,it.get("name").asString,
                it.get("boardNumber").asString,it.get("revision").asLong,it.get("updatedAt").asLong) }
        }
    }
    suspend fun pullSharedProfiles(profileIds: Set<String>): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        profileIds.map { id ->
            val rows=rpc("logbook_shared_profile_pull_v2",JsonObject().apply { add("p_profile_ids",JsonArray().apply { add(id) }) }).getAsJsonArray("profiles")
            require(rows.size()==1) { tr(R.string.msg_some_profiles_are_no_longer_available) }
            rows[0].asJsonObject.get("payload").toString()
        }
    }
}
