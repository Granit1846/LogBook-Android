package com.example.logbook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun backupIsAccessibleFromProfileListWithoutOpeningProfile() {
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Сохранить резервную копию").assertDoesNotExist()
        compose.onNodeWithText("Резервная копия").performScrollTo().performClick()
        compose.onNodeWithText("Сохранить резервную копию").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Восстановить из файла").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Резервная копия").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Сохранить резервную копию").assertDoesNotExist()
    }
}
