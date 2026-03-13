package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.data.model.Patient
import com.example.therapytrack_android.data.model.Session
import com.example.therapytrack_android.ui.theme.*
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun HomeScreen(
    viewModel: TherapyViewModel,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToPatients: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToNetworking: () -> Unit = {},
    onNavigateToTraining: () -> Unit = {}
) {
    
    LaunchedEffect(Unit) {
        if (viewModel.patients.isEmpty()) {
            viewModel.fetchAll()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header with gradient
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MidnightNavy, Color(0xFF243B71))
                        )
                    )
                    .padding(24.dp)
            ) {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PearlWhite
                )
                Text(
                    text = getTodayDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PearlWhite.copy(alpha = 0.65f)
                )
            }
        }

        // Quick Actions
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.CalendarMonth,
                        label = "Schedule",
                        color = MidnightNavy,
                        onClick = onNavigateToSchedule,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.People,
                        label = "Patients",
                        color = Color(0xFFD4A84B),
                        onClick = onNavigateToPatients,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Message,
                        label = "Messages",
                        color = DustyRose,
                        onClick = onNavigateToMessages,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.SupervisorAccount,
                        label = "Networking",
                        color = Champagne,
                        onClick = onNavigateToNetworking,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.School,
                        label = "Training",
                        color = Success,
                        onClick = onNavigateToTraining,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Stats
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Patients",
                    value = "${viewModel.patients.size}",
                    color = MidnightNavy,
                    onClick = onNavigateToPatients,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Sessions",
                    value = "${viewModel.sessions.size}",
                    color = DustyRose,
                    onClick = onNavigateToSchedule,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Supervisions",
                    value = "${viewModel.supervisionSessions.size}",
                    color = Champagne,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Upcoming Events
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { onNavigateToSchedule() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See full schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MidnightNavy
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MidnightNavy,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        items(viewModel.sessions.take(3)) { session ->
            SessionCard(session = session, onClick = onNavigateToSchedule)
        }

        // Recent Patients
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { onNavigateToPatients() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Patients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall,
                            color = MidnightNavy
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MidnightNavy,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        items(viewModel.patients.take(4)) { patient ->
            PatientCard(patient = patient, onClick = onNavigateToPatients)
        }
        
        // Bottom spacing for navigation bar
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.patient_name ?: "Patient",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = formatSessionDate(session.session_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.duration_minutes ?: 60} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MidnightNavy
                )
                Text(
                    text = session.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Success
                )
            }
        }
    }
}

@Composable
fun PatientCard(patient: Patient, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = patient.diagnosis ?: "No diagnosis",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            StatusChip(status = patient.status, riskLevel = patient.risk_level)
        }
    }
}

@Composable
fun StatusChip(status: String, riskLevel: String?) {
    val color = when (riskLevel) {
        "HIGH" -> Critical
        "MEDIUM" -> Warning
        else -> Success
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun getTodayDate(): String {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val day = today.dayOfMonth
    val month = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$dayName, $day $month"
}

fun formatSessionDate(isoString: String): String {
    return try {
        val date = LocalDate.parse(isoString.substringBefore("T"))
        val time = isoString.substringAfter("T").substringBefore("Z").take(5)
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$dayName, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth} at $time"
    } catch (e: Exception) {
        isoString
    }
}
