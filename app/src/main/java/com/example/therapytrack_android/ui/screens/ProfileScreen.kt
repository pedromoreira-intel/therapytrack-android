package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.ui.theme.*

@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MidnightNavy.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MidnightNavy,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dr. Sarah Smith",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Clinical Psychologist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "therapist@therapytrack.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        color = MidnightNavy.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Therapist",
                            style = MaterialTheme.typography.labelMedium,
                            color = MidnightNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    label = "Patients",
                    value = "6",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    label = "Sessions",
                    value = "71",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    label = "Supervisions",
                    value = "4",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Personal Information Section
        item {
            ProfileSection(title = "Personal Information") {
                ProfileInfoRow(label = "Name", value = "Dr. Sarah Smith")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Email", value = "therapist@therapytrack.com")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Phone", value = "+351 912 345 678")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "License", value = "PSY-12345")
            }
        }

        // Professional Details Section
        item {
            ProfileSection(title = "Professional Details") {
                ProfileInfoRow(label = "Title", value = "Clinical Psychologist")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Specialization", value = "Anxiety, Depression, PTSD")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Location", value = "Lisbon, Portugal")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Timezone", value = "Europe/Lisbon (WET)")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Languages", value = "English, Portuguese")
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                ProfileInfoRow(label = "Experience", value = "8 years")
            }
        }

        // App Settings Section
        item {
            ProfileSection(title = "App Settings") {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notifications"
                )
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                SettingsItem(
                    icon = Icons.Filled.Lock,
                    title = "Privacy",
                    subtitle = "Privacy settings"
                )
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    subtitle = "English"
                )
            }
        }

        // Support Section
        item {
            ProfileSection(title = "Support") {
                SettingsItem(
                    icon = Icons.Filled.Help,
                    title = "Help & Support",
                    subtitle = "Get help"
                )
                HorizontalDivider(color = SectionBackground, thickness = 1.dp)
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About",
                    subtitle = "Version 1.0.0"
                )
            }
        }

        // Sign Out Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { /* Logout */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Critical),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProfileStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MidnightNavy
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MidnightNavy,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary
        )
    }
}
