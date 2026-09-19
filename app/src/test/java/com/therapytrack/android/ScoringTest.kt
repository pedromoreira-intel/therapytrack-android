package com.therapytrack.android

import com.therapytrack.android.clinical.Scoring
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.jsonObject

/** Bands and cut-offs match the server's (services/psychometrics.js) and the iOS app's. */
class ScoringTest {
    @Test fun `PHQ-9 bands at the published cut-offs`() {
        assertEquals("minimal", Scoring.serverKey(Scoring.phq9Band(4)))
        assertEquals("mild", Scoring.serverKey(Scoring.phq9Band(5)))
        assertEquals("moderate", Scoring.serverKey(Scoring.phq9Band(14)))
        assertEquals("moderately_severe", Scoring.serverKey(Scoring.phq9Band(15)))
        assertEquals("severe", Scoring.serverKey(Scoring.phq9Band(20)))
    }

    @Test fun `GAD-7 bands at the published cut-offs`() {
        assertEquals("minimal", Scoring.serverKey(Scoring.gad7Band(0)))
        assertEquals("mild", Scoring.serverKey(Scoring.gad7Band(9)))
        assertEquals("moderate", Scoring.serverKey(Scoring.gad7Band(10)))
        assertEquals("severe", Scoring.serverKey(Scoring.gad7Band(15)))
    }

    @Test fun `WAI-SR reverses item 4 and averages`() {
        val all3 = List(12) { 3 }
        assertEquals(3.0, Scoring.waiSrMean(all3), 1e-9)
        val item4Low = List(12) { if (it == Scoring.WAISR_REVERSED_INDEX) 1 else 5 }
        assertEquals(5.0, Scoring.waiSrMean(item4Low), 1e-9)
        assertEquals("strong", Scoring.serverKey(Scoring.waiSrAlliance(4.5)))
        assertEquals("good", Scoring.serverKey(Scoring.waiSrAlliance(4.49)))
        assertEquals("weak", Scoring.serverKey(Scoring.waiSrAlliance(2.99)))
    }
}

class PlanTest {
    @Test fun `feature lookup is a plain membership test on what the server sent`() {
        val p = com.therapytrack.android.core.ApiPlan(plan = "professional", features = listOf("clients.unlimited", "session_notes"))
        assertEquals(true, p.has("session_notes"))
        assertEquals(false, p.has("directory"))
        assertEquals(false, com.therapytrack.android.core.ApiPlan().has("messaging"))   // nothing sent → nothing assumed
    }
}

class NoteDraftTest {
    @Test fun `a draft maps the model's fields and leaves the rest alone`() {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"drafted":true,"focus":"Sleep","interventions":"Breathing","progress_notes":"Better sleep","risk_level":"LOW","next_session_plan":"Review diary"}""").jsonObject
        val d = com.therapytrack.android.ui.therapist.run { json.toNoteDraft() }
        assertEquals("Sleep", d.focus); assertEquals("Breathing", d.interventions); assertEquals("Better sleep", d.progressNotes)
        assertEquals(null, d.homework); assertEquals("Review diary", d.plan)
    }
}
