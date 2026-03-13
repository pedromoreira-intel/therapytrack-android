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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.ui.theme.*

@Composable
fun TrainingScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Prof Dev", "Resources")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground)
    ) {
        // Tab Selector - matching iOS
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
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> ProfDevTab()
            1 -> ResourcesTab()
        }
    }
}

@Composable
fun ProfDevTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Trainings",
                    value = "4",
                    icon = Icons.Filled.School,
                    color = MidnightNavy,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Credentials",
                    value = "2",
                    icon = Icons.Filled.Verified,
                    color = Champagne,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Credits",
                    value = "24",
                    icon = Icons.Filled.Stars,
                    color = DustyRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Upcoming Trainings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        items(listOf(
            TrainingItem("CBT Advanced Techniques", "March 15, 2026", "Online", "Certificate"),
            TrainingItem("EMDR Basic Training", "March 20, 2026", "Lisbon", "Certification"),
            TrainingItem("Trauma-Informed Care", "March 25, 2026", "Online", "Workshop")
        )) { training ->
            TrainingCard(training)
        }

        item {
            Text(
                text = "Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(listOf(
            CredentialItem("Psychologist", "12345", "Portugal", "Active", Icons.Filled.Badge),
            CredentialItem("EMDR Practitioner", "EMDR-001", "Europe", "Active", Icons.Filled.VerifiedUser)
        )) { credential ->
            CredentialCard(credential)
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
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
fun TrainingCard(training: TrainingItem) {
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
                    text = training.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = training.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = MidnightNavy.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = training.location,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MidnightNavy
                )
            }
        }
    }
}

data class TrainingItem(val title: String, val date: String, val location: String, val type: String)

@Composable
fun CredentialCard(credential: CredentialItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MidnightNavy.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(credential.icon, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = credential.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "${credential.number} • ${credential.state}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = Success.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = credential.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Success
                )
            }
        }
    }
}

data class CredentialItem(
    val title: String,
    val number: String,
    val state: String,
    val status: String,
    val icon: ImageVector
)

@Composable
fun ResourcesTab() {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Today", "Research", "Conferences", "Resources")

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tab picker - matching iOS exactly
        TabRow(
            selectedTabIndex = selectedSubTab,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = CardBackground,
            contentColor = MidnightNavy
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedSubTab) {
            0 -> TodayTipSection()
            1 -> ResearchSection()
            2 -> ConferencesSection()
            3 -> ResourcesLibrarySection()
        }
    }
}

@Composable
fun TodayTipSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Tip Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Champagne.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFE6B800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clinical Tip of the Day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "When working with anxious patients, the 3-3-3 rule can help ground them: Name 3 things you see, 3 sounds you hear, and move 3 parts of your body.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        item {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        items(listOf(
            QuickAccessItem("Assessment Tools", Icons.Filled.Assessment),
            QuickAccessItem("Session Notes Templates", Icons.Filled.Description),
            QuickAccessItem("Crisis Protocols", Icons.Filled.Warning),
            QuickAccessItem("Insurance Forms", Icons.Filled.Policy)
        )) { item ->
            QuickAccessCard(item)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ResearchSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Latest Research",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        items(listOf(
            ResearchItem("The Therapeutic Alliance in Remote Psychotherapy", "Smith et al.", "Journal of Clinical Psychology"),
            ResearchItem("EMDR vs CBT for PTSD: A Meta-Analysis", "Johnson & Williams", "Psychology Review"),
            ResearchItem("Digital Therapeutics in Mental Health", "Garcia et al.", "Nature Digital Medicine")
        )) { item ->
            ResearchCard(item)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ConferencesSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Upcoming Conferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        items(listOf(
            ConferenceItem("European Psychology Congress", "Amsterdam", "June 15-18, 2026"),
            ConferenceItem("EMDR Europe Annual Conference", "Lisbon", "September 10-12, 2026"),
            ConferenceItem("World Congress of Psychotherapy", "Paris", "November 5-8, 2026")
        )) { item ->
            ConferenceCard(item)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ResourcesLibrarySection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Learning Resources",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        items(listOf(
            ResourceItem("Anxiety Treatment Guidelines", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("DSM-5 Quick Reference", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("CBT Techniques Handbook", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("EMDR Protocol", "PDF", Icons.Filled.PictureAsPdf),
            ResourceItem("Therapy Notes Template", "DOC", Icons.Filled.Description),
            ResourceItem("Consent Form Template", "DOC", Icons.Filled.Description)
        )) { resource ->
            ResourceCard(resource)
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// Quick Access Components
data class QuickAccessItem(val title: String, val icon: ImageVector)

@Composable
fun QuickAccessCard(item: QuickAccessItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

// Research Components
data class ResearchItem(val title: String, val authors: String, val journal: String)

@Composable
fun ResearchCard(item: ResearchItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.authors} • ${item.journal}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

// Conference Components
data class ConferenceItem(val name: String, val location: String, val date: String)

@Composable
fun ConferenceCard(item: ConferenceItem) {
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
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "${item.location} • ${item.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

// Resource Components
data class ResourceItem(val title: String, val type: String, val icon: ImageVector)

@Composable
fun ResourceCard(resource: ResourceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(resource.icon, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = SectionBackground,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = resource.type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
