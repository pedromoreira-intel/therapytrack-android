package com.example.therapytrack_android.data.model

import com.google.gson.annotations.SerializedName

data class Patient(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val diagnosis: String? = null,
    val status: String = "STABLE",
    val risk_level: String? = null,
    val session_count: Int = 0,
    val progress_score: Double? = null,
    val next_session: String? = null,
    val last_ema: String? = null,
    val pending_messages: Int = 0
)

data class Session(
    val id: Int,
    val patient_id: Int,
    val patient_name: String? = null,
    val session_date: String,
    val duration_minutes: Int? = null,
    val status: String = "scheduled",
    val notes: String? = null,
    val diagnosis: String? = null,
    val patient_status: String? = null,
    val risk_level: String? = null
)

data class Message(
    val id: Int,
    @SerializedName("from_user_id") val fromUserId: Int,
    @SerializedName("to_user_id") val toUserId: Int,
    @SerializedName("from_name") val fromName: String? = null,
    @SerializedName("to_name") val toName: String? = null,
    val message: String,
    val read: Boolean? = null,
    @SerializedName("created_at") val createdAt: String
)

data class SupervisionSession(
    val id: Int,
    @SerializedName("therapist_id") val therapistId: Int,
    @SerializedName("supervisor_name") val supervisorName: String,
    @SerializedName("supervisor_credentials") val supervisorCredentials: String? = null,
    @SerializedName("supervision_type") val supervisionType: String,
    @SerializedName("session_date") val sessionDate: String,
    val hours: Double,
    val topics: String? = null,
    val notes: String? = null,
    val rating: Int = 0
)

data class SupervisionNetworkMember(
    val id: Int,
    @SerializedName("therapist_id") val therapistId: Int,
    @SerializedName("professional_name") val professionalName: String,
    val credentials: String? = null,
    val specialization: String? = null,
    @SerializedName("is_online") val isOnline: Boolean = true,
    @SerializedName("hourly_rate") val hourlyRate: Int? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    val rating: Double? = null,
    @SerializedName("review_count") val reviewCount: Int = 0
)

data class IntervisionGroup(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("focus_area") val focusArea: String? = null,
    @SerializedName("meeting_schedule") val meetingSchedule: String? = null,
    @SerializedName("is_online") val isOnline: Boolean = true,
    @SerializedName("max_members") val maxMembers: Int = 0,
    @SerializedName("moderator_id") val moderatorId: Int,
    @SerializedName("moderator_name") val moderatorName: String? = null,
    @SerializedName("meeting_link") val meetingLink: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("member_count") val memberCount: String? = null
)

data class Discussion(
    val id: Int,
    @SerializedName("group_id") val groupId: Int,
    val title: String,
    @SerializedName("created_by") val createdBy: Int,
    @SerializedName("creator_name") val creatorName: String? = null,
    @SerializedName("message_count") val messageCount: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class GroupMeeting(
    val id: Int,
    @SerializedName("group_id") val groupId: Int,
    val title: String,
    val agenda: String? = null,
    @SerializedName("meeting_link") val meetingLink: String? = null,
    @SerializedName("scheduled_at") val scheduledAt: String,
    @SerializedName("duration_minutes") val durationMinutes: Int = 60
)

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val role: String
)
