package com.therapytrack.android

import com.therapytrack.android.core.ApiSessionNote
import com.therapytrack.android.core.noteFor
import com.therapytrack.android.offline.PendingSessionNote
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Notes are found by their session, so two sessions on one day each have their own. */
class SessionNoteLinkTest {
    private fun note(id: Int, day: String, session: Int?) =
        ApiSessionNote(id = id, patientId = 1, sessionNumber = id, sessionDate = "${day}T10:00:00Z", sessionId = session)

    @Test fun linkedNoteIsFoundBySession() {
        val notes = listOf(note(1, "2026-10-20", 7), note(2, "2026-10-20", 8))
        assertEquals(1, notes.noteFor(7, "2026-10-20")?.id)
        assertEquals(2, notes.noteFor(8, "2026-10-20")?.id)
    }

    @Test fun aSecondSessionThatDayHasNoNoteYet() {
        val notes = listOf(note(1, "2026-10-20", 7))
        assertNull(notes.noteFor(9, "2026-10-20"))
    }

    @Test fun anOldUnlinkedNoteIsFoundByDay() {
        val notes = listOf(note(1, "2026-10-19", null))
        assertEquals(1, notes.noteFor(5, "2026-10-19")?.id)
        assertNull(notes.noteFor(5, "2026-10-20"))
    }

    @Test fun aNoteQueuedByAnOlderBuildStillReads() {
        val old = """{"ownerUserId":3,"patientId":1,"patientName":"x","sessionDate":"2026-10-20","focus":null,"interventions":"a","progressNotes":"b","homework":null,"riskLevel":"LOW","nextSessionPlan":null}"""
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(PendingSessionNote.serializer(), old)
        assertNull(decoded.sessionId)
    }
}
