package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.DesktopProfilePreview
import com.example.logbook.ui.settings.DesktopImportPreview
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesktopImportUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun previewExcludesExistingProfilesAndKeepsActionsVisible() {
        var selected: Set<String>?=null
        compose.setContent { MaterialTheme { DesktopImportPreview(listOf(
            DesktopProfilePreview("new","Новый","123",20,100,listOf("Наработка без пересчёта"),false),
            DesktopProfilePreview("old","Существующий","456",40,200,emptyList(),true)),22,false,{}, {selected=it}) } }
        compose.onNodeWithText("Импортировать").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(setOf("new"),selected) }
        compose.onNodeWithText("Уже существует — импорт пропущен").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Архивов агрегатов в файле:",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Отмена").assertIsDisplayed()
    }
}
