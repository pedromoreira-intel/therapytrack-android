package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiClinicalAlert
import com.therapytrack.android.core.ApiSession
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.client.StuckWorkBanner
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Unacknowledged risk first — it outranks everything else on this screen — then today's sessions. */
@Composable
fun TodayScreen(onPatient: (Int) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<ApiClinicalAlert>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<ApiSession>>(emptyList()) }
    var pendingNotes by remember { mutableStateOf(0) }
    var offline by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.sessionNotes.changes.collectAsState()

    LaunchedEffect(reload, outboxTick) {
        pendingNotes = container.sessionNotes.pendingCount()
        try {
            alerts = container.api.alerts()
            sessions = container.api.sessions().filter { it.status != "cancelled" }
            offline = false
        } catch (e: Exception) { offline = true }
    }

    val today = LocalDate.now()
    val (todays, upcoming) = sessions.partition { ApiTimestamp.parse(it.sessionDate)?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate() == today }
    val future = upcoming.filter { (ApiTimestamp.parse(it.sessionDate)?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate() ?: today) > today }.take(5)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.today_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        if (offline) Row { Muted(stringResource(R.string.offline_showing_saved)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } }
        StuckWorkBanner(refreshKey = outboxTick)
        if (pendingNotes > 0) Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) { Text(stringResource(R.string.notes_waiting, pendingNotes)) }

        SectionTitle(stringResource(R.string.open_alerts))
        if (alerts.isEmpty()) Muted(stringResource(R.string.no_open_alerts))
        alerts.forEach { a ->
            Card(tint = (if (a.isCritical) TherapyColors.critical else TherapyColors.warning).copy(alpha = 0.15f), modifier = Modifier.clickable { onPatient(a.patientId) }) {
                Row(Modifier.fillMaxWidth()) {
                    Text(a.patientName ?: "#${a.patientId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill(a.severity, if (a.isCritical) TherapyColors.critical else TherapyColors.warning)
                }
                Text(a.detail, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                Muted(ApiTimestamp.parse(a.createdAt)?.shortDateTime() ?: a.createdAt)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ scope.launch { runCatching { container.api.acknowledgeAlert(a.id) }; reload++ } }) { Text(stringResource(R.string.acknowledge)) }
                }
            }
        }
        if (alerts.isNotEmpty()) Muted(stringResource(R.string.acknowledged_note))

        SectionTitle(stringResource(R.string.todays_sessions))
        if (todays.isEmpty()) Muted(stringResource(R.string.no_sessions_today))
        todays.forEach { SessionRow(it, onPatient) }

        SectionTitle(stringResource(R.string.upcoming_sessions))
        if (future.isEmpty()) Muted(stringResource(R.string.no_upcoming_sessions))
        future.forEach { SessionRow(it, onPatient) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SessionRow(s: ApiSession, onPatient: (Int) -> Unit) {
    Card(modifier = Modifier.clickable { onPatient(s.patientId) }) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(s.patientName ?: "#${s.patientId}", style = MaterialTheme.typography.titleMedium)
                Muted("${ApiTimestamp.parse(s.sessionDate)?.shortDateTime() ?: s.sessionDate} · ${s.durationMinutes ?: 60} min")
            }
            Pill(stringResource(when (s.status) { "completed" -> R.string.status_completed; "cancelled" -> R.string.status_cancelled; else -> R.string.status_scheduled }),
                 when (s.status) { "completed" -> TherapyColors.success; "cancelled" -> TherapyColors.muted; else -> TherapyColors.navy })
        }
    }
}
