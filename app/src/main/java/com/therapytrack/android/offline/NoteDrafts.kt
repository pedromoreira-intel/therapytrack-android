package com.therapytrack.android.offline

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** A session note in progress. Kept encrypted on this phone, never sent. */
@Serializable
data class SessionNoteDraft(
    val focus: String = "",
    val interventions: List<String> = emptyList(),
    val notes: String = "",
    val homework: String = "",
    val plan: String = "",
    val risk: String? = null,
    /** The client's topics ticked as talked about; recorded when the note is filed. */
    val discussedTopics: List<Int> = emptyList()
) {
    val isEmpty get() = focus.isEmpty() && interventions.isEmpty() && notes.isEmpty() && homework.isEmpty() && plan.isEmpty() &&
        risk == null && discussedTopics.isEmpty()
}

/**
 * One draft per session, so two sessions on one day keep separate drafts;
 * per day for an unscheduled note. Each draft is its own encrypted file.
 */
class NoteDrafts(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val dir get() = File(context.filesDir, "note-drafts").apply { mkdirs() }

    fun key(userId: Int?, patientId: Int, sessionId: Int?, day: String) =
        "note-draft-${userId ?: 0}-$patientId-${sessionId?.let { "s$it" } ?: day}.json"

    private fun storage(key: String) = EncryptedQueueStorage(context, "note-drafts/$key")

    fun load(key: String): SessionNoteDraft? = runCatching {
        storage(key).read()?.let { json.decodeFromString(SessionNoteDraft.serializer(), it.decodeToString()) }
    }.getOrNull()

    fun save(key: String, draft: SessionNoteDraft) {
        dir
        if (draft.isEmpty) remove(key) else storage(key).write(json.encodeToString(SessionNoteDraft.serializer(), draft).encodeToByteArray())
    }

    fun remove(key: String) { File(dir, key).delete() }
}
