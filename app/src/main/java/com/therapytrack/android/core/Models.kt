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
