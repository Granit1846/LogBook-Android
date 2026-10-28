package com.example.logbook.updates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import com.google.gson.JsonParser
import java.net.URI

data class ReleaseManifest(
    val version: String,
    val versionCode: Long,
    val url: String,
    val notes: String,
    val notesEn: String = "",
    val apkSize: Long = 0L
) {
    val localizedNotes: String get() = if (com.example.logbook.localization.I18n.language == "en" && notesEn.isNotBlank()) notesEn else notes

    companion object {
        fun isVersionNewer(remote: String, current: String): Boolean {
            val rParts = remote.split('.').mapNotNull { it.toIntOrNull() }
            val cParts = current.split('.').mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        fun parseGitHub(json: String): ReleaseManifest? {
            return try {
                val data = JsonParser.parseString(json).asJsonObject
                val tag = data.get("tag_name")?.asString ?: return null
                val version = tag.trimStart('v', 'V')
                val notes = data.get("body")?.asString.orEmpty()
                var apkUrl = ""
                var apkSize = 0L
                val assets = data.getAsJsonArray("assets")
                if (assets != null) {
                    for (element in assets) {
                        val asset = element.asJsonObject
                        val name = asset.get("name")?.asString ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.get("browser_download_url")?.asString ?: ""
                            apkSize = asset.get("size")?.asLong ?: 0L
                            break
                        }
                    }
                }
                ReleaseManifest(version, 0L, apkUrl, notes, "", apkSize)
            } catch (_: Exception) {
                null
            }
        }

        fun parse(json: String): ReleaseManifest {
            val data = JsonParser.parseString(json).asJsonObject
            val version = data.get("version").asString
            require(version.matches(Regex("\\d+(?:\\.\\d+){0,3}"))) { tr(R.string.msg_invalid_version_4dc841c) }
            val code = data.get("versionCode").asString
            require(code.matches(Regex("[1-9]\\d*"))) { tr(R.string.msg_invalid_build_number_c310a9f) }
            val versionCode = code.toLong()
            val url = data.get("url")?.asString.orEmpty()
            if (url.isNotEmpty()) {
                val uri = URI(url)
                require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { tr(R.string.msg_invalid_link_102b915) }
            }
            val appId = data.get("applicationId")?.asString
            require(appId == null || appId == "com.skeorz.lab.logbook") { tr(R.string.msg_build_for_another_application_d13ab83) }
            return ReleaseManifest(version, versionCode, url, data.get("notes")?.asString.orEmpty(), data.get("notesEn")?.asString.orEmpty())
        }
    }
}
