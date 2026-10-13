package com.example.logbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logbook.ui.navigation.AppNavigation
import com.example.logbook.ui.settings.SettingsViewModel
import com.example.logbook.ui.settings.SettingsViewModelFactory
import com.example.logbook.ui.theme.LogBookTheme
import com.example.logbook.data.local.AppDatabase

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.logbook.notifications.DailyWarnings
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var dailyJob: Job? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) checkDailyWarnings()
    }

    private fun checkDailyWarnings() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { DailyWarnings.create(applicationContext).check() }
        }
    }

    override fun onStart() {
        super.onStart()
        DailyWarnings.schedule(applicationContext)
        dailyJob?.cancel()
        dailyJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                runCatching { DailyWarnings.create(applicationContext).check() }
                delay(60_000)
            }
        }
    }

    override fun onStop() {
        dailyJob?.cancel()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.logbook.localization.I18n.initialize(applicationContext)
        enableEdgeToEdge()
        val permissionPrefs = getSharedPreferences("notification_permission", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !permissionPrefs.getBoolean("asked", false)) {
            permissionPrefs.edit().putBoolean("asked", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val database = AppDatabase.getInstance(applicationContext)
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(database)
            )
            val settings by settingsViewModel.settings.collectAsState()

            val themeMode = settings?.theme ?: "System"

            com.example.logbook.localization.AppLocaleHost(settings?.language) {
            LogBookTheme(themeMode = themeMode) {
                com.example.logbook.ui.util.AppInformationHost(settings?.autoCheckUpdates) { AppNavigation() }
            }
            }
        }
    }
}