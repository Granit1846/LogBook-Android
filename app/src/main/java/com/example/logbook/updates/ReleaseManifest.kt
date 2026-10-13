package com.example.logbook.updates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import com.google.gson.JsonParser
import java.net.URI

data class ReleaseManifest(val version: String, val versionCode: Long, val url: String, val notes: String,
    val notesEn: String = "") {
    val localizedNotes: String get() = if (com.example.logbook.localization.I18n.language == "en" && notesEn.isNotBlank()) notesEn else notes
    companion object {
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
            require(appId == null || appId == "com.example.logbook") { tr(R.string.msg_build_for_another_application_d13ab83) }
            return ReleaseManifest(version, versionCode, url, data.get("notes")?.asString.orEmpty(), data.get("notesEn")?.asString.orEmpty())
        }
    }
}
