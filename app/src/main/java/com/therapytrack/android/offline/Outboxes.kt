package com.therapytrack.android.offline

import com.therapytrack.android.core.Api
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

// One file per kind of work. Each item carries the account that wrote it, an
// idempotency key generated once and reused on every retry, and the time it
// was done — never re-derived at send time.

@Serializable
data class PendingCheckIn(
    override val id: String = UUID.randomUUID().toString(),
    override val clientId: String = UUID.randomUUID().toString(),
    override val ownerUserId: Int,
    val patientId: Int,
    val mood: Int, val anxiety: Int, val sleep: Int,
    val notes: String? = null,
    override val createdAtMillis: Long = System.currentTimeMillis(),
    override val attempts: Int = 0,
    override val lastError: String? = null
) : Pending

@Serializable
data class PendingAssessment(
    override val id: String = UUID.randomUUID().toString(),
    override val clientId: String = UUID.randomUUID().toString(),
    override val ownerUserId: Int,
    val instrument: Api.Instrument,
    val patientId: Int,
    val responses: List<Int>,
    override val createdAtMillis: Long = System.currentTimeMillis(),
    override val attempts: Int = 0,
    override val lastError: String? = null
) : Pending

@Serializable
data class PendingJournalEntry(
    override val id: String = UUID.randomUUID().toString(),
    override val clientId: String = UUID.randomUUID().toString(),
    override val ownerUserId: Int,
    val title: String?,
    val content: String,
    /** Travels with the entry, never re-derived: a private entry resent as shared cannot be taken back. */
    val isPrivate: Boolean,
    override val createdAtMillis: Long = System.currentTimeMillis(),
    override val attempts: Int = 0,
    override val lastError: String? = null
) : Pending

@Serializable
data class PendingMessage(
    override val id: String = UUID.randomUUID().toString(),
    override val clientId: String = UUID.randomUUID().toString(),
    override val ownerUserId: Int,
    val toUserId: Int,
    val text: String,
    override val createdAtMillis: Long = System.currentTimeMillis(),
    override val attempts: Int = 0,
    override val lastError: String? = null
) : Pending

class CheckInOutbox(queue: DurableQueue<PendingCheckIn>, private val api: Api) :
    Outbox<PendingCheckIn>(queue, { api.client.currentUserId }, StuckItem.Kind.CHECK_IN) {

    suspend fun submit(patientId: Int, mood: Int, anxiety: Int, sleep: Int, notes: String?): SaveOutcome {
        val owner = api.client.currentUserId ?: return SaveOutcome.NOT_STORED
        return submit(PendingCheckIn(ownerUserId = owner, patientId = patientId, mood = mood, anxiety = anxiety, sleep = sleep, notes = notes))
    }
    override suspend fun send(item: PendingCheckIn) = api.submitCheckIn(
        item.patientId, item.mood, item.anxiety, item.sleep, item.notes, item.clientId, Instant.ofEpochMilli(item.createdAtMillis), item.ownerUserId)
    override fun summary(item: PendingCheckIn) = "Humor ${item.mood}, ansiedade ${item.anxiety}, sono ${item.sleep}"
    override fun recordAttempt(item: PendingCheckIn, error: Throwable) = item.copy(attempts = item.attempts + 1, lastError = error.message)
}

class AssessmentOutbox(queue: DurableQueue<PendingAssessment>, private val api: Api) :
    Outbox<PendingAssessment>(queue, { api.client.currentUserId }, StuckItem.Kind.ASSESSMENT) {

    suspend fun submit(instrument: Api.Instrument, patientId: Int, responses: List<Int>): SaveOutcome {
        val owner = api.client.currentUserId ?: return SaveOutcome.NOT_STORED
        return submit(PendingAssessment(ownerUserId = owner, instrument = instrument, patientId = patientId, responses = responses))
    }
    override suspend fun send(item: PendingAssessment) { api.submitAssessment(
        item.instrument, item.patientId, item.responses, item.clientId, Instant.ofEpochMilli(item.createdAtMillis), item.ownerUserId) }
    override fun summary(item: PendingAssessment) = item.instrument.displayName
    override fun recordAttempt(item: PendingAssessment, error: Throwable) = item.copy(attempts = item.attempts + 1, lastError = error.message)
}

