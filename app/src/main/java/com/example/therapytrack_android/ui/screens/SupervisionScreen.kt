package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.therapytrack_android.data.model.IntervisionGroup
import com.example.therapytrack_android.data.model.SupervisionNetworkMember
import com.example.therapytrack_android.data.model.SupervisionSession
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel

@Composable
fun SupervisionScreen(viewModel: TherapyViewModel = viewModel()) {
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sessions", "Network", "Intervision")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Supervision",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> SupervisionSessionsTab(viewModel)
            1 -> SupervisionNetworkTab(viewModel)
            2 -> IntervisionGroupsTab(viewModel)
        }
    }
}

@Composable
fun SupervisionSessionsTab(viewModel: TherapyViewModel) {
    if (viewModel.supervisionSessions.isEmpty()) {
        EmptyState(message = "No supervision sessions yet")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total hours card
            item {
                val totalHours = viewModel.supervisionSessions.sumOf { it.hours }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                                text = "Total Supervision Hours",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${viewModel.supervisionSessions.size} sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "${totalHours}h",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(viewModel.supervisionSessions) { session ->
                SupervisionSessionCard(session)
            }
        }
    }
}

@Composable
fun SupervisionSessionCard(session: SupervisionSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.supervisorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                RatingStars(rating = session.rating)
            }
            Text(
                text = session.supervisionType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatSessionDate(session.sessionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            session.topics?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RatingStars(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = null,
                tint = if (index < rating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SupervisionNetworkTab(viewModel: TherapyViewModel) {
    if (viewModel.supervisionNetwork.isEmpty()) {
        EmptyState(message = "No supervisors in network")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.supervisionNetwork) { member ->
                SupervisorCard(member)
            }
        }
    }
}

@Composable
fun SupervisorCard(member: SupervisionNetworkMember) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.professionalName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${member.credentials ?: ""} • ${member.specialization ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    member.rating?.let {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " ${it} (${member.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                member.hourlyRate?.let {
                    Text(
                        text = "€$it/hr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (member.isOnline) {
                    Text(
                        text = "Online",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun IntervisionGroupsTab(viewModel: TherapyViewModel) {
    if (viewModel.intervisionGroups.isEmpty()) {
        EmptyState(message = "No intervision groups")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.intervisionGroups) { group ->
                IntervisionGroupCard(group)
            }
        }
    }
}

@Composable
fun IntervisionGroupCard(group: IntervisionGroup) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            group.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                group.focusArea?.let {
                    InfoChip(icon = Icons.Filled.Category, text = it)
                }
                group.meetingSchedule?.let {
                    InfoChip(icon = Icons.Filled.Schedule, text = it)
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
