package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.ui.util.ScrollableDatePickerDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calendarActionsRemainVisibleAndPreserveLocalDate() {
        var selected: LocalDate? = null
        val date = LocalDate.of(2026, 9, 2)
        compose.setContent {
            MaterialTheme { ScrollableDatePickerDialog(date, { selected = it }, {}) }
        }
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        compose.onNodeWithText("Выбрать").assertIsDisplayed().performClick()
        assertEquals(date, selected)
    }
}
