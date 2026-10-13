package com.example.logbook.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.logbook.MainActivity
import com.example.logbook.R
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.model.MaintenanceJournal
import com.example.logbook.localization.I18n
import com.example.logbook.localization.I18n.tr

object DefectNotifications {
    private const val CHANNEL = "open_defect_tasks"
    private const val ID = 702

    suspend fun refresh(
        context: Context,
        db: AppDatabase = AppDatabase.getInstance(context),
        forceDailyCheck: Boolean = false
    ) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("defect_notifications", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()

        if (forceDailyCheck && prefs.getString("last_defect_day", "") == today) {
            return
        }

        I18n.initialize(app)
        val manager = app.getSystemService(NotificationManager::class.java)
        val lines = db.profileDao().getAll().flatMap { profile ->
            MaintenanceJournal.openTasks(profile).map { task -> "${profile.name}: ${task.description}" }
        }
        if (lines.isEmpty() || Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            manager.cancel(ID)
            return
        }
        manager.createNotificationChannel(NotificationChannel(CHANNEL, tr(R.string.journal_defect_channel), NotificationManager.IMPORTANCE_DEFAULT))
        if (manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return
        val intent = PendingIntent.getActivity(app, ID, Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(ID, NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_resource_warning)
            .setContentTitle(tr(R.string.journal_open_defects_count, lines.size.toString()))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build())

        if (forceDailyCheck) {
            prefs.edit().putString("last_defect_day", today).apply()
        }
    }
}
