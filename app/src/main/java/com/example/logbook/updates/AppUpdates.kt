package com.example.logbook.updates

import com.example.logbook.R
import com.example.logbook.BuildConfig
import com.example.logbook.localization.I18n.tr

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class UpdateState(
    val busy: Boolean = false,
    val visible: Boolean = false,
    val downloadDialogVisible: Boolean = false,
    val release: ReleaseManifest? = null,
    val message: String = "",
    val isNewer: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val downloadDetail: String = "",
    val downloadedFile: File? = null,
    val downloadError: String? = null
)

class AppUpdates(application: Application) : AndroidViewModel(application) {
    @Suppress("DEPRECATION")
    private val installed = application.packageManager.getPackageInfo(application.packageName, 0)
    val version: String = installed.versionName ?: "—"
    @Suppress("DEPRECATION")
    val versionCode: Long = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
    private val mutableState = MutableStateFlow(UpdateState())
    val state = mutableState.asStateFlow()
    private val prefs = application.getSharedPreferences("app_updates", 0)
    private var downloadJob: Job? = null

    fun dismiss() {
        if (!mutableState.value.isDownloading) {
            mutableState.value = mutableState.value.copy(visible = false)
        }
    }

    fun dismissDownloadDialog() {
        if (mutableState.value.isDownloading) {
            cancelDownload()
        }
        mutableState.value = mutableState.value.copy(downloadDialogVisible = false)
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        mutableState.value = mutableState.value.copy(
            isDownloading = false,
            downloadProgress = null,
            downloadDetail = "",
            downloadDialogVisible = false
        )
    }

