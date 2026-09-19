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
import androidx.compose.ui.Alignment
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
import com.therapytrack.android.ui.common.LabelledValue
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.theme.TherapyType
import androidx.compose.foundation.layout.statusBarsPadding
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
fun PatientDetailScreen(patientId: Int, onBack: () -> Unit, onNewNote: (String) -> Unit, onSchedule: () -> Unit, onMessage: (Int, String) -> Unit,
                        onAiTools: (String) -> Unit) {
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
    var editing by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.sessionNotes.changes.collectAsState()

    LaunchedEffect(patientId, outboxTick, reload) {
        pending = container.sessionNotes.pending(patientId)
        runCatching { container.api.markNotificationsRead("alert", patientId) }; container.refreshInbox()
        runCatching { patient = container.api.patient(patientId); loadFailed = false }.onFailure { loadFailed = true }
        runCatching { brief = container.api.brief(patientId) }
        runCatching { notes = container.api.sessionNotes(patientId).sortedByDescending { it.sessionNumber } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // iOS nav bar: "< Clientes" left, "Resumo pré-sessão" centred
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("‹ " + stringResource(R.string.tab_clients), style = TherapyType.bodyLarge, color = TherapyColors.navy) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.brief_title), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
            Spacer(Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(patient?.name ?: "…", style = TherapyType.display, color = TherapyColors.ink)
                patient?.diagnosis?.takeIf { it.isNotBlank() && it != "—" }?.let { Muted(it) }
            }
            patient?.let { Pill(riskLabel(it.riskLevel), riskColor(it.riskLevel)) }
        }
        if (loadFailed) Muted(stringResource(R.string.offline_showing_saved))
        error?.let { Muted(it) }

        val name = patient?.name ?: ""
        PrimaryButton(stringResource(R.string.new_session_note)) { onNewNote(name) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onSchedule) { Text(stringResource(R.string.schedule_session), color = TherapyColors.navy) }
            TextButton({ patient?.let { p -> p.userId?.let { onMessage(it, p.name) } } }, enabled = patient?.userId != null) { Text(stringResource(R.string.message_client), color = TherapyColors.navy) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton({ editing = true }) { Text(stringResource(R.string.edit_client), color = TherapyColors.navy) }
            TextButton({ onAiTools(name) }) { Text(stringResource(R.string.ai_section), color = TherapyColors.navy) }
        }

        brief?.let { b ->
            b.openAlerts.forEach { a ->
                Card(tint = (if (a.isCritical) TherapyColors.critical else TherapyColors.warning).copy(alpha = 0.12f)) {
                    Pill(a.severity, if (a.isCritical) TherapyColors.critical else TherapyColors.warning)
                    Text(a.detail, style = TherapyType.body, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (b.trajectory.isNotEmpty()) Card(tint = TherapyColors.rose.copy(alpha = 0.2f)) {
                Overline(stringResource(R.string.concerns))
                b.trajectory.forEach { c ->
                    val title = stringResource(when (c.type) { "deterioration" -> R.string.concern_deterioration; "alliance_rupture" -> R.string.concern_alliance_rupture; else -> R.string.concern_not_on_track })
                    Text("$title · ${c.instrument.uppercase()}" + if (c.provisional) " (${stringResource(R.string.concern_provisional)})" else "", style = TherapyType.emphasisLarge, modifier = Modifier.padding(top = 8.dp))
                    Muted(c.detail)
                }
            }

            // WHERE YOU LEFT OFF
            Card(padding = 18.dp) {
                Overline(stringResource(R.string.where_you_left_off), color = TherapyColors.navy)
                b.lastSession?.let { ls ->
                    ls.plan?.let { LabelledValue(stringResource(R.string.plan), it, emphasis = true) }
                    ls.focus?.let { LabelledValue(stringResource(R.string.last_focus), it) }
                    ls.homeworkSet?.let { LabelledValue(stringResource(R.string.homework_set), it) }
                    LabelledValue(stringResource(R.string.last_session), (ls.date?.let { ApiTimestamp.parse(it)?.shortDate() } ?: "") + " · #" + (ls.sessionNumber ?: 0))
                } ?: Muted(stringResource(R.string.no_session_notes), Modifier.padding(top = 8.dp))
            }

            // WHAT MOVED
            Card(padding = 18.dp) {
                Overline(stringResource(R.string.what_moved), color = TherapyColors.navy)
                listOf("phq9" to ("PHQ-9" to R.string.measure_depression), "gad7" to ("GAD-7" to R.string.measure_anxiety), "wai_sr" to ("WAI-SR" to R.string.measure_alliance)).forEach { (key, meta) ->
                    val m = b.measures[key]
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(meta.first, style = TherapyType.emphasisLarge); Muted(stringResource(meta.second)) }
                        if (m == null || !m.hasData || m.latest == null) Muted(stringResource(R.string.not_measured))
                        else Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                m.change?.takeIf { it.delta != 0.0 }?.let { ch ->
                                    val col = when (ch.direction) { "worsened" -> TherapyColors.critical; "improved" -> TherapyColors.success; else -> TherapyColors.muted }
                                    Text((if (ch.delta < 0) "↓ " else "↑ ") + fmt(key, kotlin.math.abs(ch.delta)), style = TherapyType.emphasis, color = col, modifier = Modifier.padding(end = 8.dp))
                                }
                                Text(fmt(key, m.latest.score), style = TherapyType.metric, color = TherapyColors.ink)
                            }
                            m.latest.severity?.let { Muted(it) }
                        }
                    }
                }
            }

            // BETWEEN SESSIONS
            Card(padding = 18.dp) {
                Overline(stringResource(R.string.between_sessions), color = TherapyColors.navy)
                Muted(stringResource(R.string.since_last_session, ApiTimestamp.parse(b.since)?.shortDate() ?: b.since), Modifier.padding(top = 4.dp))
                LabelledValue(stringResource(R.string.tab_home).let { "Check-ins" },
                    if (b.checkIns.count == 0) stringResource(R.string.no_check_ins_since)
                    else "${b.checkIns.count} · ${stringResource(R.string.legend_mood)} ${avg(b.checkIns.moodAvg)}/10 · ${stringResource(R.string.legend_anxiety)} ${avg(b.checkIns.anxietyAvg)}/10")
                if (b.homework.outstandingCount > 0) LabelledValue(stringResource(R.string.to_complete), b.homework.outstanding.joinToString("\n") { it.title }, emphasis = true)
                if (b.journal.sharedCount > 0) b.journal.latest?.let { j ->
                    LabelledValue(stringResource(R.string.shared_journal) + " (" + stringResource(R.string.shared_entries_count, b.journal.sharedCount) + ")", j.excerpt)
                }
            }
        }

        SectionTitle(stringResource(R.string.session_notes))
        pending.forEach { n ->
            Card(tint = TherapyColors.champagne.copy(alpha = 0.25f)) {
                Row { Muted(n.sessionDate); Spacer(Modifier.weight(1f)); Pill(stringResource(R.string.waiting_to_send), TherapyColors.warning) }
                n.focus?.let { Text(it, style = TherapyType.emphasisLarge) }
                Text(n.progressNotes, style = TherapyType.body)
            }
        }
        if (notes.isEmpty() && pending.isEmpty()) Muted(stringResource(R.string.no_session_notes))
        notes.forEach { n ->
            Card {
                Muted(stringResource(R.string.session_n, n.sessionNumber, ApiTimestamp.parse(n.sessionDate)?.shortDate() ?: n.sessionDate))
                n.focus?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.emphasisLarge) }
                Text(n.progressNotes, style = TherapyType.body)
                n.homework?.takeIf { it.isNotBlank() }?.let { Muted("${stringResource(R.string.homework_set)}: $it", Modifier.padding(top = 4.dp)) }
            }
        }

        GoalsSection(patientId)
        HistorySection(patientId)
        SharedJournalSection(patientId)
        ClientFeaturesSection(patientId)

        SectionTitle(stringResource(R.string.reissue_invite))
        Card {
            Muted(stringResource(R.string.reissue_invite_detail))
            invite?.let { i ->
                Text(i.inviteCode, style = TherapyType.display, color = TherapyColors.navy, modifier = Modifier.padding(top = 8.dp))
                Muted(ApiTimestamp.parse(i.inviteExpiresAt)?.shortDate() ?: i.inviteExpiresAt)
                TextButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", i.inviteCode)) }) { Text(stringResource(R.string.copy_code)) }
            } ?: TextButton({ scope.launch { runCatching { invite = container.api.reissueInvite(patientId) }.onFailure { error = it.message } } }) { Text(stringResource(R.string.reissue_invite)) }
        }
        Spacer(Modifier.height(24.dp))
    }
    EditHost(patient, editing) { saved -> editing = false; if (saved) reload++ }
}

@Composable
private fun EditHost(patient: ApiPatient?, editing: Boolean, onDone: (Boolean) -> Unit) {
    if (editing && patient != null) EditClientDialog(patient, onDone)
}

private fun fmt(key: String, score: Double) = if (key == "wai_sr") String.format("%.1f", score) else score.toInt().toString()
private fun avg(v: Double?) = v?.let { String.format("%.1f", it) } ?: "–"
