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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiClinicalAlert
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiSession
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.ApiUser
import com.therapytrack.android.ui.client.StuckWorkBanner
import com.therapytrack.android.ui.common.Avatar
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.IconCircle
import com.therapytrack.android.ui.common.ListCard
import com.therapytrack.android.ui.common.ListRow
import com.therapytrack.android.ui.common.MetricTile
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.longDay
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The iOS "Hoje": date overline, serif greeting, three tiles, quick actions, upcoming, clients — alerts first when there are any. */
@Composable
fun TodayScreen(onPatient: (Int) -> Unit, onClients: () -> Unit = {}, onNewClient: () -> Unit = {}) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var me by remember { mutableStateOf<ApiUser?>(null) }
    var alerts by remember { mutableStateOf<List<ApiClinicalAlert>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<ApiSession>>(emptyList()) }
    var patients by remember { mutableStateOf<List<ApiPatient>>(emptyList()) }
    var unread by remember { mutableStateOf(0) }
    var pendingNotes by remember { mutableStateOf(0) }
    var offline by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.sessionNotes.changes.collectAsState()

    LaunchedEffect(reload, outboxTick) {
        pendingNotes = container.sessionNotes.pendingCount()
        runCatching { me = container.api.me() }
        try {
            alerts = container.api.alerts()
            sessions = container.api.sessions().filter { it.status != "cancelled" }
            patients = container.api.patients()
            offline = false
        } catch (e: Exception) { offline = true }
        runCatching { unread = container.api.threads().totalUnread }
    }

    val today = LocalDate.now()
    fun day(s: ApiSession) = ApiTimestamp.parse(s.sessionDate)?.atZone(ZoneId.systemDefault())?.toLocalDate()
    val todays = sessions.filter { day(it) == today }
    val upcoming = sessions.filter { (day(it) ?: today) >= today }.take(5)
    val hour = LocalTime.now().hour
    val greeting = stringResource(when { hour < 12 -> R.string.greeting_morning; hour < 19 -> R.string.greeting_afternoon; else -> R.string.greeting_evening }, me?.name?.split(' ')?.getOrNull(1) ?: me?.name?.substringBefore(' ') ?: "")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Overline(Instant.now().longDay(), color = com.therapytrack.android.ui.theme.TherapyColors.champagne.copy(alpha = 1f).let { androidx.compose.ui.graphics.Color(0xFFCFA84A) })
            Text(greeting.trimEnd(',', ' '), style = TherapyType.display, color = TherapyColors.navy)
        }
        if (offline) Row { Muted(stringResource(R.string.offline_showing_saved)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } }
        StuckWorkBanner(refreshKey = outboxTick)
        if (pendingNotes > 0) Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) { Text(stringResource(R.string.notes_waiting, pendingNotes)) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("${todays.size}", stringResource(R.string.tile_today))
            MetricTile("${alerts.size}", stringResource(R.string.tile_flagged), valueColor = if (alerts.isEmpty()) TherapyColors.navy else TherapyColors.critical)
            MetricTile("$unread", stringResource(R.string.tile_unread))
        }

        // Unacknowledged risk outranks everything else on this screen.
        alerts.forEach { a ->
            Card(tint = (if (a.isCritical) TherapyColors.critical else TherapyColors.warning).copy(alpha = 0.12f), modifier = Modifier.clickable { onPatient(a.patientId) }) {
                Row(Modifier.fillMaxWidth()) {
                    Text(a.patientName ?: "#${a.patientId}", style = TherapyType.emphasisLarge, modifier = Modifier.weight(1f))
                    Pill(a.severity, if (a.isCritical) TherapyColors.critical else TherapyColors.warning)
                }
                Text(a.detail, style = TherapyType.body, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Muted(ApiTimestamp.parse(a.createdAt)?.shortDateTime() ?: a.createdAt, Modifier.weight(1f))
                    TextButton({ scope.launch { runCatching { container.api.acknowledgeAlert(a.id) }; reload++ } }) { Text(stringResource(R.string.acknowledge)) }
                }
            }
        }
        if (alerts.isNotEmpty()) Muted(stringResource(R.string.acknowledged_note))

        SectionTitle(stringResource(R.string.quick_actions))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickAction(Icons.Filled.EditCalendar, stringResource(R.string.qa_schedule)) { onClients() }
            QuickAction(Icons.Filled.PersonAdd, stringResource(R.string.new_client)) { onNewClient() }
            QuickAction(Icons.Filled.NoteAdd, stringResource(R.string.qa_notes)) { onClients() }
            QuickAction(Icons.Filled.BarChart, stringResource(R.string.qa_reports)) { onClients() }
        }

        SectionTitle(stringResource(R.string.upcoming), link = stringResource(R.string.agenda_link), onLink = onClients)
        if (upcoming.isEmpty()) Muted(stringResource(R.string.no_upcoming_sessions))
        else ListCard {
            upcoming.forEachIndexed { i, s ->
                val name = s.patientName ?: "#${s.patientId}"
                ListRow(name, subtitle = ApiTimestamp.parse(s.sessionDate)?.shortDateTime() ?: s.sessionDate, trailing = "${s.durationMinutes ?: 50} min",
                    leading = { Avatar(name) }, divider = i < upcoming.lastIndex, onClick = { onPatient(s.patientId) })
            }
        }

        SectionTitle(stringResource(R.string.tab_clients), link = stringResource(R.string.all), onLink = onClients)
        if (patients.isEmpty()) Muted(stringResource(R.string.no_clients))
        else ListCard {
            patients.take(4).forEachIndexed { i, p ->
                ListRow(p.name, leading = { Avatar(p.name) }, chevron = true, divider = i < minOf(3, patients.lastIndex), onClick = { onPatient(p.id) },
                    trailingContent = { Pill(riskLabel(p.riskLevel), riskColor(p.riskLevel)) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        IconCircle(icon, size = 52.dp, square = true)
        Spacer(Modifier.height(6.dp))
        Text(label, style = TherapyType.caption, color = TherapyColors.ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
fun SessionRow(s: ApiSession, onPatient: (Int) -> Unit) {
    Card(modifier = Modifier.clickable { onPatient(s.patientId) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(s.patientName ?: "#"); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.patientName ?: "#${s.patientId}", style = TherapyType.emphasisLarge)
                Muted("${ApiTimestamp.parse(s.sessionDate)?.shortDateTime() ?: s.sessionDate} · ${s.durationMinutes ?: 60} min")
            }
            Pill(stringResource(when (s.status) { "completed" -> R.string.status_completed; "cancelled" -> R.string.status_cancelled; else -> R.string.status_scheduled }),
                 when (s.status) { "completed" -> TherapyColors.success; "cancelled" -> TherapyColors.muted; else -> TherapyColors.navy })
        }
    }
}
