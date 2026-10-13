package com.example.logbook.updates

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class UpdateState(val busy: Boolean = false, val visible: Boolean = false,
    val release: ReleaseManifest? = null, val message: String = "")

class AppUpdates(application: Application) : AndroidViewModel(application) {
    @Suppress("DEPRECATION")
    private val installed = application.packageManager.getPackageInfo(application.packageName, 0)
    val version: String = installed.versionName ?: "—"
    @Suppress("DEPRECATION")
    val versionCode: Long = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
    private val mutableState = MutableStateFlow(UpdateState())
    val state = mutableState.asStateFlow()
    private val prefs = application.getSharedPreferences("app_updates", 0)

    fun dismiss() { mutableState.value = mutableState.value.copy(visible = false) }
    fun check(automatic: Boolean = false) {
        if (mutableState.value.busy) return
        val today = LocalDate.now().toString()
        if (automatic && prefs.getString("last_attempt", "") == today) return
        if (automatic) prefs.edit().putString("last_attempt", today).apply()
        mutableState.value = UpdateState(busy = true, visible = !automatic, message = tr(R.string.msg_checking_for_updates_7aac9b2))
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.instanceFollowRedirects = false
                        connection.setRequestProperty("Cache-Control", "no-cache")
                        if (connection.responseCode == 404) error(tr(R.string.msg_android_update_information_has_not_been_publis_9f06c24))
                        check(connection.responseCode == 200) { tr(R.string.msg_the_update_server_is_temporarily_unavailable_6615cf2) }
                        val bytes = connection.inputStream.use { input ->
                            val buffer = ByteArray(65537)
                            var size = 0
                            while (size < buffer.size) {
                                val count = input.read(buffer, size, buffer.size - size)
                                if (count < 0) break
                                size += count
                            }
                            buffer.copyOf(size)
                        }
                        check(bytes.size <= 65536) { tr(R.string.msg_invalid_response_from_the_update_server_409fdd3) }
                        ReleaseManifest.parse(bytes.toString(Charsets.UTF_8))
                    } finally { connection.disconnect() }
                }
                val newer = release.versionCode > versionCode
                mutableState.value = UpdateState(visible = !automatic || newer, release = release,
                    message = if (newer) tr(R.string.msg_version_1_s_is_available_92efea1, release.version) else tr(R.string.msg_your_app_is_up_to_date_1647d70))
            } catch (_: kotlinx.coroutines.CancellationException) {
                throw kotlinx.coroutines.CancellationException()
            } catch (e: Exception) {
                mutableState.value = UpdateState(visible = !automatic,
                    message = if (e is java.io.IOException) tr(R.string.msg_could_not_check_for_updates_check_your_interne_a8061ec)
                    else e.message ?: tr(R.string.msg_could_not_read_update_information_edefb18))
            }
        }
    }
    companion object {
        const val MANIFEST_URL = "https://zjagjauwmpuzcwzidhkn.supabase.co/storage/v1/object/public/updates/android/version.json"
        const val WEBSITE = "https://logbook-landing-page-cbo2.bolt.host/"
    }
}
