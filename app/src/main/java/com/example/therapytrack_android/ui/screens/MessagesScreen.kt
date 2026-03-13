package com.example.therapytrack_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.therapytrack_android.data.model.Message
import com.example.therapytrack_android.ui.theme.*
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel

@Composable
fun MessagesScreen(viewModel: TherapyViewModel) {
    
    // Use mock messages for now
    val messages = remember {
        listOf(
            Message(1, 1, 2, "John Anderson", "Therapist", "Hi Dr. Smith, I wanted to check in about our session.", false, "2026-03-09T10:00:00Z"),
            Message(2, 2, 1, "Emma Wilson", "Therapist", "Thank you for the resources you shared.", false, "2026-03-09T09:30:00Z"),
            Message(3, 3, 1, "Michael Chen", "Therapist", "I had a question about the homework.", true, "2026-03-08T15:00:00Z")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SectionBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Messages List
        items(messages) { message ->
            MessageCard(message = message)
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MessageCard(message: Message) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MidnightNavy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.fromName?.take(1)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.fromName ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
            
            if (message.read == false) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MidnightNavy)
                )
            }
        }
    }
}

fun formatMessageTime(isoString: String): String {
    return try {
        isoString.substringBefore("T")
    } catch (e: Exception) {
        isoString
    }
}
