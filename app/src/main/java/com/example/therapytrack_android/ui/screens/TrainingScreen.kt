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

@Composable
fun TrainingScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Prof Dev", "Resources")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Training",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            0 -> ProfDevTab()
            1 -> ResourcesTab()
        }
    }
}

@Composable
fun ProfDevTab() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Upcoming Trainings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        items(listOf(
            TrainingItem("CBT Advanced Techniques", "March 15, 2026", "Online"),
            TrainingItem("EMDR Basic Training", "March 20, 2026", "Lisbon"),
            TrainingItem("Trauma-Informed Care", "March 25, 2026", "Online"),
            TrainingItem("Child Psychology Update", "April 1, 2026", "Online")
        )) { training ->
            TrainingCard(training)
        }
    }
}

@Composable
fun TrainingCard(training: TrainingItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = training.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = training.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = training.location,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

data class TrainingItem(val title: String, val date: String, val location: String)

@Composable
fun ResourcesTab() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Learning Resources",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(listOf(
            ResourceItem("Anxiety Treatment Guidelines", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("DSM-5 Quick Reference", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("CBT Techniques Handbook", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("EMDR Protocol", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("Therapy Notes Template", "DOCX", Icons.Filled.Description),
            ResourceItem("Consent Form Template", "DOCX", Icons.Filled.Description)
        )) { resource ->
            ResourceCard(resource)
        }
    }
}

@Composable
fun ResourceCard(resource: ResourceItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                resource.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = resource.type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

data class ResourceItem(val title: String, val type: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
