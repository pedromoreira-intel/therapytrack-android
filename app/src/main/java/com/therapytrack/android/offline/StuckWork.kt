package com.therapytrack.android.offline

/**
 * One piece of work that has stopped retrying on its own. Each outbox gives up
 * after a set number of failures — right, but until surfaced it left a person
 * believing something had been sent.
 */
data class StuckItem(
    val id: String,
    val kind: Kind,
    /** One line, in the person's own words where there are any. */
    val summary: String,
    val writtenAtMillis: Long,
    val attempts: Int,
    val lastError: String?
) {
    enum class Kind(val isIrreplaceable: Boolean) {
        SESSION_NOTE(true),
        ASSESSMENT(true),
        CHECK_IN(false),
        MESSAGE(false),
        JOURNAL_ENTRY(true)
    }
}

/** An outbox that can report and act on work that has stopped retrying. */
interface StuckWorkSource {
    suspend fun stuckWork(): List<StuckItem>
    /** Try once more, now, because a person asked. Whether it went. */
    suspend fun retryStuck(id: String): Boolean
    /** The only path that destroys what someone wrote — always explicit. */
    suspend fun discardStuck(id: String)
}

object StuckWork {
    suspend fun all(sources: List<StuckWorkSource>): List<StuckItem> =
        sources.flatMap { it.stuckWork() }.sortedBy { it.writtenAtMillis }   // oldest first

    suspend fun retry(id: String, sources: List<StuckWorkSource>): Boolean {
        for (source in sources) if (source.stuckWork().any { it.id == id }) return source.retryStuck(id)
        return false
    }

    suspend fun discard(id: String, sources: List<StuckWorkSource>) {
        for (source in sources) if (source.stuckWork().any { it.id == id }) { source.discardStuck(id); return }
    }

    /** The first line, trimmed, capped — this appears in a list, not a reading view. */
    fun oneLine(text: String, limit: Int = 80): String {
        val line = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
        return if (line.length <= limit) line else line.take(limit - 1).trimEnd() + "…"
    }
}
