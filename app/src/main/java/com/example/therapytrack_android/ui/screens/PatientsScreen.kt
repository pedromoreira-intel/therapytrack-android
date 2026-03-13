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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.data.model.Patient
import com.example.therapytrack_android.ui.theme.*
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel

@Composable
fun PatientsScreen(viewModel: TherapyViewModel) {
    
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredPatients = viewModel.patients.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Patients",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search patients...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightNavy,
                    unfocusedBorderColor = Neutral
                ),
                singleLine = true
            )
        }
        
        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stableCount = viewModel.patients.count { it.status == "STABLE" }
                val improvingCount = viewModel.patients.count { it.status == "IMPROVING" }
                val atRiskCount = viewModel.patients.count { it.risk_level == "HIGH" }
                
                FilterChip(
                    label = "Stable ($stableCount)",
                    selected = false,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    label = "Improving ($improvingCount)",
                    selected = false,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    label = "At Risk ($atRiskCount)",
                    selected = false,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Patient List
        items(filteredPatients) { patient ->
            PatientListCard(patient = patient)
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) MidnightNavy else CardBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PearlWhite else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun PatientListCard(patient: Patient) {
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
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MidnightNavy.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
                Text(
                    text = "${patient.session_count} sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = patient.status, riskLevel = patient.risk_level)
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
