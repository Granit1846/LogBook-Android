package com.example.logbook

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.localization.I18n
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class SettingsNavigationUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private var originalLanguage="ru"
    @Before fun rememberLanguage()=runBlocking {
        originalLanguage=AppDatabase.getInstance(compose.activity.applicationContext).appSettingsDao().get()?.language ?: "ru"
    }
    @After fun restoreLanguage() {
        runBlocking {
            val dao=AppDatabase.getInstance(compose.activity.applicationContext).appSettingsDao()
            dao.get()?.let { dao.update(it.copy(language=originalLanguage)) }
        }
    }
    @Test fun fullSettingsScreenRegistersPickersAcrossLanguageChangeAndRecreation() {
        openSettings()
        val first=if(I18n.language=="en") "en" else "ru"
        val second=if(first=="ru") "en" else "ru"
        switchLanguage(first,second)
        assertPickerSections(second)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        // Navigation may restore Settings or return to Profiles; both must remain usable.
        if(compose.onAllNodesWithText(languageLabel(second)).fetchSemanticsNodes().isEmpty()) openSettings()
        assertPickerSections(second)
        switchLanguage(second,first)
        assertPickerSections(first)
    }
    @Test fun localizedContextRetainsActivityAndResultRegistryOwner() {
        var context:Context?=null
        var owner:Any?=null
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContentForOwnerCheck { current, registry -> context=current;owner=registry }
        }
        compose.waitUntil(5000) { context != null }
        compose.runOnIdle {
            var current=context
            while(current is ContextWrapper && current !is ComponentActivity) current=current.baseContext
            assertSame(compose.activity,current)
            assertSame(compose.activity,owner)
        }
    }
    private fun openSettings() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithContentDescription("Настройки").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        val button = if (compose.onAllNodesWithContentDescription("Настройки").fetchSemanticsNodes().isNotEmpty()) "Настройки" else "Settings"
        compose.onNodeWithContentDescription(button).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText(languageLabel(I18n.language)).fetchSemanticsNodes().isNotEmpty() }
        assertPickerSections(I18n.language)
    }
    private fun languageLabel(language:String)=if(language=="en") "Language" else "Язык"
    private fun switchLanguage(from:String,to:String) {
        compose.onNodeWithText(languageLabel(from)).performScrollTo()
        compose.onNodeWithText(if(from=="ru") "Русский" else "English").performClick()
        compose.onNodeWithText(if(to=="ru") "Русский" else "English").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText(languageLabel(to)).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun assertPickerSections(language:String) {
        compose.onNodeWithText(I18n.tr(R.string.msg_import_from_the_pc_app_932fcb0)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(I18n.tr(R.string.msg_synchronization_7ceddda)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(languageLabel(language)).performScrollTo().assertIsDisplayed()
    }
}

private fun MainActivity.setContentForOwnerCheck(report:(Context,Any?)->Unit) {
    setContent {
        com.example.logbook.localization.AppLocaleHost("en") {
            val context=LocalContext.current
            val registry=LocalActivityResultRegistryOwner.current
            SideEffect { report(context,registry) }
        }
    }
}
