package com.example.logbook

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.logbook.data.local.entity.AppSettingsEntity
import com.example.logbook.localization.*
import com.example.logbook.ui.settings.SettingsContent
import com.example.logbook.ui.theme.LogBookTheme
import org.junit.*
import java.io.File

class LocalizationUiTest {
    @get:Rule val compose=createComposeRule()
    @Before fun setup() { I18n.initialize(ApplicationProvider.getApplicationContext<Context>());I18n.changeLanguage("ru") }
    @After fun cleanup() { compose.runOnIdle { I18n.changeLanguage("ru") } }
    @Test fun settingsSwitchLanguageWithoutReopeningAndKeepsSelectedTheme() {
        compose.setContent {
            var settings by remember { mutableStateOf(AppSettingsEntity(theme="Nexus")) }
            AppLocaleHost(settings.language) { LogBookTheme(themeMode=settings.theme) { androidx.compose.material3.Surface {
                SettingsContent(settings,{}, {},{ settings=settings.copy(language=it) },{},{},{},{},{})
            } } }
        }
        compose.onNodeWithText("Язык").assertIsDisplayed()
        compose.onNodeWithText("Русский").performClick()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Language").assertIsDisplayed()
        compose.onNodeWithText("Theme").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(32);compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(150) // Let SurfaceFlinger present the composed frame before the system screenshot.
        saveScreen("localization-settings-en.png")
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Русский").performClick()
        compose.onNodeWithText("Язык").assertIsDisplayed()
        compose.onNodeWithText("Тема").assertIsDisplayed()
    }
    private fun saveScreen(name:String) {
        val i=InstrumentationRegistry.getInstrumentation();val b=i.uiAutomation.takeScreenshot()
        File(i.targetContext.getExternalFilesDir(null),name).outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
}
