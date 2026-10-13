package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.ui.tabs.resources.ReplaceAggregateDialog
import com.example.logbook.ui.tabs.resources.AggregatesOnBoardContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AggregateUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowPortraitKeepsStatusAndReplacementVisibleAndLocksSetup() {
        var replaced = ""
        val assignments = mapOf("Chast1" to "04008", "Vr80" to "vr-2",
            "Ta14" to "ta-2", "Vk250001" to "vk-3", "Vk250002" to "vk-4")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 640.dp).testTag("portrait")) {
                    AggregatesOnBoardContent(assignments, { replaced = it }, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("Настроить агрегаты").assertIsNotEnabled()
        val bounds = compose.onNodeWithTag("portrait").fetchSemanticsNode().boundsInRoot
        for (description in listOf("Агрегат назначен", "Заменить")) {
            val nodes = compose.onAllNodesWithContentDescription(description)
            assertEquals(if (description == "Заменить") 4 else 5, nodes.fetchSemanticsNodes().size)
            for (i in nodes.fetchSemanticsNodes().indices) {
                nodes[i].assertIsDisplayed()
                val icon = nodes[i].fetchSemanticsNode().boundsInRoot
                assertTrue("$description clipped: $icon outside $bounds",
                    icon.left >= bounds.left && icon.right <= bounds.right &&
                        icon.top >= bounds.top && icon.bottom <= bounds.bottom)
            }
        }
        compose.onAllNodesWithContentDescription("Заменить")[0].performClick()
        assertEquals("Vr80", replaced)
    }

    @Test fun incompleteAssignmentsAllowSetup() {
        compose.setContent {
            MaterialTheme {
                AggregatesOnBoardContent(mapOf("Chast1" to "04008", "Vr80" to " "), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Настроить агрегаты").assertIsEnabled()
    }

    @Test fun replacementPassesSelectedDateAndRejectsInvalidDate() {
        var selected: java.time.LocalDate? = null
        compose.setContent {
            MaterialTheme {
                ReplaceAggregateDialog(type = "Vr80", currentNumber = "ВР-1",
                    onConfirm = { _, _, date -> selected = date }, onDismiss = {})
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("ВР-2")
        compose.onNodeWithText("Дата установки").performScrollTo().performTextReplacement("310226")
        compose.onNodeWithText("Заменить").assertIsNotEnabled()
        compose.onNodeWithText("Дата установки").performTextReplacement("020926")
        compose.onNodeWithText("Заменить").performScrollTo().performClick()
        assertEquals(java.time.LocalDate.of(2026, 9, 2), selected)
    }

    @Test fun replacementAcceptsAlphanumericSerialAndRequiresNewNumber() {
        var number = ""
        compose.setContent {
            MaterialTheme {
                ReplaceAggregateDialog(type = "Vk250001", currentNumber = "Д-1",
                    onConfirm = { serial, _, _ -> number = serial }, onDismiss = {})
            }
        }
        compose.onNodeWithText("Заменить").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Д-123")
        compose.onNodeWithText("Заменить").performClick()
        assertEquals("Д-123", number)
    }
}
