package com.therapytrack.android.ui.therapist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiBrief
import com.therapytrack.android.core.ApiInviteIssued
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiSessionNote
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.PendingSessionNote
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.Instant

/** The pre-session brief, the notes, and the four things a therapist does from here. */
@Composable
fun PatientDetailScreen(patientId: Int, onBack: () -> Unit, onNewNote: (String) -> Unit, onSchedule: () -> Unit, onMessage: (Int, String) -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var patient by remember { mutableStateOf<ApiPatient?>(null) }
    var brief by remember { mutableStateOf<ApiBrief?>(null) }
    var notes by remember { mutableStateOf<List<ApiSessionNote>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingSessionNote>>(emptyList()) }
    var invite by remember { mutableStateOf<ApiInviteIssued?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    val outboxTick by container.sessionNotes.changes.collectAsState()

    LaunchedEffect(patientId, outboxTick) {
        pending = container.sessionNotes.pending(patientId)
        runCatching { patient = container.api.patient(patientId); loadFailed = false }.onFailure { loadFailed = true }
        runCatching { brief = container.api.brief(patientId) }
        runCatching { notes = container.api.sessionNotes(patientId).sortedByDescending { it.sessionNumber } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(patient?.name ?: "…", style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
                patient?.diagnosis?.takeIf { it.isNotBlank() && it != "—" }?.let { Muted(it) }
            }
            patient?.let { Pill(riskLabel(it.riskLevel), riskColor(it.riskLevel)) }
        }
        if (loadFailed) Muted(stringResource(R.string.offline_showing_saved))
        error?.let { Muted(it) }

        val name = patient?.name ?: ""
        PrimaryButton(stringResource(R.string.new_session_note)) { onNewNote(name) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onSchedule, Modifier.weight(1f)) { Text(stringResource(R.string.schedule_session)) }
            TextButton({ patient?.let { p -> p.userId?.let { onMessage(it, p.name) } } }, Modifier.weight(1f), enabled = patient?.userId != null) { Text(stringResource(R.string.message_client)) }
        }

        brief?.let { b ->
            SectionTitle(stringResource(R.string.brief_title))
            Muted(stringResource(R.string.since_last_session, ApiTimestamp.parse(b.since)?.shortDate() ?: b.since))

            b.openAlerts.forEach { a ->
                Card(tint = (if (a.isCritical) TherapyColors.critical else TherapyColors.warning).copy(alpha = 0.15f)) {
                    Pill(a.severity, if (a.isCritical) TherapyColors.critical else TherapyColors.warning)
                    Text(a.detail, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (b.trajectory.isNotEmpty()) Card(tint = TherapyColors.rose.copy(alpha = 0.2f)) {
                Text(stringResource(R.string.concerns), style = MaterialTheme.typography.titleMedium)
                b.trajectory.forEach { c ->
                    val title = stringResource(when (c.type) { "deterioration" -> R.string.concern_deterioration; "alliance_rupture" -> R.string.concern_alliance_rupture; else -> R.string.concern_not_on_track })
                    Text("$title · ${c.instrument.uppercase()}" + if (c.provisional) " (${stringResource(R.string.concern_provisional)})" else "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                    Muted(c.detail)
                }
            }

            b.lastSession?.let { ls ->
                Card {
                    Text(stringResource(R.string.last_session), style = MaterialTheme.typography.titleMedium)
                    Muted(stringResource(R.string.session_n, ls.sessionNumber ?: 0, ls.date?.let { ApiTimestamp.parse(it)?.shortDate() } ?: ""))
                    ls.focus?.let { Text("${stringResource(R.string.focus)}: $it", modifier = Modifier.padding(top = 4.dp)) }
                    ls.homeworkSet?.let { Text("${stringResource(R.string.homework_set)}: $it") }
                    ls.plan?.let { Text("${stringResource(R.string.plan)}: $it") }
                }
            }

            Card {
                Text(stringResource(R.string.measures), style = MaterialTheme.typography.titleMedium)
                listOf("phq9" to "PHQ-9", "gad7" to "GAD-7", "wai_sr" to "WAI-SR").forEach { (key, label) ->
                    val m = b.measures[key]
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(label, modifier = Modifier.weight(1f))
                        if (m == null || !m.hasData || m.latest == null) Muted(stringResource(R.string.no_measure_data))
                        else {
                            val latest = fmt(key, m.latest.score)
                            val text = m.previous?.let { stringResource(R.string.measure_change, fmt(key, it.score), latest) } ?: latest
                            Text(text + (m.latest.severity?.let { " · $it" } ?: ""),
                                color = when (m.change?.direction) { "worsened" -> TherapyColors.critical; "improved" -> TherapyColors.success; else -> TherapyColors.ink })
                        }
                    }
                }
            }

            Card {
                if (b.checkIns.count == 0) Muted(stringResource(R.string.no_check_ins_since))
                else Text(stringResource(R.string.check_ins_summary, b.checkIns.count, avg(b.checkIns.moodAvg), avg(b.checkIns.anxietyAvg), avg(b.checkIns.sleepAvg)))
                if (b.homework.outstandingCount > 0) Muted(stringResource(R.string.homework_outstanding, b.homework.outstandingCount), Modifier.padding(top = 4.dp))
            }

            if (b.journal.sharedCount > 0) Card {
                Text(stringResource(R.string.shared_journal), style = MaterialTheme.typography.titleMedium)
                Muted(stringResource(R.string.shared_entries_count, b.journal.sharedCount))
                b.journal.latest?.let { j ->
                    j.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
                    Text(j.excerpt, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SectionTitle(stringResource(R.string.session_notes))
        pending.forEach { n ->
            Card(tint = TherapyColors.champagne.copy(alpha = 0.25f)) {
                Row { Muted(n.sessionDate); Spacer(Modifier.weight(1f)); Pill(stringResource(R.string.waiting_to_send), TherapyColors.warning) }
                n.focus?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                Text(n.progressNotes, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (notes.isEmpty() && pending.isEmpty()) Muted(stringResource(R.string.no_session_notes))
        notes.forEach { n ->
            Card {
                Muted(stringResource(R.string.session_n, n.sessionNumber, ApiTimestamp.parse(n.sessionDate)?.shortDate() ?: n.sessionDate))
                n.focus?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                Text(n.progressNotes, style = MaterialTheme.typography.bodyMedium)
                n.homework?.takeIf { it.isNotBlank() }?.let { Muted("${stringResource(R.string.homework_set)}: $it", Modifier.padding(top = 4.dp)) }
            }
        }

        SectionTitle(stringResource(R.string.reissue_invite))
        Card {
            Muted(stringResource(R.string.reissue_invite_detail))
            invite?.let { i ->
                Text(i.inviteCode, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(top = 8.dp))
                Muted(ApiTimestamp.parse(i.inviteExpiresAt)?.shortDate() ?: i.inviteExpiresAt)
                TextButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", i.inviteCode)) }) { Text(stringResource(R.string.copy_code)) }
            } ?: TextButton({ scope.launch { runCatching { invite = container.api.reissueInvite(patientId) }.onFailure { error = it.message } } }) { Text(stringResource(R.string.reissue_invite)) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun fmt(key: String, score: Double) = if (key == "wai_sr") String.format("%.1f", score) else score.toInt().toString()
private fun avg(v: Double?) = v?.let { String.format("%.1f", it) } ?: "–"
