package com.example.therapytrack_android.data.api

import com.example.therapytrack_android.data.model.*
import retrofit2.http.*

interface ApiService {
    // Patients
    @GET("patients")
    suspend fun getPatients(@Header("Authorization") token: String): List<Patient>

    // Sessions
    @GET("sessions")
    suspend fun getSessions(@Header("Authorization") token: String): List<Session>

    // Messages
    @GET("messages")
    suspend fun getMessages(@Header("Authorization") token: String): List<Message>

    // Supervision
    @GET("supervision/sessions")
    suspend fun getSupervisionSessions(@Header("Authorization") token: String): List<SupervisionSession>

    @GET("supervision/network")
    suspend fun getSupervisionNetwork(@Header("Authorization") token: String): List<SupervisionNetworkMember>

    // Intervision
    @GET("intervision/groups")
    suspend fun getIntervisionGroups(@Header("Authorization") token: String): List<IntervisionGroup>

    @GET("intervision/groups/{id}/discussions")
    suspend fun getGroupDiscussions(
        @Header("Authorization") token: String,
        @Path("id") groupId: Int
    ): List<Discussion>

    @GET("intervision/meetings")
    suspend fun getGroupMeetings(
        @Header("Authorization") token: String,
        @Query("group_id") groupId: Int
    ): List<GroupMeeting>

    // Auth
    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any>
}
