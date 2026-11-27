package app.palm.feature.onboarding

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

val MIN_BIRTH_DATE: LocalDate = LocalDate.of(1900, 1, 1)

fun isValidBirthDate(date: LocalDate, today: LocalDate): Boolean = !date.isBefore(MIN_BIRTH_DATE) && !date.isAfter(today)

// M3 DatePicker millis are UTC midnight; the device zone would shift the day (research Pitfall 5)
fun utcMillisToDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

fun dateToUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
