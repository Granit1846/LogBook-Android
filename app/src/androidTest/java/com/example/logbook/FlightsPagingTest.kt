package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.logbook.data.local.entity.DayEntity
import com.example.logbook.data.local.entity.LaunchEntity
import com.example.logbook.data.local.entity.SnkEntity
import com.example.logbook.ui.tabs.flights.FlightsTabContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FlightsPagingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun largeFilledDaysComposeOnlyVisibleLaunchesAndPageBothWays() {
        val dayIndex = mutableStateOf(0)
        val dates = listOf("10.10.2026", "11.10.2026")
        val records = List(2) { index ->
            List(100) { order ->
                LaunchEntity(id = index * 100L + order + 1, dayId = index + 1L,
                    order = order, vsu = 5, zemlya = 12, vozduh = 46)
            }
        }
        compose.setContent {
            val index = dayIndex.value
            MaterialTheme {
                FlightsTabContent(
                    profileId = 1, date = dates[index], launches = records[index],
                    currentDay = DayEntity(id = index + 1L, profileId = 1,
                        date = dates[index], crew = "Экипаж $index"),
                    snk = SnkEntity(dayId = index + 1L, aLeft = 1.5),
                    showToolbar = true,
                    onShiftDay = { dayIndex.value = (index + it).coerceIn(0, 1) },
                    onUpdateCrew = {}, onAddLaunch = {}, onUpdateLaunch = {},
                    onDeleteLaunch = {}, onUpdateSnk = {}
                )
            }
        }
        compose.onNodeWithText(dates[0]).assertIsDisplayed()
        val visibleCards = compose.onAllNodesWithContentDescription("Удалить запуск")
            .fetchSemanticsNodes().size
        assertTrue("Only viewport cards should be composed: $visibleCards", visibleCards in 1..20)
        compose.onNodeWithContentDescription("Следующий день").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(dates[1]).assertIsDisplayed()
        compose.onNodeWithText("Экипаж 1").assertExists()
        compose.onNodeWithContentDescription("Предыдущий день").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(dates[0]).assertIsDisplayed()
        compose.onNodeWithText("Экипаж 0").assertExists()
        compose.onAllNodesWithContentDescription("Удалить запуск").fetchSemanticsNodes()
            .also { assertTrue(it.size < 100) }
    }
}
