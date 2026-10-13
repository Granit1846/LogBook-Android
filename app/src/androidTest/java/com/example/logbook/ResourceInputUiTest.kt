package com.example.logbook

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.*
import com.example.logbook.ui.tabs.resources.ProfileEntryWarnings
import com.example.logbook.ui.tabs.resources.aggregates.AggregateTextField
import com.example.logbook.ui.tabs.resources.oils.OilTextField
import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceInputUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun entryDialogCombinesBothTabsAndReopensForAnotherProfile() {
        val profile = mutableStateOf(ProfileEntity(id = 100, name = "Проверка", fuselageSne = 100,
            resourcesJson = Gson().toJson(listOf(ResourceItem(name = "Агрегат", extendedTo1 = 90))),
            oilsJson = Gson().toJson(listOf(OilItem(name = "Масло", coefficient = 90)))))
        compose.setContent { MaterialTheme { ProfileEntryWarnings(profile.value) } }
        compose.onNodeWithText("Предупреждения: Проверка").assertIsDisplayed()
        compose.onNodeWithText("Агрегаты —", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Масла —", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Предупреждения: Проверка").assertDoesNotExist()
        compose.runOnIdle { profile.value = profile.value.copy(id = 101, name = "Другой") }
        compose.onNodeWithText("Предупреждения: Другой").assertIsDisplayed()
    }

    @Test fun focusedResourceFieldsScrollAboveKeyboardInBothTabs() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().imePadding()) {
                    Box(Modifier.weight(1f).fillMaxWidth().testTag("viewport")) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(700.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                Spacer(Modifier.width(800.dp))
                                AggregateTextField("агрегат", {}, 140.dp)
                                OilTextField("масло", {}, 140.dp)
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
        for (label in listOf("агрегат", "масло")) {
            val field = compose.onNodeWithText(label)
            field.performScrollTo().performClick()
            compose.waitUntil(5000) {
                val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
                val bounds = field.fetchSemanticsNode().boundsInRoot
                bounds.top >= viewport.top && bounds.bottom <= viewport.bottom &&
                    bounds.left >= viewport.left && bounds.right <= viewport.right
            }
            assertTrue(field.fetchSemanticsNode().boundsInRoot.width > 0)
        }
    }
}
