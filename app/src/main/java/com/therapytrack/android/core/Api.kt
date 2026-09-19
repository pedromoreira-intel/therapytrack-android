package com.therapytrack.android.core

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    // Therapist: caseload -------------------------------------------------

    suspend fun patients(): List<ApiPatient> =
        json.decodeFromString(ListSerializer(ApiPatient.serializer()), client.request("GET", "/patients").body)

    suspend fun patient(id: Int): ApiPatient = decode(client.request("GET", "/patients/$id").body)

    /** Creates the record and returns the one-time invite the therapist passes on. */
    suspend fun createPatient(name: String, email: String, diagnosis: String?, riskLevel: String): CreatedPatient {
        val body = client.request("POST", "/patients", buildJsonObject {
            put("name", name); put("email", email); put("risk_level", riskLevel)
            diagnosis?.takeIf { it.isNotBlank() }?.let { put("diagnosis", it) }
        }).body
        val obj = json.parseToJsonElement(body).jsonObject
        return CreatedPatient(
            json.decodeFromJsonElement(ApiPatient.serializer(), obj),
            obj["invite_code"]?.jsonPrimitive?.contentOrNull,
            obj["invite_expires_at"]?.jsonPrimitive?.contentOrNull)
    }

    /** Reissue a client's invitation; revokes any previous unused code. */
    suspend fun reissueInvite(patientId: Int): ApiInviteIssued =
        decode(client.request("POST", "/patients/$patientId/invite", buildJsonObject {}).body)

    suspend fun brief(patientId: Int): ApiBrief = decode(client.request("GET", "/patients/$patientId/brief").body)

    suspend fun checkIns(patientId: Int): List<ApiEmaResponse> =
        json.decodeFromString(ListSerializer(ApiEmaResponse.serializer()), client.request("GET", "/ema?patient_id=$patientId").body)

    // Therapist: sessions and notes ---------------------------------------

    suspend fun sessions(date: String? = null, patientId: Int? = null): List<ApiSession> {
        val params = listOfNotNull(date?.let { "date=$it" }, patientId?.let { "patient_id=$it" })
        val path = "/sessions" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return json.decodeFromString(ListSerializer(ApiSession.serializer()), client.request("GET", path).body)
    }

    suspend fun createSession(patientId: Int, sessionDate: String, durationMinutes: Int, notes: String?) {
        client.request("POST", "/sessions", buildJsonObject {
            put("patient_id", patientId); put("session_date", sessionDate); put("duration_minutes", durationMinutes)
            notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        })
    }

    suspend fun updateSession(id: Int, status: String) {
        client.request("PUT", "/sessions/$id", buildJsonObject { put("status", status) })
    }

    suspend fun sessionNotes(patientId: Int): List<ApiSessionNote> =
        json.decodeFromString(ListSerializer(ApiSessionNote.serializer()), client.request("GET", "/session-notes?patient_id=$patientId").body)

    /** The server numbers the session from the patient's existing notes. */
    suspend fun createSessionNote(
        patientId: Int, sessionDate: String, focus: String?, interventions: String, progressNotes: String,
        homework: String?, riskLevel: String, nextSessionPlan: String?, clientId: String, asUser: Int
    ): ApiSessionNoteCreated = decode(client.request("POST", "/session-notes", buildJsonObject {
        put("patient_id", patientId); put("session_date", sessionDate)
        put("interventions", interventions); put("progress_notes", progressNotes); put("risk_level", riskLevel)
        focus?.let { put("focus", it) }; homework?.let { put("homework", it) }; nextSessionPlan?.let { put("next_session_plan", it) }
        put("client_id", clientId)
    }, asUser = asUser).body)

    // Therapist: alerts and threads ---------------------------------------

    suspend fun alerts(includeAcknowledged: Boolean = false): List<ApiClinicalAlert> =
        json.decodeFromString(ListSerializer(ApiClinicalAlert.serializer()), client.request("GET", "/alerts?include_acknowledged=$includeAcknowledged").body)

    suspend fun acknowledgeAlert(id: Int) { client.request("POST", "/alerts/$id/acknowledge", buildJsonObject {}) }

    suspend fun threads(): ApiThreads = decode(client.request("GET", "/messages/threads").body)

    // Plan, professional records, supervision ------------------------------

    suspend fun plan(): ApiPlan = decode(client.request("GET", "/billing/me").body)

    suspend fun credentials(): List<ApiCredential> = list(ApiCredential.serializer(), "/professional/credentials")
    suspend fun saveCredential(id: Int?, licenseType: String, licenseNumber: String?, issuingBody: String?, region: String?, expiresOn: String?, notes: String?): ApiCredential {
        val body = buildJsonObject {
            put("license_type", licenseType); licenseNumber?.let { put("license_number", it) }; issuingBody?.let { put("issuing_body", it) }
            region?.let { put("region", it) }; expiresOn?.let { put("expires_on", it) }; notes?.let { put("notes", it) }
        }
        return decode(if (id == null) client.request("POST", "/professional/credentials", body).body
                      else client.request("PUT", "/professional/credentials/$id", body).body)
    }
    suspend fun deleteCredential(id: Int) { client.request("DELETE", "/professional/credentials/$id") }

    suspend fun training(): List<ApiTraining> = list(ApiTraining.serializer(), "/professional/training")
    suspend fun addTraining(title: String, institution: String?, type: String, completedOn: String, hours: Double, notes: String?): ApiTraining =
        decode(client.request("POST", "/professional/training", buildJsonObject {
            put("title", title); institution?.let { put("institution", it) }; put("training_type", type)
            put("completed_on", completedOn); put("hours", hours); notes?.let { put("notes", it) }
        }).body)
    suspend fun deleteTraining(id: Int) { client.request("DELETE", "/professional/training/$id") }

    suspend fun professionalSummary(): ApiProfessionalSummary = decode(client.request("GET", "/professional/summary").body)

    suspend fun supervisionSessions(): List<ApiSupervisionSession> = list(ApiSupervisionSession.serializer(), "/supervision/sessions")
    suspend fun addSupervisionSession(supervisorName: String, credentials: String?, type: String, date: String, hours: Double,
                                      topics: String?, notes: String?, rating: Int?, asUser: Int? = null) {
        client.request("POST", "/supervision/sessions", buildJsonObject {
            put("supervisor_name", supervisorName); credentials?.let { put("supervisor_credentials", it) }
            put("supervision_type", type); put("session_date", date); put("hours", hours)
            topics?.let { put("topics", it) }; notes?.let { put("notes", it) }; rating?.let { put("rating", it) }
        }, asUser = asUser)
    }
    suspend fun deleteSupervisionSession(id: Int) { client.request("DELETE", "/supervision/sessions/$id") }
    suspend fun supervisors(): List<ApiSupervisor> = list(ApiSupervisor.serializer(), "/supervision/network")
    suspend fun addSupervisor(name: String, credentials: String?, specialization: String?, isOnline: Boolean, hourlyRate: Double?, email: String?) {
        client.request("POST", "/supervision/network", buildJsonObject {
            put("professional_name", name); credentials?.let { put("credentials", it) }; specialization?.let { put("specialization", it) }
            put("is_online", isOnline); hourlyRate?.let { put("hourly_rate", it) }; email?.let { put("contact_email", it) }
        })
    }

    // Directory & referrals -------------------------------------------------

    suspend fun myProfile(): ApiTherapistProfile = decode(client.request("GET", "/profiles/me").body)
    suspend fun therapistProfile(id: Int): ApiTherapistProfile = decode(client.request("GET", "/profiles/$id").body)
    suspend fun updateMyProfile(p: ApiTherapistProfile): ApiTherapistProfile = decode(client.request("PUT", "/profiles/me", buildJsonObject {
        p.headline?.let { put("headline", it) }; p.bio?.let { put("bio", it) }; p.yearsExperience?.let { put("years_experience", it) }
        p.city?.let { put("city", it) }; p.region?.let { put("region", it) }; p.country?.let { put("country", it) }
        put("offers_in_person", p.offersInPerson); put("offers_online", p.offersOnline); put("accepting_clients", p.acceptingClients)
        put("offers_supervision", p.offersSupervision); put("is_listed", p.isListed)
        listOf("specialty" to p.specialty, "population" to p.population, "language" to p.language, "approach" to p.approach).forEach { (kind, values) ->
            putJsonArray(kind) { values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
        }
    }).body)

    suspend fun directory(q: String? = null, city: String? = null, accepting: Boolean = false, online: Boolean = false): List<ApiTherapistProfile> {
        val params = listOfNotNull(q?.takeIf { it.isNotBlank() }?.let { "q=" + URLEncoder.encode(it, "UTF-8") },
            city?.takeIf { it.isNotBlank() }?.let { "city=" + URLEncoder.encode(it, "UTF-8") },
            if (accepting) "accepting=true" else null, if (online) "online=true" else null)
        return list(ApiTherapistProfile.serializer(), "/profiles/directory" + if (params.isEmpty()) "" else "?" + params.joinToString("&"))
    }

    suspend fun referrals(direction: String): List<ApiReferral> = list(ApiReferral.serializer(), "/referrals?direction=$direction")
    /** A description, not a record: no client identity travels with a referral. */
    suspend fun sendReferral(toTherapistId: Int, delivery: String, urgency: String, presentingIssue: String?, population: String?,
                             language: String?, city: String?, note: String?): ApiReferral =
        decode(client.request("POST", "/referrals", buildJsonObject {
            put("to_therapist_id", toTherapistId); put("delivery", delivery); put("urgency", urgency)
            presentingIssue?.let { put("presenting_issue", it) }; population?.let { put("population", it) }
            language?.let { put("language", it) }; city?.let { put("city", it) }; note?.let { put("note", it) }
        }).body)
    suspend fun respondToReferral(id: Int, accept: Boolean, note: String?) {
        client.request("PUT", "/referrals/$id/respond", buildJsonObject { put("accept", accept); note?.let { put("response_note", it) } })
    }
    suspend fun withdrawReferral(id: Int) { client.request("PUT", "/referrals/$id/withdraw", buildJsonObject {}) }

    // Intervision -------------------------------------------------------------

    suspend fun intervisionGroups(): List<ApiIntervisionGroup> = list(ApiIntervisionGroup.serializer(), "/intervision/groups")
    suspend fun myIntervisionGroups(): List<ApiIntervisionGroup> = list(ApiIntervisionGroup.serializer(), "/intervision/my-groups")
    suspend fun intervisionGroup(id: Int): ApiGroupDetail = decode(client.request("GET", "/intervision/groups/$id").body)
    suspend fun createIntervisionGroup(name: String, description: String?, focusArea: String?, schedule: String?, isOnline: Boolean, maxMembers: Int, link: String?): ApiCreated =
        decode(client.request("POST", "/intervision/groups", buildJsonObject {
            put("name", name); description?.let { put("description", it) }; focusArea?.let { put("focus_area", it) }
            schedule?.let { put("meeting_schedule", it) }; put("is_online", isOnline); put("max_members", maxMembers); link?.let { put("meeting_link", it) }
        }).body)
    suspend fun requestToJoin(groupId: Int, message: String?) {
        client.request("POST", "/intervision/join", buildJsonObject { put("group_id", groupId); message?.let { put("message", it) } })
    }
    suspend fun reviewJoinRequest(requestId: Int, approve: Boolean) {
        client.request("PUT", "/intervision/requests/$requestId", buildJsonObject { put("status", if (approve) "approved" else "rejected") })
    }
    suspend fun discussions(groupId: Int): List<ApiDiscussion> = list(ApiDiscussion.serializer(), "/intervision/groups/$groupId/discussions")
    suspend fun createDiscussion(groupId: Int, title: String): ApiCreated =
        decode(client.request("POST", "/intervision/discussions", buildJsonObject { put("group_id", groupId); put("title", title) }).body)
    suspend fun discussionMessages(discussionId: Int): List<ApiDiscussionMessage> = list(ApiDiscussionMessage.serializer(), "/intervision/discussions/$discussionId/messages")
    suspend fun postDiscussionMessage(discussionId: Int, content: String) {
        client.request("POST", "/intervision/discussions/$discussionId/messages", buildJsonObject { put("content", content) })
    }
    suspend fun createMeeting(groupId: Int, title: String, agenda: String?, link: String?, scheduledAt: String, minutes: Int) {
        client.request("POST", "/intervision/meetings", buildJsonObject {
            put("group_id", groupId); put("title", title); agenda?.let { put("agenda", it) }; link?.let { put("meeting_link", it) }
            put("scheduled_at", scheduledAt); put("duration_minutes", minutes)
        })
    }

    // Client page: goals, journal, toggles, edits -------------------------

    suspend fun goals(patientId: Int): List<ApiGoal> = list(ApiGoal.serializer(), "/goals?patient_id=$patientId")
    suspend fun createGoal(patientId: Int, title: String, description: String?, category: String, dueDate: String?) {
        client.request("POST", "/goals", buildJsonObject {
            put("patient_id", patientId); put("title", title); put("category", category)
            description?.let { put("description", it) }; dueDate?.let { put("due_date", it) }
        })
    }
    suspend fun setGoalCompleted(id: Int, completed: Boolean) { client.request("PUT", "/goals/$id", buildJsonObject { put("completed", completed) }) }
    suspend fun deleteGoal(id: Int) { client.request("DELETE", "/goals/$id") }

    /** For a therapist the server returns only entries the client chose to share. */
    suspend fun sharedJournal(patientId: Int): List<ApiJournalEntry> =
        list(ApiJournalEntry.serializer(), "/journal").filter { it.patientId == patientId }

    suspend fun clientFeatures(patientId: Int): Map<String, Boolean> {
        val obj = json.parseToJsonElement(client.request("GET", "/client-features/$patientId").body).jsonObject
        return obj.mapValues { (_, v) -> v.jsonPrimitive.let { it.booleanOrNull ?: (it.intOrNull == 1) } }
    }
    suspend fun setClientFeature(patientId: Int, feature: String, enabled: Boolean) {
        client.request("PUT", "/client-features/$patientId", buildJsonObject { put("feature_name", feature); put("enabled", enabled) })
    }
    suspend fun updatePatient(id: Int, diagnosis: String?, status: String?, riskLevel: String?) {
        client.request("PUT", "/patients/$id", buildJsonObject {
            diagnosis?.let { put("diagnosis", it) }; status?.let { put("status", it) }; riskLevel?.let { put("risk_level", it) }
        })
    }

    // AI drafting ---------------------------------------------------------
    // Every call names a client; the server checks plan (402) and the
    // client's ai_drafting consent (403 with a reason). Results are returned
    // as raw JSON objects: their shape is the model's, rendered generically.

    suspend fun draftNoteFromTranscript(patientId: Int, transcript: String): JsonObject =
        json.parseToJsonElement(client.request("POST", "/session-notes/generate", buildJsonObject { put("patient_id", patientId); put("transcript", transcript) }).body).jsonObject
    suspend fun summarizeSession(patientId: Int, notes: String): JsonObject =
        json.parseToJsonElement(client.request("POST", "/ai/summarize-session", buildJsonObject { put("patient_id", patientId); put("session_notes", notes) }).body).jsonObject
    suspend fun suggestInterventions(patientId: Int, concerns: String?): JsonObject =
        json.parseToJsonElement(client.request("POST", "/ai/suggest-interventions", buildJsonObject { put("patient_id", patientId); concerns?.let { put("current_concerns", it) } }).body).jsonObject
    suspend fun progressReport(patientId: Int): JsonObject =
        json.parseToJsonElement(client.request("POST", "/ai/generate-progress-report", buildJsonObject { put("patient_id", patientId) }).body).jsonObject

    // Community -----------------------------------------------------------

    suspend fun posts(sort: String = "recent"): List<ApiPost> = list(ApiPost.serializer(), "/posts?sort=$sort&limit=50")
    suspend fun post(id: Int): ApiPostDetail = decode(client.request("GET", "/posts/$id").body)
    suspend fun createPost(title: String, content: String, type: String, anonymous: Boolean) {
        client.request("POST", "/posts", buildJsonObject { put("title", title); put("content", content); put("post_type", type); put("is_anonymous", anonymous) })
    }
    suspend fun votePost(id: Int, value: Int) { client.request("POST", "/posts/$id/vote", buildJsonObject { put("vote_value", value) }) }
    suspend fun replyToPost(id: Int, content: String) { client.request("POST", "/posts/$id/reply", buildJsonObject { put("content", content) }) }
    suspend fun acceptAnswer(postId: Int, replyId: Int) { client.request("POST", "/posts/$postId/accept", buildJsonObject { put("reply_id", replyId) }) }

    private suspend fun <T> list(serializer: kotlinx.serialization.KSerializer<T>, path: String): List<T> =
        json.decodeFromString(ListSerializer(serializer), client.request("GET", path).body)

    enum class Instrument(val path: String, val displayName: String) {
        PHQ9("phq9", "PHQ-9"), GAD7("gad7", "GAD-7"), WAISR("wai-sr", "WAI-SR")
    }
}
