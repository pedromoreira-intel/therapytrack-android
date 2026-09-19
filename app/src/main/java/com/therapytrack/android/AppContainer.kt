package com.therapytrack.android

import android.content.Context
import com.therapytrack.android.core.Api
import com.therapytrack.android.core.ApiClient
import com.therapytrack.android.core.EncryptedSessionStore
import com.therapytrack.android.core.SessionStore
import com.therapytrack.android.offline.AssessmentOutbox
import com.therapytrack.android.offline.CheckInOutbox
import com.therapytrack.android.offline.DurableQueue
import com.therapytrack.android.offline.DurableQueueStore
import com.therapytrack.android.offline.EncryptedQueueStorage
import com.therapytrack.android.offline.JournalOutbox
import com.therapytrack.android.offline.JsonQueueCodec
import com.therapytrack.android.offline.MessageOutbox
import com.therapytrack.android.offline.Pending
import com.therapytrack.android.offline.PendingAssessment
import com.therapytrack.android.offline.PendingCheckIn
import com.therapytrack.android.offline.PendingJournalEntry
import com.therapytrack.android.offline.PendingMessage
import com.therapytrack.android.offline.PendingSessionNote
import com.therapytrack.android.offline.SessionNoteOutbox
import com.therapytrack.android.offline.PendingSupervision
import com.therapytrack.android.offline.SupervisionOutbox
import com.therapytrack.android.core.ApiPlan
import kotlinx.coroutines.flow.MutableStateFlow
import com.therapytrack.android.offline.StuckWorkSource
import kotlinx.serialization.KSerializer

/** Everything long-lived, built once per process. Plain constructors; no DI framework. */
class AppContainer(private val app: Context) {
    val sessions: SessionStore = EncryptedSessionStore(app)
    val client = ApiClient(BuildConfig.API_BASE_URL, sessions)
    val api = Api(client)

    private inline fun <reified T : Pending> queue(name: String, serializer: KSerializer<T>): DurableQueue<T> {
        val store = DurableQueueStore(EncryptedQueueStorage(app, name), JsonQueueCodec(serializer), { it.id }) { android.util.Log.w("Queue", it) }
        return DurableQueue(store, { it.id }) { android.util.Log.w("Queue", it) }
    }

    val checkIns = CheckInOutbox(queue("pending-check-ins.json", PendingCheckIn.serializer()), api)
    val assessments = AssessmentOutbox(queue("pending-assessments.json", PendingAssessment.serializer()), api)
    val journal = JournalOutbox(queue("pending-journal-entries.json", PendingJournalEntry.serializer()), api)
    val messages = MessageOutbox(queue("pending-messages.json", PendingMessage.serializer()), api)
    val sessionNotes = SessionNoteOutbox(queue("pending-session-notes.json", PendingSessionNote.serializer()), api)

    val supervision = SupervisionOutbox(queue("pending-supervision.json", PendingSupervision.serializer()), api)

    val stuckSources: List<StuckWorkSource> get() = listOf(checkIns, assessments, journal, messages, sessionNotes, supervision)

    /**
     * The therapist's plan as last read from the server. Only used to *say*
     * what the plan is; every gate is the server's 402, never a local check.
     */
    val plan = MutableStateFlow<ApiPlan?>(null)
    suspend fun refreshPlan() { runCatching { plan.value = api.plan() } }

    /** Retry everything queued. Safe on launch, on foreground, and when the network returns. */
    suspend fun drainQueues() {
        checkIns.flush(); assessments.flush(); journal.flush(); messages.flush(); sessionNotes.flush(); supervision.flush()
    }
}
