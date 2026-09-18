package com.therapytrack.android.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** The server's few date shapes, and the two the app sends. */
object ApiTimestamp {
    private val local = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")   // SQLite datetime()
    private val localT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun parse(raw: String?): Instant? {
        if (raw.isNullOrEmpty()) return null
        runCatching { return Instant.parse(raw) }
        runCatching { return java.time.OffsetDateTime.parse(raw).toInstant() }
        runCatching { return LocalDateTime.parse(raw, local).toInstant(ZoneOffset.UTC) }
        runCatching { return LocalDateTime.parse(raw, localT).toInstant(ZoneOffset.UTC) }
        return try { LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant() } catch (e: DateTimeParseException) { null }
    }

    /** `2026-09-18` in the device's zone: the day the person completed it. */
    fun dateOnly(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()

    /** RFC 3339 in UTC, whole seconds. */
    fun iso8601(instant: Instant): String =
        DateTimeFormatter.ISO_INSTANT.format(instant.with(java.time.temporal.ChronoField.NANO_OF_SECOND, 0))
}
