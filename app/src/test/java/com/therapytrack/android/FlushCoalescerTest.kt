package com.therapytrack.android

import com.therapytrack.android.offline.FlushCoalescer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class FlushCoalescerTest {
    @Test fun `a request during a drain causes one more drain, not zero and not many`() = runBlocking {
        val coalescer = FlushCoalescer()
        var runs = 0
        val gate = CompletableDeferred<Unit>()
        val first = async { coalescer.run { runs += 1; if (runs == 1) gate.await() } }
        while (runs == 0) yield()
        // Three triggers arrive while the first drain is stuck (say, timing out).
        repeat(3) { coalescer.run { runs += 1 } }
        gate.complete(Unit)
        first.await()
        assertEquals(2, runs)
    }

    @Test fun `a job that always asks again is bounded`() = runBlocking {
        val coalescer = FlushCoalescer()
        var runs = 0
        coalescer.run { runs += 1; coalescer.run { } }   // re-requests from inside every run
        assertEquals(8, runs)
    }

    @Test fun `a plain run happens once`() = runBlocking {
        val coalescer = FlushCoalescer()
        var runs = 0
        coalescer.run { runs += 1 }
        assertEquals(1, runs)
    }
}
