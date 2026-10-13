package com.example.logbook

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.model.*
import com.example.logbook.ui.tabs.resources.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceWarningsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun warningDetailsAndZeroRemainVisibleAndDisappearAfterCorrection() {
        val warnings = mutableStateOf(listOf(ResourceWarning(ResourceWarningLevel.EXPIRED, "ВР-80: срок истёк 1 дн. назад.")))
        compose.setContent {
            MaterialTheme {
                Column {
                    ResourceRemainingField("0:00", 90.dp, ResourceWarningLevel.CRITICAL)
                    ResourceWarningsButton(warnings.value)
                }
            }
        }
        compose.onNodeWithContentDescription("Критический остаток: 0:00").assertIsDisplayed()
        compose.onNodeWithText("Предупреждения: 1").performClick()
        compose.onNodeWithText("ВР-80: срок истёк 1 дн. назад.").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        compose.runOnIdle { warnings.value = emptyList() }
        compose.onNodeWithText("Предупреждения: 1").assertDoesNotExist()
    }
}
