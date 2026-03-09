package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.therapytrack_android.data.model.Session
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(viewModel: TherapyViewModel = viewModel()) {
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val daysOfWeek = remember { 
        (0..6).map { LocalDate.now().plusDays(it.toLong()) } 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Week selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(daysOfWeek) { date ->
                DayCard(
                    date = date,
                    isSelected = date == selectedDate,
                    hasSession = viewModel.sessions.any { 
                        it.session_date.startsWith(date.toString()) 
                    },
                    onClick = { selectedDate = date }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sessions for selected day
        val daySessions = viewModel.sessions.filter { 
            it.session_date.startsWith(selectedDate.toString()) 
        }

        Text(
            text = formatDateHeader(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (daySessions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No sessions scheduled",
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daySessions) { session ->
                    ScheduleSessionCard(session = session)
                }
            }
        }
    }
}

@Composable
fun DayCard(
    date: LocalDate,
    isSelected: Boolean,
    hasSession: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.width(60.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfWeek.name.take(3),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurface
            )
            if (hasSession) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .padding(2.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.primary
                    ) {}
                }
            }
        }
    }
}

@Composable
fun ScheduleSessionCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = session.patient_name ?: "Patient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatSessionTime(session.session_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                session.diagnosis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.duration_minutes ?: 60} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(status = session.status, riskLevel = null)
            }
        }
    }
}

fun formatDateHeader(date: LocalDate): String {
    return when {
        date == LocalDate.now() -> "Today"
        date == LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
}

fun formatSessionTime(isoString: String): String {
    return try {
        isoString.substringAfter("T").substringBefore("Z").take(5)
    } catch (e: Exception) {
        isoString
    }
}
