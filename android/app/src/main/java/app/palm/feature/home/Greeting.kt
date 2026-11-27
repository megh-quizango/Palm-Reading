package app.palm.feature.home

import androidx.annotation.StringRes
import app.palm.R
import java.time.LocalTime

enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

/** D-11: 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-20:59 evening, else night. */
fun dayPart(time: LocalTime): DayPart = when (time.hour) {
    in 5..11 -> DayPart.MORNING
    in 12..16 -> DayPart.AFTERNOON
    in 17..20 -> DayPart.EVENING
    else -> DayPart.NIGHT
}

@StringRes
fun greetingRes(part: DayPart): Int = when (part) {
    DayPart.MORNING -> R.string.home_greeting_morning
    DayPart.AFTERNOON -> R.string.home_greeting_afternoon
    DayPart.EVENING -> R.string.home_greeting_evening
    DayPart.NIGHT -> R.string.home_greeting_night
}
