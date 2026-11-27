package app.palm.feature.home

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    private fun at(h: Int, m: Int) = dayPart(LocalTime.of(h, m))

    @Test fun windows() {
        assertEquals(DayPart.NIGHT, at(4, 59))
        assertEquals(DayPart.MORNING, at(5, 0))
        assertEquals(DayPart.MORNING, at(11, 59))
        assertEquals(DayPart.AFTERNOON, at(12, 0))
        assertEquals(DayPart.AFTERNOON, at(16, 59))
        assertEquals(DayPart.EVENING, at(17, 0))
        assertEquals(DayPart.EVENING, at(20, 59))
        assertEquals(DayPart.NIGHT, at(21, 0))
        assertEquals(DayPart.NIGHT, at(0, 0))
    }
}
