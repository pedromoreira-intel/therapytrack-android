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

// Plan, professional records, supervision -------------------------------

@Serializable
data class ApiPlan(
    val plan: String = "free",
    val status: String = "active",
    @SerialName("current_period_end") val currentPeriodEnd: String? = null,
    val source: String? = null,
    @SerialName("effective_reason") val effectiveReason: String? = null,
    val features: List<String> = emptyList(),
    @SerialName("free_client_limit") val freeClientLimit: Int? = null
) {
    fun has(feature: String) = feature in features
}

@Serializable
data class ApiCredential(
    val id: Int,
    @SerialName("license_type") val licenseType: String,
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("issuing_body") val issuingBody: String? = null,
    val region: String? = null,
    @SerialName("expires_on") val expiresOn: String? = null,
    val notes: String? = null,
    /** active · renewal_due (≤90 days) · expiring_soon (≤30) · expired — computed by the server. */
    val status: String = "active",
    @SerialName("days_until_expiry") val daysUntilExpiry: Int? = null
)

@Serializable
data class ApiTraining(
    val id: Int,
    val title: String,
    val institution: String? = null,
    @SerialName("training_type") val trainingType: String,
    @SerialName("completed_on") val completedOn: String,
    val hours: Double = 0.0,
    @SerialName("certificate_url") val certificateUrl: String? = null,
    val notes: String? = null
)

@Serializable
data class ApiHours(val total: Double = 0.0, val individual: Double = 0.0, val group: Double = 0.0, val peer: Double = 0.0,
                    @SerialName("session_count") val sessionCount: Int = 0, @SerialName("record_count") val recordCount: Int = 0)

@Serializable
data class ApiCredentialSummary(val total: Int = 0, val expired: List<ApiCredential> = emptyList(),
                                @SerialName("expiring_soon") val expiringSoon: List<ApiCredential> = emptyList(),
                                @SerialName("renewal_due") val renewalDue: List<ApiCredential> = emptyList())

@Serializable
data class ApiProfessionalSummary(
    val year: Int,
    @SerialName("supervision_hours") val supervisionHours: ApiHours = ApiHours(),
    @SerialName("training_hours") val trainingHours: ApiHours = ApiHours(),
    val credentials: ApiCredentialSummary = ApiCredentialSummary()
)

@Serializable
data class ApiSupervisionSession(
    val id: Int,
    @SerialName("supervisor_name") val supervisorName: String,
    @SerialName("supervisor_credentials") val supervisorCredentials: String? = null,
    @SerialName("supervision_type") val supervisionType: String,
    @SerialName("session_date") val sessionDate: String,
    val hours: Double = 1.0,
    val topics: String? = null,
    val notes: String? = null,
    val rating: Int? = null
)

@Serializable
data class ApiSupervisor(
    val id: Int,
    @SerialName("professional_name") val professionalName: String,
    val credentials: String? = null,
    val specialization: String? = null,
    @SerialName("is_online") @Serializable(with = LenientBoolean::class) val isOnline: Boolean = true,
    @SerialName("hourly_rate") val hourlyRate: Double? = null,
    @SerialName("contact_email") val contactEmail: String? = null
)

// Directory & referrals ----------------------------------------------------

@Serializable
data class ApiTherapistProfile(
    @SerialName("therapist_id") val therapistId: Int,
    val name: String = "",
    val headline: String? = null,
    val bio: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    @SerialName("offers_in_person") @Serializable(with = LenientBoolean::class) val offersInPerson: Boolean = false,
    @SerialName("offers_online") @Serializable(with = LenientBoolean::class) val offersOnline: Boolean = false,
    @SerialName("accepting_clients") @Serializable(with = LenientBoolean::class) val acceptingClients: Boolean = false,
    @SerialName("offers_supervision") @Serializable(with = LenientBoolean::class) val offersSupervision: Boolean = false,
    @SerialName("is_listed") @Serializable(with = LenientBoolean::class) val isListed: Boolean = false,
    val specialty: List<String> = emptyList(),
    val population: List<String> = emptyList(),
    val language: List<String> = emptyList(),
    val approach: List<String> = emptyList()
)

@Serializable
data class ApiReferral(
    val id: Int,
    val direction: String,
    @SerialName("counterparty_id") val counterpartyId: Int,
    @SerialName("counterparty_name") val counterpartyName: String? = null,
    @SerialName("counterparty_email") val counterpartyEmail: String? = null,
    @SerialName("presenting_issue") val presentingIssue: String? = null,
    val population: String? = null,
    val language: String? = null,
    val city: String? = null,
    val delivery: String = "either",
    val urgency: String = "routine",
    val note: String? = null,
    val status: String = "pending",
    @SerialName("response_note") val responseNote: String? = null,
    @SerialName("responded_at") val respondedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

// Intervision ---------------------------------------------------------------

@Serializable
data class ApiIntervisionGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("focus_area") val focusArea: String? = null,
    @SerialName("meeting_schedule") val meetingSchedule: String? = null,
    @SerialName("is_online") @Serializable(with = LenientBoolean::class) val isOnline: Boolean = true,
    @SerialName("max_members") val maxMembers: Int = 10,
    @SerialName("moderator_id") val moderatorId: Int,
    @SerialName("moderator_name") val moderatorName: String? = null,
    @SerialName("meeting_link") val meetingLink: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    /** Present on `/my-groups`: this user's role in the group. */
    val role: String? = null
)

@Serializable
data class ApiGroupMember(val id: Int, @SerialName("user_id") val userId: Int, val role: String = "member",
                          @SerialName("user_name") val userName: String? = null)

@Serializable
data class ApiJoinRequest(val id: Int, @SerialName("user_id") val userId: Int, @SerialName("user_name") val userName: String? = null,
                          val message: String? = null, val status: String = "pending", @SerialName("requested_at") val requestedAt: String? = null)

@Serializable
data class ApiMeeting(val id: Int, val title: String, val agenda: String? = null, @SerialName("meeting_link") val meetingLink: String? = null,
                      @SerialName("scheduled_at") val scheduledAt: String, @SerialName("duration_minutes") val durationMinutes: Int = 60)

@Serializable
data class ApiGroupDetail(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("focus_area") val focusArea: String? = null,
    @SerialName("meeting_schedule") val meetingSchedule: String? = null,
    @SerialName("is_online") @Serializable(with = LenientBoolean::class) val isOnline: Boolean = true,
    @SerialName("max_members") val maxMembers: Int = 10,
    @SerialName("moderator_id") val moderatorId: Int,
    @SerialName("moderator_name") val moderatorName: String? = null,
    @SerialName("meeting_link") val meetingLink: String? = null,
    val members: List<ApiGroupMember> = emptyList(),
    /** Only sent to the moderator. */
    val requests: List<ApiJoinRequest> = emptyList(),
    val meetings: List<ApiMeeting> = emptyList(),
    @SerialName("my_role") val myRole: String = "member"
)

@Serializable
data class ApiDiscussion(val id: Int, @SerialName("group_id") val groupId: Int, val title: String,
                         @SerialName("created_by") val createdBy: Int, @SerialName("creator_name") val creatorName: String? = null,
                         @SerialName("created_at") val createdAt: String, @SerialName("message_count") val messageCount: Int = 0)

@Serializable
data class ApiDiscussionMessage(val id: Int, @SerialName("user_id") val userId: Int, @SerialName("user_name") val userName: String? = null,
                                val content: String, @SerialName("posted_at") val postedAt: String)

@Serializable
data class ApiCreated(val id: Int, val message: String? = null)