    fun check(automatic: Boolean = false) {
        if (!BuildConfig.IN_APP_UPDATES) return
        if (mutableState.value.busy) return
        val today = LocalDate.now().toString()
        if (automatic && prefs.getString("last_attempt", "") == today) return
        if (automatic) prefs.edit().putString("last_attempt", today).apply()
        mutableState.value = UpdateState(busy = true, visible = !automatic, message = tr(R.string.msg_checking_for_updates_7aac9b2))
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    // 1. Попытка запроса к GitHub Releases
                    var ghRelease: ReleaseManifest? = null
                    try {
                        val ghConn = URL(GITHUB_RELEASES_URL).openConnection() as HttpURLConnection
                        ghConn.connectTimeout = 8000
                        ghConn.readTimeout = 8000
                        ghConn.setRequestProperty("User-Agent", "LogBook-Android")
                        ghConn.setRequestProperty("Accept", "application/vnd.github+json")
                        ghConn.setRequestProperty("Cache-Control", "no-cache")
                        if (ghConn.responseCode == 200) {
                            val ghJson = ghConn.inputStream.bufferedReader().use { it.readText() }
                            ghRelease = ReleaseManifest.parseGitHub(ghJson)
                        }
                        ghConn.disconnect()
                    } catch (_: Exception) {}

                    if (ghRelease != null && ghRelease.url.isNotBlank()) {
                        ghRelease
                    } else {
                        // 2. Запасной вариант — Supabase MANIFEST_URL
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
                }

                val newer = ReleaseManifest.isVersionNewer(release.version, version) ||
                        (release.versionCode > 0 && release.versionCode > versionCode)

                mutableState.value = UpdateState(
                    visible = !automatic || newer,
                    release = release,
                    isNewer = newer,
                    message = if (newer) tr(R.string.msg_version_1_s_is_available_92efea1, release.version) else tr(R.string.msg_your_app_is_up_to_date_1647d70)
                )
            } catch (_: kotlinx.coroutines.CancellationException) {
                throw kotlinx.coroutines.CancellationException()
            } catch (e: Exception) {
                mutableState.value = UpdateState(
                    visible = !automatic,
                    message = if (e is java.io.IOException) tr(R.string.msg_could_not_check_for_updates_check_your_interne_a8061ec)
                    else e.message ?: tr(R.string.msg_could_not_read_update_information_edefb18)
                )
            }
        }
    }

    fun startDownload(context: Context? = null) {
        if (!BuildConfig.IN_APP_UPDATES) return
        val release = mutableState.value.release ?: return
        if (release.url.isBlank()) return
        if (mutableState.value.isDownloading) return

        val app = getApplication<Application>()
        val targetDir = app.externalCacheDir ?: app.cacheDir
        val apkFile = File(targetDir, "LogBook-update.apk")

        mutableState.value = mutableState.value.copy(
            visible = false,
            downloadDialogVisible = true,
            isDownloading = true,
            downloadProgress = 0f,
            downloadDetail = tr(R.string.msg_downloading_update_prefix),
            downloadedFile = null,
            downloadError = null
        )

        downloadJob = viewModelScope.launch {
            try {
                val downloadedFile = withContext(Dispatchers.IO) {
                    var currentUrl = release.url
                    var connection: HttpURLConnection
                    var redirectCount = 0
                    while (true) {
                        val u = URL(currentUrl)
                        connection = u.openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty("User-Agent", "LogBook-Android")
                        val code = connection.responseCode
                        if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                            val loc = connection.getHeaderField("Location")
                            connection.disconnect()
                            if (loc != null && redirectCount < 5) {
                                currentUrl = loc
                                redirectCount++
                                continue
                            }
                        }
                        break
                    }

                    if (connection.responseCode !in 200..299) throw java.io.IOException(tr(R.string.msg_update_download_failed_http, connection.responseCode))
                    val serverBytes = connection.contentLengthLong
                    val totalBytes = serverBytes.takeIf { it > 0 } ?: release.apkSize
                    if (apkFile.exists()) apkFile.delete()

                    val received = connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(32768)
                            var count: Int
                            var downloaded = 0L
                            var lastReport = System.currentTimeMillis()

                            while (input.read(buffer).also { count = it } != -1) {
                                output.write(buffer, 0, count)
                                downloaded += count

                                val now = System.currentTimeMillis()
                                if (now - lastReport > 100 || (totalBytes > 0 && downloaded == totalBytes)) {
                                    lastReport = now
                                    val progress = if (totalBytes > 0) (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                                    val mbDown = downloaded / (1024f * 1024f)
                                    val mbTotal = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
                                    val percent = (progress * 100).toInt()
                                    val mbDownStr = String.format(java.util.Locale.US, "%.1f", mbDown)
                                    val mbTotalStr = String.format(java.util.Locale.US, "%.1f", mbTotal)
                                    val detail = if (totalBytes > 0) {
                                        "$percent% ($mbDownStr / $mbTotalStr МБ)"
                                    } else {
                                        "$mbDownStr МБ"
                                    }
                                    mutableState.value = mutableState.value.copy(
                                        downloadProgress = progress,
                                        downloadDetail = detail
                                    )
                                }
                            }
                            downloaded
                        }
                    }
                    connection.disconnect()
                    // A dropped connection ends the stream early; installing that file only shows "package is invalid".
                    if (serverBytes > 0 && received != serverBytes) {
                        apkFile.delete()
                        throw java.io.IOException(tr(R.string.msg_update_download_incomplete))
                    }
                    apkFile
                }

                mutableState.value = mutableState.value.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    downloadedFile = downloadedFile,
                    downloadDetail = tr(R.string.msg_update_ready_to_install)
                )
            } catch (_: kotlinx.coroutines.CancellationException) {
                mutableState.value = mutableState.value.copy(
                    isDownloading = false,
                    downloadProgress = null,
                    downloadDetail = ""
                )
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(
                    isDownloading = false,
                    downloadProgress = null,
                    downloadError = e.message ?: tr(R.string.msg_update_download_incomplete)
                )
            }
        }
    }

    fun installDownloadedApk(context: Context) {
        if (!BuildConfig.IN_APP_UPDATES) return
        val file = mutableState.value.downloadedFile ?: return
        installApk(context, file)
    }

    fun downloadAndInstall(context: Context) {
        startDownload(context)
    }

    fun installApk(context: Context, apkFile: File) {
        if (!BuildConfig.IN_APP_UPDATES) return
        try {
            val app = getApplication<Application>()
            val activity = context.findActivity()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = if (activity != null) activity.packageManager.canRequestPackageInstalls()
                                 else app.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (activity != null) activity.startActivity(intent) else app.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (activity != null) activity.startActivity(intent) else app.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (activity != null) activity.startActivity(intent) else app.startActivity(intent)
        } catch (e: Exception) {
            mutableState.value = mutableState.value.copy(
                downloadError = e.message ?: "Не удалось запустить установку"
            )
        }
    }

    companion object {
        const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Granit1846/LogBook-Android/releases/latest"
        const val MANIFEST_URL = "https://zjagjauwmpuzcwzidhkn.supabase.co/storage/v1/object/public/updates/android/version.json"
        const val WEBSITE = "https://logbook-landing-page-cbo2.bolt.host/"
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
