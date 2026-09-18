package com.therapytrack.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun Card(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tint)
            .border(0.5.dp, TherapyColors.hairline, MaterialTheme.shapes.medium)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = TherapyColors.navy, contentColor = Color.White)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TherapyColors.navy, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun Pill(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = TherapyColors.muted, modifier = modifier)
}

@Composable
fun ErrorText(text: String?) {
    if (text != null) Text(text, color = TherapyColors.critical, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
}

private val dayTime = DateTimeFormatter.ofPattern("d MMM, HH:mm")
private val dayOnly = DateTimeFormatter.ofPattern("d MMM yyyy")

fun Instant.shortDateTime(): String = dayTime.format(atZone(ZoneId.systemDefault()))
fun Instant.shortDate(): String = dayOnly.format(atZone(ZoneId.systemDefault()))
