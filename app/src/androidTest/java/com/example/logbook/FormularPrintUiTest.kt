package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.ui.main.MainScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormularPrintUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun toolbarOpensScrollablePrintOptionsWithVisibleActions() {
        compose.setContent { MaterialTheme {
            MainScreenContent(ProfileEntity(id = 998, name = "Проверка"), 998, null, onBack = {})
        } }
        compose.onNodeWithContentDescription("Печать / PDF").performClick()
        compose.onNodeWithText("Сохранить PDF").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Печать").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Бумага A3 (по умолчанию A4)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Начальные данные").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Сохранить PDF").assertDoesNotExist()
    }
}
