package app.palm.feature.onboarding

import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthRulesTest {
    private val savedZone: TimeZone = TimeZone.getDefault()
    private val today = LocalDate.of(2026, 11, 27)
    private val date = LocalDate.of(1996, 3, 12)
    private val utcMidnight = 826_588_800_000L // 1996-03-12T00:00:00Z

    @After fun restoreZone() = TimeZone.setDefault(savedZone)

    @Test fun firstAllowedDay() = assertTrue(isValidBirthDate(LocalDate.of(1900, 1, 1), today))

    @Test fun dayBeforeMinimum() = assertFalse(isValidBirthDate(LocalDate.of(1899, 12, 31), today))

    @Test fun today() = assertTrue(isValidBirthDate(today, today))

    @Test fun tomorrow() = assertFalse(isValidBirthDate(today.plusDays(1), today))

    private fun assertNoShiftIn(zone: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        assertEquals(date, utcMillisToDate(dateToUtcMillis(date)))
        assertEquals(utcMidnight, dateToUtcMillis(date))
        assertEquals(date, utcMillisToDate(utcMidnight))
    }

    @Test fun noDayShiftWestOfUtc() = assertNoShiftIn("America/Sao_Paulo")

    @Test fun noDayShiftEastOfUtc() = assertNoShiftIn("Asia/Kolkata")
}
