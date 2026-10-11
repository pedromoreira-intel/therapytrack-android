package com.therapytrack.android

import com.therapytrack.android.core.ApiSupervisionOffer
import com.therapytrack.android.core.SupervisionMarket
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class SupervisionMarketTest {
    @Test fun `prices are typed in euros, with a comma or a point`() {
        assertNull(SupervisionMarket.priceCents(""))          // to be agreed
        assertNull(SupervisionMarket.priceCents("   "))
        assertEquals(4000, SupervisionMarket.priceCents("40"))
        assertEquals(3750, SupervisionMarket.priceCents("37,50"))
        assertEquals(3750, SupervisionMarket.priceCents("37.5"))
        assertEquals(0, SupervisionMarket.priceCents("0"))
        assertEquals(SupervisionMarket.INVALID, SupervisionMarket.priceCents("-5"))
        assertEquals(SupervisionMarket.INVALID, SupervisionMarket.priceCents("quarenta"))
    }

    @Test fun `a saved price goes back into the form as it was typed`() {
        assertEquals("", SupervisionMarket.priceText(null))
        assertEquals("40", SupervisionMarket.priceText(4000))
        assertEquals("37.50", SupervisionMarket.priceText(3750))
        assertEquals(3750, SupervisionMarket.priceCents(SupervisionMarket.priceText(3750)))
    }

    @Test fun `a card shows the price in the reader's convention`() {
        assertNull(SupervisionMarket.priceLabel(null, "EUR"))
        assertTrue(SupervisionMarket.priceLabel(4000, "EUR", Locale.forLanguageTag("pt-PT"))!!.startsWith("40"))
        assertTrue(SupervisionMarket.priceLabel(3750, "EUR", Locale.forLanguageTag("pt-PT"))!!.contains("37,50"))
    }

    @Test fun `the start is read in the phone's zone and sent as an instant`() {
        val lisbon = ZoneId.of("Europe/Lisbon")
        // 18 October 2026 is still summer time in Lisbon (UTC+1).
        assertEquals(Instant.parse("2026-10-18T18:00:00Z"), SupervisionMarket.startsAt("2026-10-18", "19:00", lisbon))
        assertEquals(Instant.parse("2026-11-18T19:00:00Z"), SupervisionMarket.startsAt("2026-11-18", "19:00", lisbon))
        assertNull(SupervisionMarket.startsAt("18/10/2026", "19:00", lisbon))
        assertNull(SupervisionMarket.startsAt("2026-10-18", "7pm", lisbon))
    }

    @Test fun `topics are split on commas and trimmed`() {
        assertEquals(listOf("trauma", "casal"), SupervisionMarket.topics(" trauma, casal ,, "))
        assertEquals(emptyList<String>(), SupervisionMarket.topics(""))
    }

    @Test fun `an offer decodes as the server sends it`() {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val offer = json.decodeFromString(ApiSupervisionOffer.serializer(), """
            {"id":3,"kind":"group","title":"Supervisão de grupo","description":null,"topics":["trauma"],
             "starts_at":"2026-10-18T18:00:00.000Z","duration_minutes":90,"format":"online","location":null,
             "places":4,"places_left":3,"price_cents":null,"currency":"EUR","status":"open",
             "supervisor":{"user_id":7,"name":"Dra. Ana","headline":null,"city":"Porto","verification_status":"self_declared","accepting_supervisees":1},
             "my_place":{"id":11,"status":"requested"},"requests_waiting":2,"is_past":false}
        """)
        assertEquals("requested", offer.myPlace?.status)
        assertTrue(offer.supervisor.acceptingSupervisees)
        assertFalse(offer.isCancelled)
        assertEquals(2, offer.requestsWaiting)
    }
}
