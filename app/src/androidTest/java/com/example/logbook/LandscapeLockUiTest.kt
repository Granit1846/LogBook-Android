package com.example.logbook

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.logbook.localization.AppLocaleHost
import com.example.logbook.ui.util.LockLandscape
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LandscapeLockUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun localizedScreenLocksLandscapeAndRestoresPreviousOrientation() {
        val visible = mutableStateOf(true)
        var previous = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        compose.activityRule.scenario.onActivity { activity ->
            previous = activity.requestedOrientation
            activity.setContent {
                AppLocaleHost(null) {
                    if (visible.value) LockLandscape()
                }
            }
        }

        compose.runOnIdle {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, compose.activity.requestedOrientation)
            visible.value = false
        }
        compose.runOnIdle { assertEquals(previous, compose.activity.requestedOrientation) }
    }
}
