package com.example.therapytrack_android.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.therapytrack_android.data.api.ApiClient
import com.example.therapytrack_android.data.model.*
import kotlinx.coroutines.launch

class TherapyViewModel : ViewModel() {
    
    var patients by mutableStateOf<List<Patient>>(emptyList())
    var sessions by mutableStateOf<List<Session>>(emptyList())
    var messages by mutableStateOf<List<Message>>(emptyList())
    var supervisionSessions by mutableStateOf<List<SupervisionSession>>(emptyList())
    var supervisionNetwork by mutableStateOf<List<SupervisionNetworkMember>>(emptyList())
    var intervisionGroups by mutableStateOf<List<IntervisionGroup>>(emptyList())
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    
    // Current user (hardcoded for demo)
    val currentUserId = 1
    val authToken = ApiClient.TEST_TOKEN

    fun fetchPatients() {
        viewModelScope.launch {
            isLoading = true
            try {
                patients = ApiClient.api.getPatients("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                // Use mock data if API fails
                patients = getMockPatients()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchSessions() {
        viewModelScope.launch {
            isLoading = true
            try {
                sessions = ApiClient.api.getSessions("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                sessions = getMockSessions()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchMessages() {
        viewModelScope.launch {
            isLoading = true
            try {
                messages = ApiClient.api.getMessages("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                messages = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchSupervisionSessions() {
        viewModelScope.launch {
            isLoading = true
            try {
                supervisionSessions = ApiClient.api.getSupervisionSessions("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                supervisionSessions = getMockSupervisionSessions()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchSupervisionNetwork() {
        viewModelScope.launch {
            isLoading = true
            try {
                supervisionNetwork = ApiClient.api.getSupervisionNetwork("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                supervisionNetwork = getMockSupervisionNetwork()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchIntervisionGroups() {
        viewModelScope.launch {
            isLoading = true
            try {
                intervisionGroups = ApiClient.api.getIntervisionGroups("Bearer $authToken")
            } catch (e: Exception) {
                error = e.message
                intervisionGroups = getMockIntervisionGroups()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchAll() {
        fetchPatients()
        fetchSessions()
        fetchMessages()
        fetchSupervisionSessions()
        fetchSupervisionNetwork()
        fetchIntervisionGroups()
    }

    // Mock data for offline/testing
    private fun getMockPatients() = listOf(
        Patient(1, "John Anderson", "john@email.com", diagnosis = "Anxiety Disorder", status = "STABLE", risk_level = "LOW", session_count = 12),
        Patient(2, "Emma Wilson", "emma@email.com", diagnosis = "Depression", status = "IMPROVING", risk_level = "MEDIUM", session_count = 8),
        Patient(3, "Michael Chen", "michael@email.com", diagnosis = "PTSD", status = "AT RISK", risk_level = "HIGH", session_count = 15),
        Patient(4, "Sarah Davis", "sarah@email.com", diagnosis = "Bipolar", status = "STABLE", risk_level = "LOW", session_count = 20),
        Patient(5, "Robert Martinez", "robert@email.com", diagnosis = "OCD", status = "IMPROVING", risk_level = "LOW", session_count = 6),
        Patient(6, "Lisa Thompson", "lisa@email.com", diagnosis = "GAD", status = "STABLE", risk_level = "LOW", session_count = 10)
    )

    private fun getMockSessions() = listOf(
        Session(1, 1, "John Anderson", "2026-03-09T10:00:00Z", 60, "scheduled"),
        Session(2, 2, "Emma Wilson", "2026-03-09T14:00:00Z", 60, "scheduled"),
        Session(3, 3, "Michael Chen", "2026-03-10T09:00:00Z", 60, "scheduled"),
        Session(4, 4, "Sarah Davis", "2026-03-10T11:00:00Z", 60, "scheduled"),
        Session(5, 5, "Robert Martinez", "2026-03-11T15:00:00Z", 60, "scheduled"),
        Session(6, 6, "Lisa Thompson", "2026-03-11T16:30:00Z", 60, "scheduled")
    )

    private fun getMockSupervisionSessions() = listOf(
        SupervisionSession(1, 1, "Dr. Maria Santos", "PhD", "individual", "2026-03-01", 1.0, "Anxiety treatment", "Great session", 5),
        SupervisionSession(2, 1, "Dr. Pedro Almeida", "PhD", "individual", "2026-02-25", 1.0, "Trauma therapy", "EMDR review", 5),
        SupervisionSession(3, 1, "Peer Supervision Group", "Weekly", "group", "2026-02-28", 2.0, "Complex cases", "Group discussion", 4),
        SupervisionSession(4, 1, "Dr. Ana Rodrigues", "PhD", "peer", "2026-02-20", 1.0, "Assessment tools", "Shared resources", 4)
    )

    private fun getMockSupervisionNetwork() = listOf(
        SupervisionNetworkMember(1, 1, "Dr. Maria Santos", "PhD", "Anxiety & Depression", true, 80, "maria@clinic.pt", 4.8, 12),
        SupervisionNetworkMember(2, 1, "Dr. Pedro Almeida", "PhD", "Trauma & PTSD", true, 90, "pedro@clinic.pt", 4.9, 8),
        SupervisionNetworkMember(3, 1, "Dr. Ana Rodrigues", "PhD", "Child & Adolescent", false, 100, "ana@clinic.pt", 4.7, 15),
        SupervisionNetworkMember(4, 1, "Dr. João Ferreira", "Master's", "Cognitive Behavioral", true, 70, "joao@clinic.pt", 4.5, 6),
        SupervisionNetworkMember(5, 1, "Dr. Sofia Costa", "PhD", "Eating Disorders", true, 85, "sofia@clinic.pt", 4.6, 9)
    )

    private fun getMockIntervisionGroups() = listOf(
        IntervisionGroup(1, "Anxiety Specialists Portugal", "Peer consultation for anxiety treatment specialists", "Anxiety Disorders", "Tuesday 7pm", true, 12, 1, "Dr. Sarah Smith", "https://meet.google.com/abc", true, "0"),
        IntervisionGroup(2, "Trauma Therapists Network", "EMDR and trauma therapy case discussion", "Trauma & PTSD", "Wednesday 8pm", true, 10, 1, "Dr. Sarah Smith", "https://zoom.us/123", true, "0"),
        IntervisionGroup(3, "Child Psychology Circle", "Supporting therapists working with children", "Child & Adolescent", "Thursday 6pm", true, 8, 1, "Dr. Sarah Smith", "https://meet.google.com/xyz", true, "0")
    )
}
