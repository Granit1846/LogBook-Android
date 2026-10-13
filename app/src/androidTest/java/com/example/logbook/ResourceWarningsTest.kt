package com.example.logbook

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceWarningsTest {
    private val today = LocalDate.of(2026, 10, 10)

    @Test fun aggregateMinuteBoundaries() {
        val expected = listOf(6001 to ResourceWarningLevel.NONE, 6000 to ResourceWarningLevel.WARNING,
            3001 to ResourceWarningLevel.WARNING, 3000 to ResourceWarningLevel.URGENT,
            1501 to ResourceWarningLevel.URGENT, 1500 to ResourceWarningLevel.CRITICAL,
            0 to ResourceWarningLevel.CRITICAL, -1 to ResourceWarningLevel.EXPIRED)
        expected.forEach { (remaining, level) ->
            assertEquals(level, ResourceWarnings.aggregateHours(ResourceItem(extendedTo1 = 10000, sne = 10000 - remaining)))
        }
    }

    @Test fun oilMinuteBoundariesAndZeroFillSne() {
        listOf(3001 to ResourceWarningLevel.NONE, 3000 to ResourceWarningLevel.WARNING,
            1201 to ResourceWarningLevel.WARNING, 1200 to ResourceWarningLevel.CRITICAL,
            0 to ResourceWarningLevel.CRITICAL, -1 to ResourceWarningLevel.EXPIRED).forEach { (remaining, level) ->
            assertEquals(level, ResourceWarnings.oilHours(OilItem(byHours = 0, coefficient = 4000), 4000 - remaining))
        }
    }

    @Test fun calendarUsesFullMonthsAndActualExpiryDay() {
        assertEquals(ResourceWarningLevel.NONE, ResourceWarnings.calendar(today.plusMonths(4), today))
        assertEquals(ResourceWarningLevel.WARNING, ResourceWarnings.calendar(today.plusMonths(3), today))
        assertEquals(ResourceWarningLevel.URGENT, ResourceWarnings.calendar(today.plusMonths(2), today))
        assertEquals(ResourceWarningLevel.CRITICAL, ResourceWarnings.calendar(today.plusMonths(1), today))
        assertEquals(ResourceWarningLevel.CRITICAL, ResourceWarnings.calendar(today, today))
        assertEquals(ResourceWarningLevel.EXPIRED, ResourceWarnings.calendar(today.minusDays(1), today))
        assertEquals(ResourceWarningLevel.WARNING, ResourceWarnings.calendar(today.plusMonths(2), today, true))
        assertEquals(ResourceWarningLevel.NONE, ResourceWarnings.calendar(today.plusMonths(3), today, true))
    }

    @Test fun missingAndInvalidDataNeverBecomeExpired() {
        assertTrue(ResourceWarnings.aggregates(listOf(ResourceItem(name = "Пустой", sne = 1000)), today).isEmpty())
        assertTrue(ResourceWarnings.oils(listOf(OilItem(name = "Пустой", byHours = 1000)), 9999, today).isEmpty())
        assertNull(ResourceWarnings.aggregateExpiry(ResourceItem(issueDate = "31.02.26", yearsResource = "1")))
        assertNull(ResourceWarnings.oilExpiry(OilItem(byYears = "01.10.26", term = "-1")))
        assertEquals(ResourceWarningLevel.NONE, ResourceWarnings.calendar(null, today))
    }

    @Test fun expirationMatchesExistingCalculationsAndRefreshes() {
        val oil = OilItem(name = "Редуктор", byYears = "31.01.26", term = "0,125", coefficient = 1200)
        assertEquals(LocalDate.of(2026, 3, 31), ResourceWarnings.oilExpiry(oil))
        val item = ResourceItem(name = "Агрегат", issueDate = "10.10.25", yearsResource = "1", extendedTo1 = 10000, sne = 9990)
        assertEquals(today, ResourceWarnings.aggregateExpiry(item))
        assertEquals("0 дн.", item.calcRemaining2(today))
        val warnings = ResourceWarnings.aggregates(listOf(item), today.plusDays(1))
        assertEquals(2, warnings.size)
        assertEquals(ResourceWarningLevel.EXPIRED, warnings.first().level)
        assertTrue(warnings.first().text.contains("1 дн. назад"))
        assertTrue(ResourceWarnings.aggregates(listOf(item.copy(extendedTo1 = 20000, yearsResource = "5")), today).isEmpty())
    }
}
