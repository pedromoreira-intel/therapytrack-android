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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.data.model.IntervisionGroup
import com.example.therapytrack_android.data.model.SupervisionSession
import com.example.therapytrack_android.ui.theme.*
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel

@Composable
fun NetworkingScreen(viewModel: TherapyViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Supervision", "Intervision")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground)
    ) {
        // Segmented Picker - matching iOS exactly
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            containerColor = CardBackground,
            contentColor = MidnightNavy
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> SupervisionTabContent(viewModel)
            1 -> IntervisionTabContent(viewModel)
        }
    }
}

@Composable
fun SupervisionTabContent(viewModel: TherapyViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchSupervisionSessions()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Stats
        item {
            val totalHours = viewModel.supervisionSessions.sumOf { it.hours }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatCard(
                    title = "Hours",
                    value = String.format("%.1f", totalHours),
                    icon = Icons.Filled.Schedule,
                    color = MidnightNavy,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    title = "Sessions",
                    value = "${viewModel.supervisionSessions.size}",
                    icon = Icons.Filled.People,
                    color = Champagne,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Sessions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = { }) {
                    Text("Add", color = MidnightNavy)
                }
            }
        }

        // Sessions
        items(viewModel.supervisionSessions.take(5)) { session ->
            SupervisionSessionCard(session)
        }

        // Bottom spacing for tab bar
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun IntervisionTabContent(viewModel: TherapyViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchIntervisionGroups()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Intervision Groups Header
        item {
            Text(
                text = "Your Groups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Groups
        items(viewModel.intervisionGroups) { group ->
            IntervisionGroupCard(group)
        }

        // Empty state if no groups
        if (viewModel.intervisionGroups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Groups,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No intervision groups yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Text(
                            text = "Join or create a group to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Bottom spacing for tab bar
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SupervisionSessionCard(session: SupervisionSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = session.supervisorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = session.supervisionType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MidnightNavy
                )
                Text(
                    text = formatSessionDate(session.sessionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.hours}h",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy
                )
                RatingStars(rating = session.rating)
            }
        }
    }
}

@Composable
fun RatingStars(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                if (index < rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = null,
                tint = if (index < rating) Champagne else TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun IntervisionGroupCard(group: IntervisionGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (group.isOnline) {
                    Surface(color = Success.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            group.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                group.focusArea?.let { InfoChip(icon = Icons.Filled.Category, text = it) }
                group.meetingSchedule?.let { InfoChip(icon = Icons.Filled.Schedule, text = it) }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MidnightNavy)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MidnightNavy)
    }
}
