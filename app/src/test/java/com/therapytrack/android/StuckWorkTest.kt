package com.therapytrack.android

import com.therapytrack.android.offline.StuckItem
import com.therapytrack.android.offline.StuckWork
import com.therapytrack.android.offline.StuckWorkSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StuckWorkTest {
    private class Fake(val kind: StuckItem.Kind, vararg ids: Pair<String, Long>) : StuckWorkSource {
        val items = ids.map { (id, at) -> StuckItem(id, kind, id, at, 25, null) }.toMutableList()
        val retried = mutableListOf<String>()
        override suspend fun stuckWork() = items.toList()
        override suspend fun retryStuck(id: String): Boolean { retried += id; return false }
        override suspend fun discardStuck(id: String) { items.removeAll { it.id == id } }
    }

    @Test fun `everything stuck, oldest first`() = runBlocking {
        val a = Fake(StuckItem.Kind.CHECK_IN, "c1" to 300L)
        val b = Fake(StuckItem.Kind.JOURNAL_ENTRY, "j1" to 100L, "j2" to 200L)
        assertEquals(listOf("j1", "j2", "c1"), StuckWork.all(listOf(a, b)).map { it.id })
    }

    @Test fun `retry and discard go to the queue that owns the id`() = runBlocking {
        val a = Fake(StuckItem.Kind.CHECK_IN, "c1" to 1L)
        val b = Fake(StuckItem.Kind.MESSAGE, "m1" to 2L)
        StuckWork.retry("m1", listOf(a, b))
        assertEquals(listOf("m1"), b.retried); assertTrue(a.retried.isEmpty())
        StuckWork.discard("c1", listOf(a, b))
        assertTrue(a.items.isEmpty()); assertEquals(1, b.items.size)
        assertFalse("an id nobody owns is reported, not thrown", StuckWork.retry("gone", listOf(a, b)))
    }

    @Test fun `what discarding destroys is named per kind`() {
        assertTrue(StuckItem.Kind.JOURNAL_ENTRY.isIrreplaceable)
        assertTrue(StuckItem.Kind.ASSESSMENT.isIrreplaceable)
        assertFalse(StuckItem.Kind.CHECK_IN.isIrreplaceable)
        assertFalse(StuckItem.Kind.MESSAGE.isIrreplaceable)
    }

    @Test fun `summaries are one line`() {
        assertEquals("first line", StuckWork.oneLine("  \n first line \nsecond"))
        assertEquals(80, StuckWork.oneLine("x".repeat(200)).length)
        assertEquals("", StuckWork.oneLine("\n\n"))
    }
}
