package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.data.model.Session
import com.example.therapytrack_android.ui.theme.*
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

enum class ScheduleViewMode { Day, Week, Month }

@Composable
fun ScheduleScreen(viewModel: TherapyViewModel) {
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.Week) }
    
    val daysOfWeek = remember { 
        (0..6).map { LocalDate.now().plusDays(it.toLong()) } 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PearlWhite)
                .padding(16.dp)
        ) {
            // View Mode Picker
            TabRow(
                selectedTabIndex = viewMode.ordinal,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                containerColor = SectionBackground,
                contentColor = MidnightNavy
            ) {
                ScheduleViewMode.entries.forEach { mode ->
                    Tab(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        text = { Text(mode.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Week Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
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
        }

        // Sessions for selected day
        val daySessions = viewModel.sessions.filter { 
            it.session_date.startsWith(selectedDate.toString()) 
        }

        Text(
            text = formatDateHeader(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        if (daySessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No sessions scheduled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                isSelected -> MidnightNavy
                isToday -> MidnightNavy.copy(alpha = 0.1f)
                else -> CardBackground
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(56.dp).height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfWeek.name.take(3),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isSelected -> PearlWhite
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected -> PearlWhite
                    else -> TextPrimary
                }
            )
            if (hasSession) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PearlWhite else MidnightNavy)
                )
            }
        }
    }
}

@Composable
fun ScheduleSessionCard(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Time indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = formatSessionTime(session.session_date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy
                )
                Text(
                    text = "${session.duration_minutes ?: 60} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MidnightNavy)
            )
            
            // Session details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = session.patient_name ?: "Patient",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                session.diagnosis?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                SessionTypeChip(type = "Individual")
            }
            
            // Status
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}

@Composable
fun SessionTypeChip(type: String) {
    Surface(
        color = DustyRose.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            color = DustyRose,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun formatDateHeader(date: LocalDate): String {
    return when {
        date == LocalDate.now() -> "Today"
        date == LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    }
}

fun formatSessionTime(isoString: String): String {
    return try {
        isoString.substringAfter("T").substringBefore("Z").take(5)
    } catch (e: Exception) {
        isoString
    }
}
