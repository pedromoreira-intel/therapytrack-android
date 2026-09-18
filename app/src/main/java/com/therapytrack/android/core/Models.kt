package com.therapytrack.android.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/** `role` and `email` are absent when the user is the other party of a conversation. */
data class ApiUser(val id: Int, val name: String, val email: String = "", val role: String = "")

@Serializable
data class LoginResponse(val token: String, @SerialName("refresh_token") val refreshToken: String? = null, val user: ApiUser)

@Serializable
data class ApiPatient(
    val id: Int,
    /** The client's user account, which is what a message is addressed to. */
    @SerialName("user_id") val userId: Int? = null,
    val name: String,
    val email: String = "",
    val diagnosis: String? = null,
    val status: String = "active",
    @SerialName("risk_level") val riskLevel: String = "low",
    @SerialName("session_count") val sessionCount: Int? = null,
    @SerialName("last_ema") val lastEma: String? = null,
    @SerialName("next_session") val nextSession: String? = null,
    @SerialName("therapist_id") val therapistId: Int? = null,
    @SerialName("therapist_name") val therapistName: String? = null
)

@Serializable
data class ApiEmaResponse(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("mood_score") val mood: Int? = null,
    @SerialName("anxiety_score") val anxiety: Int? = null,
    @SerialName("sleep_score") val sleep: Int? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiAssessmentResult(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    /** Integer for PHQ-9/GAD-7; a 1–5 mean for WAI-SR. */
    val score: Double,
    val severity: String? = null,
    @SerialName("assessment_date") val assessmentDate: String,
    @SerialName("suicidal_ideation_flagged") @Serializable(with = LenientBoolean::class) val suicidalIdeationFlagged: Boolean = false
)

@Serializable
data class ApiJournalEntry(
    val id: Int,
    val title: String? = null,
    val content: String,
    @SerialName("is_private") @Serializable(with = LenientBoolean::class) val isPrivate: Boolean = true,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiMessage(
    val id: Int,
    @SerialName("from_user_id") val fromUserId: Int,
    @SerialName("to_user_id") val toUserId: Int,
    @SerialName("from_name") val fromName: String? = null,
    val message: String,
    @Serializable(with = LenientBoolean::class) val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @Serializable(with = LenientBoolean::class) val duplicate: Boolean = false
)

@Serializable
data class ApiConversation(val messages: List<ApiMessage>, @SerialName("other_user") val otherUser: ApiUser, @SerialName("unread_count") val unreadCount: Int = 0)

@Serializable
data class ApiInviteLookup(val valid: Boolean, val name: String, val email: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class ConsentEntry(
    @Serializable(with = LenientBoolean::class) val granted: Boolean,
    @SerialName("document_version") val documentVersion: String,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("current_document") @Serializable(with = LenientBoolean::class) val currentDocument: Boolean
)

@Serializable
data class ConsentState(
    @SerialName("document_version") val documentVersion: String,
    val purposes: Map<String, String> = emptyMap(),
    val consent: Map<String, ConsentEntry> = emptyMap(),
    @SerialName("history_count") val historyCount: Int = 0
)

@Serializable
data class ErasurePreview(val erased: List<String>, val retained: List<String>, val why: String, val retention: String? = null)

@Serializable
data class ErasureOutcome(
    val erased: Boolean? = null,
    val outcome: String? = null,
    val detail: String? = null,
    val retention: String? = null,
    @SerialName("what_will_happen") val whatWillHappen: ErasurePreview? = null,
    val reason: String? = null
)

// Therapist side ---------------------------------------------------------

@Serializable
data class ApiSession(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("session_date") val sessionDate: String,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val status: String = "scheduled",
    val notes: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null
)

@Serializable
data class ApiSessionNote(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("session_number") val sessionNumber: Int,
    @SerialName("session_date") val sessionDate: String,
    val focus: String? = null,
    val interventions: String = "",
    @SerialName("progress_notes") val progressNotes: String = "",
    val homework: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("next_session_plan") val nextSessionPlan: String? = null
)

/** `POST /session-notes` replies with the id and assigned number only. */
@Serializable
data class ApiSessionNoteCreated(
    val id: Int? = null,
    @SerialName("session_number") val sessionNumber: Int,
    @Serializable(with = LenientBoolean::class) val duplicate: Boolean = false
)

@Serializable
data class ApiClinicalAlert(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("alert_type") val alertType: String,
    val severity: String,
    val source: String = "",
    val detail: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("acknowledged_at") val acknowledgedAt: String? = null
) {
    val isCritical get() = severity == "CRITICAL"
}

@Serializable
data class ApiThread(
    val id: Int,
    @SerialName("other_user_id") val otherUserId: Int,
    @SerialName("other_user_name") val otherUserName: String? = null,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("unread_count") val unreadCount: Int = 0
)

@Serializable
data class ApiThreads(val threads: List<ApiThread>, @SerialName("total_unread") val totalUnread: Int = 0)

@Serializable
data class ApiInviteIssued(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("invite_expires_at") val inviteExpiresAt: String,
    val name: String? = null,
    val email: String? = null
)

/** A newly created client plus the invitation their therapist has to pass on. */
data class CreatedPatient(val patient: ApiPatient, val inviteCode: String?, val inviteExpiresAt: String?)

// Pre-session brief

@Serializable
data class BriefLastSession(
    val date: String? = null,
    @SerialName("session_number") val sessionNumber: Int? = null,
    val focus: String? = null,
    @SerialName("homework_set") val homeworkSet: String? = null,
    val plan: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null
)

@Serializable
data class BriefReading(val score: Double, val severity: String? = null, val date: String? = null)

@Serializable
data class BriefChange(val delta: Double = 0.0, val direction: String = "unchanged", @Serializable(with = LenientBoolean::class) val notable: Boolean = false)

@Serializable
data class BriefMeasure(
    @SerialName("has_data") @Serializable(with = LenientBoolean::class) val hasData: Boolean = false,
    val latest: BriefReading? = null,
    val previous: BriefReading? = null,
    val change: BriefChange? = null
)

@Serializable
data class BriefConcern(val type: String, val instrument: String = "", val severity: String = "", val detail: String = "",
                        @Serializable(with = LenientBoolean::class) val provisional: Boolean = false)

@Serializable
data class BriefCheckIns(val count: Int = 0, @SerialName("mood_avg") val moodAvg: Double? = null,
                         @SerialName("anxiety_avg") val anxietyAvg: Double? = null, @SerialName("sleep_avg") val sleepAvg: Double? = null,
                         val latest: String? = null)

@Serializable
data class BriefHomeworkItem(val id: Int, val title: String, val category: String? = null, @SerialName("due_date") val dueDate: String? = null)

@Serializable
data class BriefHomework(@SerialName("outstanding_count") val outstandingCount: Int = 0, val outstanding: List<BriefHomeworkItem> = emptyList(),
                         @SerialName("completed_total") val completedTotal: Int = 0)

@Serializable
data class BriefJournalLatest(val date: String? = null, val title: String? = null, val excerpt: String = "")

@Serializable
data class BriefJournal(@SerialName("shared_count") val sharedCount: Int = 0, val latest: BriefJournalLatest? = null)

@Serializable
data class ApiBrief(
    val patient: ApiPatient,
    @SerialName("last_session") val lastSession: BriefLastSession? = null,
    val since: String = "",
    @SerialName("open_alerts") val openAlerts: List<ApiClinicalAlert> = emptyList(),
    val trajectory: List<BriefConcern> = emptyList(),
    val measures: Map<String, BriefMeasure> = emptyMap(),
    @SerialName("check_ins") val checkIns: BriefCheckIns = BriefCheckIns(),
    val homework: BriefHomework = BriefHomework(),
    val journal: BriefJournal = BriefJournal()
)
