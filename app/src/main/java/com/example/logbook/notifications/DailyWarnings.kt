package com.example.logbook.notifications

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.logbook.MainActivity
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.model.ResourceWarnings
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/** One successful notification per local calendar day, across every entry point. */
class DailyWarnings(
    private val database: AppDatabase,
    private val preferences: SharedPreferences,
    private val enabled: () -> Boolean,
    private val deliver: (List<String>) -> Unit
) {
    suspend fun check(today: LocalDate = LocalDate.now()): Boolean = mutex.withLock {
        if (preferences.getString("last_day", "") == today.toString() || !enabled()) return@withLock false
        val lines = database.profileDao().getAll().flatMap { profile ->
            ResourceWarnings.forProfile(profile, today).map { "${profile.name}: ${it.text}" }
        }
        if (lines.isEmpty()) return@withLock false
        deliver(lines)
        kotlin.check(preferences.edit().putString("last_day", today.toString()).commit())
        true
    }

    companion object {
        private val mutex = Mutex()
        const val CHANNEL = "resource_warnings"
        const val NOTIFICATION_ID = 701
        fun create(context: Context): DailyWarnings {
            com.example.logbook.localization.I18n.initialize(context)
            val app = context.applicationContext
            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, tr(R.string.msg_resources_and_oils_fe8bb5b), NotificationManager.IMPORTANCE_DEFAULT))
            return DailyWarnings(AppDatabase.getInstance(app), app.getSharedPreferences("daily_warnings", Context.MODE_PRIVATE),
                enabled = {
                    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                        NotificationManagerCompat.from(app).areNotificationsEnabled() &&
                        manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
                }, deliver = { lines -> postNotification(app, lines) })
        }

        internal fun postNotification(app: Context, lines: List<String>, id: Int = NOTIFICATION_ID) {
            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, tr(R.string.msg_resources_and_oils_fe8bb5b), NotificationManager.IMPORTANCE_DEFAULT))
            val launch = PendingIntent.getActivity(app, id, Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_resource_warning)
                .setContentTitle(tr(R.string.msg_today_logbook_warnings_f68db41))
                .setContentText(tr(R.string.msg_warnings_1_s_open_a_profile_to_view_them_d9d7f85, lines.size))
                .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
                .setContentIntent(launch).setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
            manager.notify(id, notification)
        }

        fun schedule(context: Context) {
            val next = LocalDate.now().plusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = PendingIntent.getBroadcast(context, 701, Intent(context, DailyWarningsReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
        }
    }
}

class DailyWarningsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyWarnings.schedule(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { DailyWarnings.create(context).check() }
            catch (error: Exception) { android.util.Log.e("LogBook", "Daily warning check failed", error) }
            finally { pending.finish() }
        }
    }
}
