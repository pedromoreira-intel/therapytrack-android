package com.therapytrack.android

import com.therapytrack.android.core.ApiTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ApiTimestampTest {
    @Test fun `parses every shape the server sends`() {
        assertEquals(Instant.parse("2026-09-18T10:00:00Z"), ApiTimestamp.parse("2026-09-18T10:00:00.000Z"))
        assertEquals(Instant.parse("2026-09-18T10:00:00Z"), ApiTimestamp.parse("2026-09-18T10:00:00Z"))
        assertEquals(Instant.parse("2026-09-18T10:00:00Z"), ApiTimestamp.parse("2026-09-18 10:00:00"))   // SQLite datetime()
        assertEquals(Instant.parse("2026-09-18T10:00:00Z"), ApiTimestamp.parse("2026-09-18T10:00:00"))
        assertNotNull(ApiTimestamp.parse("2026-09-18"))
        assertNull(ApiTimestamp.parse(""))
        assertNull(ApiTimestamp.parse("yesterday"))
    }

    @Test fun `sends whole-second UTC and a plain date`() {
        val t = Instant.parse("2026-09-18T10:00:00.123Z")
        assertEquals("2026-09-18T10:00:00Z", ApiTimestamp.iso8601(t))
        assertEquals(10, ApiTimestamp.dateOnly(t).length)
    }
}

class LenientBooleanTest {
    @kotlinx.serialization.Serializable
    data class Row(@kotlinx.serialization.Serializable(with = com.therapytrack.android.core.LenientBoolean::class) val flag: Boolean)

    @Test fun `accepts both the Postgres and the SQLite shape`() {
        val json = kotlinx.serialization.json.Json
        assertEquals(true, json.decodeFromString(Row.serializer(), """{"flag":true}""").flag)
        assertEquals(true, json.decodeFromString(Row.serializer(), """{"flag":1}""").flag)
        assertEquals(false, json.decodeFromString(Row.serializer(), """{"flag":0}""").flag)
        assertEquals(false, json.decodeFromString(Row.serializer(), """{"flag":false}""").flag)
    }
}
