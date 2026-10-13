package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

data class LanPairing(val host: String, val port: Int, val peer: String, val pin: String, val token: String, val expires: Long) {
    companion object {
        fun parse(text: String): LanPairing {
            require(text.length <= 2048) { tr(R.string.msg_this_is_not_a_logbook_qr_code_32fd204) }
            val value = JsonParser.parseString(text).asJsonObject
            require(value.get("format").asString == "LogBookLan" && value.get("version").asInt == 1) { tr(R.string.msg_this_is_not_a_logbook_pairing_qr_code_a04446a) }
            val result = LanPairing(value.get("host").asString, value.get("port").asInt,
                value.get("peer").asString, value.get("pin").asString, value.get("token").asString, value.get("expires").asLong)
            val parts = result.host.split('.').map { it.toIntOrNull() ?: -1 }
            require(parts.size == 4 && parts.all { it in 0..255 } &&
                (parts[0] == 10 || parts[0] == 192 && parts[1] == 168 || parts[0] == 172 && parts[1] in 16..31)) { tr(R.string.msg_the_qr_code_must_contain_a_local_network_addre_2e0a28e) }
            require(result.port in 1..65535 && result.pin.matches(Regex("[0-9a-f]{64}")) && result.token.matches(Regex("[0-9a-f]{64}"))) { tr(R.string.msg_the_pairing_code_is_damaged_dc1231d) }
            java.util.UUID.fromString(result.peer)
            require(result.expires > System.currentTimeMillis() && result.expires <= System.currentTimeMillis() + 15*60*1000) { tr(R.string.msg_the_qr_code_expired_create_a_new_one_on_the_pc_ad88978) }
            return result
        }
    }
}

class LanSync(private val pairing: LanPairing): SyncRemote {
    fun report(kind: String) { request(JsonObject().apply { addProperty("op","complete");addProperty("kind",kind) }) }
    private fun request(body: JsonObject): JsonObject {
        require(pairing.expires > System.currentTimeMillis()) { tr(R.string.msg_the_wi_fi_session_expired_scan_a_new_qr_code_64f09ad) }
        val trust = object: X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, type: String) = throw java.security.cert.CertificateException(tr(R.string.msg_client_mode_is_not_supported_51cb3ad))
            override fun checkServerTrusted(chain: Array<X509Certificate>, type: String) {
                require(chain.isNotEmpty()) { tr(R.string.msg_pc_certificate_is_missing_387f0f2) }
                val hash = MessageDigest.getInstance("SHA-256").digest(chain[0].encoded).joinToString("") { "%02x".format(it.toInt() and 255) }
                if(hash != pairing.pin) throw java.security.cert.CertificateException(tr(R.string.msg_the_pc_certificate_does_not_match_the_qr_code_122c7f6))
                chain[0].checkValidity()
            }
        }
        val context = SSLContext.getInstance("TLS").apply { init(null,arrayOf(trust),SecureRandom()) }
        Socket().use { socket ->
            socket.connect(InetSocketAddress(pairing.host,pairing.port),15000);socket.soTimeout=45000
            (context.socketFactory.createSocket(socket,pairing.host,pairing.port,true) as SSLSocket).use { tls ->
                tls.soTimeout=45000;tls.startHandshake()
                body.addProperty("token",pairing.token)
                val bytes=body.toString().toByteArray(Charsets.UTF_8)
                require(bytes.size<=64*1024*1024) { tr(R.string.msg_the_packet_exceeds_64_mb_fd0c2a5) }
                val output=DataOutputStream(tls.outputStream);output.writeInt(bytes.size);output.write(bytes);output.flush()
                val input=DataInputStream(tls.inputStream);val length=input.readInt()
                require(length in 1..64*1024*1024) { tr(R.string.msg_invalid_response_from_the_pc_31248f8) }
                val response=ByteArray(length);input.readFully(response)
                val value=JsonParser.parseString(String(response,Charsets.UTF_8)).asJsonObject
                value.get("error")?.let { error(it.asString) }
                return value
            }
        }
    }
    override suspend fun pull(): RemoteCheckpoint {
        val value=request(JsonObject().apply { addProperty("op","pull") })
        return RemoteCheckpoint(value.get("revision").asLong,value.get("payload")?.takeUnless { it.isJsonNull }?.toString())
    }
    override suspend fun commit(change: String, expected: Long, payload: String): CommitReply {
        val value=request(JsonObject().apply { addProperty("op","commit");addProperty("change",change);addProperty("expected",expected);add("payload",JsonParser.parseString(payload)) })
        return CommitReply(value.get("accepted").asBoolean,value.get("revision").asLong)
    }
}
