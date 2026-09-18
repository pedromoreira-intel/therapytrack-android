package com.therapytrack.android.offline

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs a drain, and if another is asked for while one is in flight, runs it
 * once more when the current one finishes.
 *
 * A plain "already running → return" drops the request that matters most: the
 * reconnect that arrives while a no-signal drain is still timing out item by
 * item. Coalescing keeps "never two at once" while making sure a request that
 * arrives mid-drain still causes one.
 */
class FlushCoalescer {
    private val state = Mutex()
    private var running = false
    private var repeatRequested = false

    suspend fun run(job: suspend () -> Unit) {
        state.withLock {
            if (running) { repeatRequested = true; return }
            running = true
        }
        try {
            // Bounded: triggers from outside are finite; a job that always asks
            // for another run must not spin here forever.
            var iterations = 0
            do {
                // Cleared before the job so a request arriving *during* it survives.
                state.withLock { repeatRequested = false }
                job()
                iterations += 1
            } while (state.withLock { repeatRequested } && iterations < MAX_ITERATIONS)
        } finally {
            state.withLock { running = false }
        }
    }

    private companion object { const val MAX_ITERATIONS = 8 }
}
