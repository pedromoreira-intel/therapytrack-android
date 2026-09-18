package com.therapytrack.android.offline

import com.therapytrack.android.core.ApiError

/** Whether a write reached the server, is held on device, or is nowhere. */
enum class SaveOutcome {
    /** The server has it. */
    SENT,
    /** On disk and will retry. Safe, but not yet with the therapist. */
    QUEUED,
    /** Neither sent nor written to disk. The caller must keep the draft. */
    NOT_STORED
}

/** A queued item every outbox holds. */
interface Pending : OwnedWork {
    val id: String
    val clientId: String
    val createdAtMillis: Long
    val attempts: Int
    val lastError: String?
}

/**
 * The drain loop and stuck-work handling every outbox shares. Subclasses say
 * how to send one item and how to describe it.
 */
abstract class Outbox<Item : Pending>(
    private val queue: DurableQueue<Item>,
    private val currentUserId: () -> Int?,
    private val kind: StuckItem.Kind,
    private val maxAttempts: Int = 25
) : StuckWorkSource {
    private val coalescer = FlushCoalescer()

    /** Bumped whenever the queue changes, so a screen showing pending work can re-query after a drain. */
    val changes = kotlinx.coroutines.flow.MutableStateFlow(0)
    private fun changed() { changes.value += 1 }

    protected abstract suspend fun send(item: Item)
    protected abstract fun summary(item: Item): String
    protected abstract fun recordAttempt(item: Item, error: Throwable): Item

    /** Write to disk first, then try the network once. */
    protected suspend fun submit(item: Item): SaveOutcome {
        val onDisk = queue.append(item)
        return try {
            send(item)
            queue.remove(item.id)
            SaveOutcome.SENT
        } catch (e: Exception) {
            if (!onDisk) { queue.remove(item.id); return SaveOutcome.NOT_STORED }
            queue.update(item.id) { recordAttempt(it, e) }
            SaveOutcome.QUEUED
        } finally { changed() }
    }

    suspend fun flush() = coalescer.run { drainOnce() }

    suspend fun mine(): List<Item> = queue.all().ownedBy(currentUserId())
    suspend fun pendingCount(): Int = mine().size

    private suspend fun drainOnce() {
        val items = mine().filter { it.attempts < maxAttempts }
        if (items.isEmpty()) return
        try {
            for (item in items) {
                try {
                    send(item)
                    queue.remove(item.id)
                } catch (e: ApiError.AccountChanged) {
                    return   // another account signed in mid-drain; leave the rest
                } catch (e: Exception) {
                    queue.update(item.id) { recordAttempt(it, e) }
                }
            }
        } finally { changed() }
    }

    override suspend fun stuckWork(): List<StuckItem> = mine().filter { it.attempts >= maxAttempts }.map {
        StuckItem(it.id, kind, summary(it), it.createdAtMillis, it.attempts, it.lastError)
    }

    override suspend fun retryStuck(id: String): Boolean {
        val item = mine().firstOrNull { it.id == id } ?: return true
        return try { send(item); queue.remove(item.id); true } catch (e: Exception) { queue.update(item.id) { recordAttempt(it, e) }; false } finally { changed() }
    }

    override suspend fun discardStuck(id: String) {
        if (mine().any { it.id == id }) { queue.remove(id); changed() }
    }
}
