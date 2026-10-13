package com.example.logbook

import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.DefectTask
import com.example.logbook.data.model.DueState
import com.example.logbook.data.model.MaintenanceJournal as J
import com.example.logbook.data.model.MaintenanceWork
import com.example.logbook.ui.tabs.maintenance.normalizeCounterInput
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MaintenanceJournalTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test fun fiftyHourCycleRestartsAfterHundredHourWork() {
        val hundred = MaintenanceWork(kind = J.HOURS_100, date = "2026-10-01", counterMinutes = 1000 * 60, description = "100 ч")
        fun due(works: List<MaintenanceWork>, hours: Int) = J.due(works, hours * 60, today).first { it.kind == J.HOURS_50 }
        assertEquals(DueState.LATER, due(listOf(hundred), 1044).state)
        assertEquals(DueState.DUE, due(listOf(hundred), 1045).state)
        assertEquals("1050:00", due(listOf(hundred), 1050).nominal)
        assertEquals(DueState.DUE, due(listOf(hundred), 1060).state)
        assertEquals(DueState.OVERDUE, due(listOf(hundred), 1061).state)
        val fifty = MaintenanceWork(kind = J.HOURS_50, date = "2026-10-05", counterMinutes = 1050 * 60, description = "50 ч")
        assertEquals("1100:00", due(listOf(hundred, fifty), 1070).nominal)
        val nextHundred = hundred.copy(id = java.util.UUID.randomUUID().toString(), counterMinutes = 1100 * 60)
        assertEquals("1150:00", due(listOf(hundred, fifty, nextHundred), 1110).nominal)
    }

    @Test fun independentHundredAndThreeHundredHourWindows() {
        val works = listOf(
            MaintenanceWork(kind = J.HOURS_100, date = "2026-10-01", counterMinutes = 1000 * 60, description = "100 ч"),
            MaintenanceWork(kind = J.HOURS_300, date = "2026-10-01", counterMinutes = 900 * 60, description = "300 ч")
        )
        val due = J.due(works, 1100 * 60, today)
        assertEquals("1100:00", due.first { it.kind == J.HOURS_100 }.nominal)
        assertEquals("1200:00", due.first { it.kind == J.HOURS_300 }.nominal)
    }

    @Test fun calendarWindowsUseActualDaysAndMonths() {
        val works = listOf(
            MaintenanceWork(kind = J.PERIODIC, date = "2026-09-21", counterMinutes = 0, description = "Осмотр"),
            MaintenanceWork(kind = J.REG_12, date = "2025-10-11", counterMinutes = 0, description = "12 мес"),
            MaintenanceWork(kind = J.REG_24, date = "2024-10-11", counterMinutes = 0, description = "24 мес")
        )
        val due = J.due(works, 0, today)
        assertEquals("2026-10-06", due.first { it.kind == J.PERIODIC }.lower)
        assertEquals("2026-10-16", due.first { it.kind == J.PERIODIC }.upper)
        assertEquals("2026-11-11", due.first { it.kind == J.REG_12 }.upper)
        assertEquals("2026-12-11", due.first { it.kind == J.REG_24 }.upper)
        assertTrue(due.all { it.state == DueState.DUE || it.state == DueState.NEEDS_BASELINE })
    }

    @Test fun completedTaskLeavesActiveListAfterTenMinutesButRemainsStored() {
        val task = DefectTask(description = "Течь", createdAt = 1000L, completedAt = 2000L)
        val profile = ProfileEntity(defectTasksJson = J.encodeTasks(listOf(task)))
        assertEquals(1, J.visibleTasks(profile, 601_999L).size)
        assertTrue(J.visibleTasks(profile, 602_000L).isEmpty())
        assertEquals(1, J.tasks(profile).size)
        assertTrue(J.openTasks(profile).isEmpty())
    }

    @Test fun remainingWindowUsesEqualTrafficLightThirds() {
        val work = MaintenanceWork(kind = J.HOURS_50, date = "2026-10-01", counterMinutes = 1000 * 60, description = "50 ч")
        fun remaining(hours: Int) = J.remaining(J.due(listOf(work), hours * 60, today).first { it.kind == J.HOURS_50 }, hours * 60, today)!!
        assertEquals(0, remaining(1019).third)
        assertEquals(1, remaining(1020).third)
        assertEquals(2, remaining(1040).third)
        assertEquals(45L, remaining(1050).lower)
        assertEquals(50L, remaining(1050).nominal)
        assertEquals(60L, remaining(1050).upper)
        assertEquals(50 * 60L, remaining(1050).current)
        assertEquals(61 * 60L, remaining(1061).current)
        assertEquals(1L, J.roundHours(89))
        assertEquals(2L, J.roundHours(90))
    }

    @Test fun punctuationInCounterInputBecomesColon() {
        assertEquals("12:30", normalizeCounterInput("12,30"))
        assertEquals("12:30", normalizeCounterInput("12..30"))
        assertEquals("12:30", normalizeCounterInput("12-30"))
    }
}
