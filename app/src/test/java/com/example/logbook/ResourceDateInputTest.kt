package com.example.logbook

import com.example.logbook.ui.util.expandShortYear
import com.example.logbook.ui.util.formatResourceDateInput
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceDateInputTest {
    @Test fun maskAcceptsShortAndFullYear() {
        assertEquals("12.05.25", formatResourceDateInput("120525"))
        assertEquals("12.05.2025", formatResourceDateInput("12052025"))
        assertEquals("12.05.2025", formatResourceDateInput("120520251"))
        // Backspace in a full date keeps the remaining digits instead of cutting the year.
        assertEquals("12.05.202", formatResourceDateInput("12.05.202"))
    }

    @Test fun shortYearExpandsOnlyWhenComplete() {
        assertEquals("12.05.2025", expandShortYear("12.05.25"))
        assertEquals("12.05.2025", expandShortYear("12.05.2025"))
        assertEquals("12.05.2", expandShortYear("12.05.2"))
        assertEquals("", expandShortYear(""))
    }
}
