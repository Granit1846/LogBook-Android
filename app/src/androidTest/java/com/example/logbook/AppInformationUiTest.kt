package com.example.logbook

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppInformationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun informationAndManualCheckRemainUsableInBothOrientations() {
        val packageInfo=compose.activity.packageManager.getPackageInfo(compose.activity.packageName,0)
        val version=packageInfo.versionName
        val code=packageInfo.longVersionCode
        compose.onNodeWithContentDescription("Информация").performClick()
        compose.onNodeWithText("О приложении LogBook").assertIsDisplayed()
        compose.onNodeWithText("Android · версия $version · сборка $code").assertIsDisplayed()
        saveScreen("information-portrait.png")
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        compose.onNodeWithText("Закрыть").assertIsDisplayed()
        compose.onNodeWithText("Проверить обновления").assertIsDisplayed().performClick()
        compose.waitUntil(20000) { compose.onAllNodesWithText("Проверяем обновления…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Установлена версия $version ($code)").assertIsDisplayed()
        compose.onNodeWithText("У вас актуальная версия приложения.").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").assertIsDisplayed()
        saveScreen("updates-landscape.png")
        compose.onNodeWithText("Закрыть").performClick()
        compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
    private fun saveScreen(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