class JournalOutbox(queue: DurableQueue<PendingJournalEntry>, private val api: Api) :
    Outbox<PendingJournalEntry>(queue, { api.client.currentUserId }, StuckItem.Kind.JOURNAL_ENTRY) {

    suspend fun save(title: String?, content: String, isPrivate: Boolean): SaveOutcome {
        val owner = api.client.currentUserId ?: return SaveOutcome.NOT_STORED
        return submit(PendingJournalEntry(ownerUserId = owner, title = title, content = content, isPrivate = isPrivate))
    }
    override suspend fun send(item: PendingJournalEntry) { api.createJournalEntry(
        item.title, item.content, item.isPrivate, item.clientId, Instant.ofEpochMilli(item.createdAtMillis), item.ownerUserId) }
    override fun summary(item: PendingJournalEntry) = StuckWork.oneLine(item.title?.takeIf { it.isNotBlank() } ?: item.content)
    override fun recordAttempt(item: PendingJournalEntry, error: Throwable) = item.copy(attempts = item.attempts + 1, lastError = error.message)
}

class MessageOutbox(queue: DurableQueue<PendingMessage>, private val api: Api) :
    Outbox<PendingMessage>(queue, { api.client.currentUserId }, StuckItem.Kind.MESSAGE) {

    suspend fun send(toUserId: Int, text: String): SaveOutcome {
        val owner = api.client.currentUserId ?: return SaveOutcome.NOT_STORED
        return submit(PendingMessage(ownerUserId = owner, toUserId = toUserId, text = text))
    }
    suspend fun pending(toUserId: Int): List<PendingMessage> = mine().filter { it.toUserId == toUserId }
    override suspend fun send(item: PendingMessage) { api.sendMessage(item.toUserId, item.text, item.clientId, item.ownerUserId) }
    override fun summary(item: PendingMessage) = StuckWork.oneLine(item.text)
    override fun recordAttempt(item: PendingMessage, error: Throwable) = item.copy(attempts = item.attempts + 1, lastError = error.message)
}

@Serializable
data class PendingSessionNote(
    override val id: String = UUID.randomUUID().toString(),
    override val clientId: String = UUID.randomUUID().toString(),
    override val ownerUserId: Int,
    val patientId: Int,
    /** Kept so the stuck-work row can name the client without a network. */
    val patientName: String,
    val sessionDate: String,
    val focus: String?,
    val interventions: String,
    val progressNotes: String,
    val homework: String?,
    val riskLevel: String,
    val nextSessionPlan: String?,
    override val createdAtMillis: Long = System.currentTimeMillis(),
    override val attempts: Int = 0,
    override val lastError: String? = null
) : Pending

/** Clinical text only this therapist has, so it retries longer than the others before it is called stuck. */
class SessionNoteOutbox(queue: DurableQueue<PendingSessionNote>, private val api: Api) :
    Outbox<PendingSessionNote>(queue, { api.client.currentUserId }, StuckItem.Kind.SESSION_NOTE, maxAttempts = 50) {

    suspend fun save(patientId: Int, patientName: String, sessionDate: String, focus: String?, interventions: String,
                     progressNotes: String, homework: String?, riskLevel: String, nextSessionPlan: String?): SaveOutcome {
        val owner = api.client.currentUserId ?: return SaveOutcome.NOT_STORED
        return submit(PendingSessionNote(ownerUserId = owner, patientId = patientId, patientName = patientName, sessionDate = sessionDate,
            focus = focus, interventions = interventions, progressNotes = progressNotes, homework = homework,
            riskLevel = riskLevel, nextSessionPlan = nextSessionPlan))
    }
    suspend fun pending(patientId: Int): List<PendingSessionNote> = mine().filter { it.patientId == patientId }
    override suspend fun send(item: PendingSessionNote) { api.createSessionNote(
        item.patientId, item.sessionDate, item.focus, item.interventions, item.progressNotes, item.homework,
        item.riskLevel, item.nextSessionPlan, item.clientId, item.ownerUserId) }
    override fun summary(item: PendingSessionNote) = "${item.patientName} · ${item.sessionDate}"
    override fun recordAttempt(item: PendingSessionNote, error: Throwable) = item.copy(attempts = item.attempts + 1, lastError = error.message)
}
