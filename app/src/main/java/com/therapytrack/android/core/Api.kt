package com.therapytrack.android.core

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.URLEncoder
import java.time.Instant

/**
 * The endpoints the client side uses, typed. Every write that can be queued
 * takes `clientId` (reused on retry so the server recognises a replay),
 * the time it was *done*, and `asUser` (the account it belongs to).
 */
class Api(val client: ApiClient) {
    private val json = client.json

    private inline fun <reified T> decode(text: String): T = json.decodeFromString(text)

    // Auth ---------------------------------------------------------------

    suspend fun login(email: String, password: String): ApiUser = signIn(
        "/auth/login", buildJsonObject { put("email", email); put("password", password) })

    /** Therapists only: a client's account is created by their therapist and activated with an invite. */
    suspend fun register(name: String, email: String, password: String): ApiUser = signIn(
        "/auth/register", buildJsonObject { put("name", name); put("email", email); put("password", password); put("role", "therapist") })

    suspend fun lookUpInvite(code: String): ApiInviteLookup =
        decode(client.request("GET", "/auth/invite/${URLEncoder.encode(code, "UTF-8")}", asUser = null).body)

    /** Sets the password and signs in — acceptance *is* a sign-in. */
    suspend fun acceptInvite(code: String, password: String): ApiUser = signIn(
        "/auth/invite/accept", buildJsonObject { put("code", code); put("password", password) })

    private suspend fun signIn(path: String, body: JsonObject): ApiUser {
        // Not bound to an account: there is none yet, or it is being replaced.
        val response: LoginResponse = decode(client.request("POST", path, body, allowRefresh = false).body)
        client.storeSession(Session(response.token, response.refreshToken, response.user.id, response.user.role))
        return response.user
    }

    suspend fun me(): ApiUser = decode(client.request("GET", "/auth/me").body)
    suspend fun signOut() = client.signOut()

    // Client record ------------------------------------------------------

    suspend fun myPatientRecord(): ApiPatient = decode(client.request("GET", "/patients/me").body)

    // Check-ins ----------------------------------------------------------

    suspend fun checkIns(): List<ApiEmaResponse> =
        json.decodeFromString(ListSerializer(ApiEmaResponse.serializer()), client.request("GET", "/ema").body)

    suspend fun submitCheckIn(
        patientId: Int, mood: Int, anxiety: Int, sleep: Int, notes: String?,
        clientId: String, completedAt: Instant, asUser: Int
    ) {
        client.request("POST", "/ema", buildJsonObject {
            put("patient_id", patientId); put("mood_score", mood); put("anxiety_score", anxiety); put("sleep_score", sleep)
            notes?.let { put("notes", it) }
            put("completed_at", ApiTimestamp.iso8601(completedAt))   // when it was done, not sent
            put("client_id", clientId)
        }, asUser = asUser)
    }

    // Assessments --------------------------------------------------------

    suspend fun assessmentResults(instrument: Instrument, patientId: Int): List<ApiAssessmentResult> =
        json.decodeFromString(ListSerializer(ApiAssessmentResult.serializer()),
            client.request("GET", "/assessments/${instrument.path}?patient_id=$patientId").body)

    /** Raw item responses only: the server scores and decides on alerts. */
    suspend fun submitAssessment(
        instrument: Instrument, patientId: Int, responses: List<Int>,
        clientId: String, completedOn: Instant, asUser: Int
    ): ApiAssessmentResult = decode(client.request("POST", "/assessments/${instrument.path}", buildJsonObject {
        put("patient_id", patientId)
        putJsonArray("responses") { responses.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
        put("client_id", clientId)
        put("assessment_date", ApiTimestamp.dateOnly(completedOn))
    }, asUser = asUser).body)

    // Journal ------------------------------------------------------------

    suspend fun journalEntries(): List<ApiJournalEntry> =
        json.decodeFromString(ListSerializer(ApiJournalEntry.serializer()), client.request("GET", "/journal").body)

    suspend fun createJournalEntry(
        title: String?, content: String, isPrivate: Boolean,
        clientId: String, completedAt: Instant, asUser: Int
    ): ApiJournalEntry = decode(client.request("POST", "/journal", buildJsonObject {
        put("title", title ?: ""); put("content", content); put("is_private", isPrivate)
        put("completed_at", ApiTimestamp.iso8601(completedAt)); put("client_id", clientId)
    }, asUser = asUser).body)

    suspend fun deleteJournalEntry(id: Int) { client.request("DELETE", "/journal/$id") }

    // Messages -----------------------------------------------------------

    suspend fun conversation(withUserId: Int): ApiConversation =
        decode(client.request("GET", "/messages/conversation/$withUserId").body)

    suspend fun sendMessage(toUserId: Int, text: String, clientId: String, asUser: Int): ApiMessage =
        decode(client.request("POST", "/messages", buildJsonObject {
            put("to_user_id", toUserId); put("message", text); put("client_id", clientId)
        }, asUser = asUser).body)

    suspend fun markConversationRead(withUserId: Int) { client.request("PUT", "/messages/read-all/$withUserId", buildJsonObject {}) }

    // Data protection ----------------------------------------------------

    suspend fun consent(): ConsentState = decode(client.request("GET", "/privacy/consent").body)

    suspend fun recordConsent(purpose: String, granted: Boolean) {
        client.request("POST", "/privacy/consent", buildJsonObject { put("purpose", purpose); put("granted", granted) })
    }

    /** Raw bytes on purpose: an Art. 15 export must not lose fields the app does not know. */
    suspend fun exportMyData(): String = client.request("GET", "/privacy/export").body

    suspend fun erasurePreview(): ErasureOutcome = decode(client.request("GET", "/privacy/erasure/preview").body)

    suspend fun requestErasure(): ErasureOutcome {
        val response = try {
            client.request("POST", "/privacy/erasure", buildJsonObject { put("confirm", "ERASE") })
        } catch (e: ApiError.Server) {
            if (e.status == 409) return ErasureOutcome(erased = false, reason = e.message) else throw e
        }
        return decode(response.body)
    }

    enum class Instrument(val path: String, val displayName: String) {
        PHQ9("phq9", "PHQ-9"), GAD7("gad7", "GAD-7"), WAISR("wai-sr", "WAI-SR")
    }
}
