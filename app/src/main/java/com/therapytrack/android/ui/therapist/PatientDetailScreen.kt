package com.therapytrack.android.ui.therapist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.Api
import com.therapytrack.android.core.ApiAssessmentResult
import com.therapytrack.android.core.ApiBrief
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiGoal
import com.therapytrack.android.core.ApiInviteIssued
import com.therapytrack.android.core.ApiJournalEntry
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiSession
import com.therapytrack.android.core.ApiSessionNote
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.noteFor
import com.therapytrack.android.offline.PendingSessionNote
import com.therapytrack.android.ui.client.appLocale
import com.therapytrack.android.ui.common.BackRow
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.LabelledValue
import com.therapytrack.android.ui.common.LineChart
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.common.Series
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The client's record, as iOS ClientDetailView: the name once, two small
 * actions, and three views — Resumo (resuming the work), Histórico (one dated
 * timeline, each item saying who it came from) and Avaliações (an index of
 * measures, each opening its history). Editing, the client's app features,
 * the invitation and AI drafts sit behind Detalhes.
 */
@Composable
fun PatientDetailScreen(patientId: Int, onBack: () -> Unit, onSession: (ApiSession, Boolean) -> Unit, onReview: (String) -> Unit,
                        onNewNote: (String) -> Unit, onSchedule: () -> Unit, onMessage: (Int, String) -> Unit, onAiTools: (String) -> Unit,
                        startOnHistory: Boolean = false) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var patient by remember { mutableStateOf<ApiPatient?>(null) }
    var brief by remember { mutableStateOf<ApiBrief?>(null) }
    var sessions by remember { mutableStateOf<List<ApiSession>>(emptyList()) }
    var notes by remember { mutableStateOf<List<ApiSessionNote>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingSessionNote>>(emptyList()) }
    var goals by remember { mutableStateOf<List<ApiGoal>>(emptyList()) }
    var journal by remember { mutableStateOf<List<ApiJournalEntry>>(emptyList()) }
    var checkIns by remember { mutableStateOf<List<ApiEmaResponse>>(emptyList()) }
    var results by remember { mutableStateOf<Map<Api.Instrument, List<ApiAssessmentResult>>>(emptyMap()) }
    var loadFailed by remember { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf(if (startOnHistory) "history" else "summary") }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.sessionNotes.changes.collectAsState()

    LaunchedEffect(patientId, outboxTick, reload) {
        pending = container.sessionNotes.pending(patientId)
        runCatching { container.api.markNotificationsRead("alert", patientId) }
        runCatching { container.api.markNotificationsRead("client_record", patientId) }; container.refreshInbox()
        runCatching { patient = container.api.patient(patientId); loadFailed = false }.onFailure { loadFailed = true }
        runCatching { brief = container.api.brief(patientId) }
        runCatching { sessions = container.api.sessions(patientId = patientId) }
        runCatching { notes = container.api.sessionNotes(patientId) }
        runCatching { goals = container.api.goals(patientId) }
        runCatching { journal = container.api.sharedJournal(patientId) }
        runCatching { checkIns = container.api.checkIns(patientId) }
        results = Api.Instrument.entries.associateWith { runCatching { container.api.assessmentResults(it, patientId) }.getOrDefault(emptyList()) }
    }

    val name = patient?.name ?: ""
    val shown = patient
    if (showDetails && shown != null) {
        DetailsPage(shown, onBack = { showDetails = false; reload++ }, onNewNote = { onNewNote(name) }, onAiTools = { onAiTools(name) })
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BackRow(onBack)
        // The name once, in the page's serif, and two small actions.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(patient?.name ?: "…", style = TherapyType.display, color = TherapyColors.navy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                patient?.userId?.let { uid -> HeaderAction(stringResource(R.string.message_short), Icons.AutoMirrored.Outlined.Chat) { onMessage(uid, name) } }
                HeaderAction(stringResource(R.string.details_short), Icons.Outlined.Badge) { showDetails = true }
            }
        }
        if (loadFailed) Text(stringResource(R.string.offline_showing_saved), style = TherapyType.caption, color = TherapyColors.muted)
        Segmented(listOf("summary" to stringResource(R.string.rec_summary), "history" to stringResource(R.string.rec_history), "assessments" to stringResource(R.string.rec_assessments)),
            view, { view = it })

        val zone = ZoneId.systemDefault()
        val dated = sessions.filter { it.status != "cancelled" }.mapNotNull { s -> ApiTimestamp.parse(s.sessionDate)?.let { s to it } }.sortedBy { it.second }
        when (view) {
            "summary" -> Summary(brief, dated, pending, goals, journal, checkIns, results, onSession, onReview = { onReview(name) }, onSchedule,
                onOpenHistory = { view = "history" },
                onToggleGoal = { g -> scope.launch { runCatching { container.api.setGoalCompleted(g.id, !g.completed) }; reload++ } },
                onAddGoal = { title, category, due -> scope.launch { runCatching { container.api.createGoal(patientId, title, null, category, due) }; reload++ } })
            "history" -> History(dated, notes, pending, journal, brief, results, zone, onSession, onQuestionnaire = { view = "assessments" })
            else -> Assessments(checkIns, results)
        }
    }
}

@Composable
private fun HeaderAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.08f)).clickable(onClick = onClick).heightIn(min = 36.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TherapyColors.navy, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = TherapyType.emphasis, color = TherapyColors.navy)
    }
}

@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral, modifier = Modifier.size(18.dp))

private fun ended(s: ApiSession, at: Instant) = at.plusSeconds(60L * (s.durationMinutes ?: 50)).isBefore(Instant.now())

@Composable
private fun whenText(at: Instant, minutes: Int?): String {
    val locale = appLocale()
    val local = at.atZone(ZoneId.systemDefault())
    val day = if (local.toLocalDate() == LocalDate.now()) stringResource(R.string.today) else local.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
    return listOfNotNull(day, local.format(DateTimeFormatter.ofPattern("HH:mm", locale)), minutes?.let { "$it min" }).joinToString(" · ")
}

// Resumo ----------------------------------------------------------------------

/** Resuming the work: what is next, where it was left, what is waiting. */
@Composable
private fun Summary(brief: ApiBrief?, dated: List<Pair<ApiSession, Instant>>, pending: List<PendingSessionNote>, goals: List<ApiGoal>,
                    journal: List<ApiJournalEntry>, checkIns: List<ApiEmaResponse>, results: Map<Api.Instrument, List<ApiAssessmentResult>>,
                    onSession: (ApiSession, Boolean) -> Unit, onReview: () -> Unit, onSchedule: () -> Unit, onOpenHistory: () -> Unit,
                    onToggleGoal: (ApiGoal) -> Unit, onAddGoal: (String, String, String?) -> Unit) {
    val locale = appLocale()
    // The first session that has not ended: one under way is still "next".
    val next = dated.firstOrNull { (s, at) -> !ended(s, at) }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.rec_next_session))
            CompactCard {
                if (next != null) {
                    val (s, at) = next
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onSession(s, false) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(whenText(at, s.durationMinutes), style = TherapyType.emphasis, color = TherapyColors.ink)
                            if (at <= Instant.now()) Text(stringResource(R.string.slot_in_progress), style = TherapyType.caption, color = TherapyColors.navy)
                        }
                        Text(stringResource(R.string.sf_prepare), style = TherapyType.emphasis, color = TherapyColors.navy)
                        Chevron()
                    }
                } else Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.no_session_scheduled), style = TherapyType.body, color = TherapyColors.muted, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.schedule_session), style = TherapyType.emphasis, color = TherapyColors.navy,
                        modifier = Modifier.clickable(onClick = onSchedule).heightIn(min = 44.dp).padding(vertical = 12.dp))
                }
            }
        }

        val alerts = brief?.openAlerts.orEmpty()
        if (alerts.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.rec_to_review))
            CompactCard {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onReview).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(3.dp, 30.dp).clip(RoundedCornerShape(1.5.dp)).background(TherapyColors.warning))
                    Spacer(Modifier.width(12.dp))
                    Text(if (alerts.size == 1) stringResource(R.string.rec_one_review) else stringResource(R.string.rec_n_review, alerts.size),
                        style = TherapyType.body, color = TherapyColors.ink, modifier = Modifier.weight(1f))
                    Chevron()
                }
            }
        }

        if (pending.isNotEmpty()) Text(stringResource(R.string.notes_waiting, pending.size), style = TherapyType.caption, color = TherapyColors.warning)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.sf_where_left_off))
            CompactCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val last = brief?.lastSession
                    if (last == null) Text(stringResource(R.string.rec_no_note_yet), style = TherapyType.body, color = TherapyColors.muted)
                    else {
                        ApiTimestamp.parse(last.date)?.let { d ->
                            Text(listOfNotNull(d.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM", locale)), last.sessionNumber?.let { stringResource(R.string.sf_session_n, it) }).joinToString(" · "),
                                style = TherapyType.caption, color = TherapyColors.muted)
                        }
                        last.focus?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_focus), it) }
                        last.plan?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_agreed_next_steps), it, emphasis = true) }
                        last.homeworkSet?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_homework_set), it) }
                        // Risk with where it came from: the therapist's own rating in that note.
                        last.riskLevel?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.rec_risk_in_note), riskLabel(it)) }
                    }
                }
            }
        }

        // What the client added since the last session — counted, opened by choice.
        val since = ApiTimestamp.parse(brief?.lastSession?.date) ?: Instant.now().minus(30, ChronoUnit.DAYS)
        val nCheckIns = checkIns.count { ApiTimestamp.parse(it.doneAt)?.isAfter(since) == true }
        val nQuestionnaires = results.values.flatten().count { ApiTimestamp.parse(it.assessmentDate)?.isAfter(since) == true }
        val nJournal = journal.count { ApiTimestamp.parse(it.createdAt)?.isAfter(since) == true }
        val nTopics = brief?.clientTopics?.size ?: 0
        val parts = listOfNotNull(
            when (nCheckIns) { 0 -> null; 1 -> stringResource(R.string.rec_1_checkin); else -> stringResource(R.string.rec_n_checkins, nCheckIns) },
            when (nQuestionnaires) { 0 -> null; 1 -> stringResource(R.string.rec_1_questionnaire); else -> stringResource(R.string.rec_n_questionnaires, nQuestionnaires) },
            when (nJournal) { 0 -> null; 1 -> stringResource(R.string.rec_1_journal); else -> stringResource(R.string.rec_n_journal, nJournal) },
            when (nTopics) { 0 -> null; 1 -> stringResource(R.string.rec_1_topic); else -> stringResource(R.string.rec_n_topics, nTopics) })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(if (brief?.lastSession?.date == null) R.string.rec_from_client_30 else R.string.rec_from_client_since))
            CompactCard {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = parts.isNotEmpty(), onClick = onOpenHistory).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (parts.isEmpty()) stringResource(R.string.rec_nothing_new) else parts.joinToString(" · "), style = TherapyType.body,
                        color = if (parts.isEmpty()) TherapyColors.muted else TherapyColors.ink, modifier = Modifier.weight(1f))
                    if (parts.isNotEmpty()) Chevron()
                }
            }
        }

        // Goals and homework share a table; homework is the category, anything else reads as a goal.
        GoalList(stringResource(R.string.rec_goals), goals.filter { it.category != "homework" }, withDue = true, onToggleGoal) { t, due -> onAddGoal(t, "general", due) }
        GoalList(stringResource(R.string.sf_homework), goals.filter { it.category == "homework" }, withDue = false, onToggleGoal) { t, _ -> onAddGoal(t, "homework", null) }
    }
}

@Composable
private fun GoalList(title: String, items: List<ApiGoal>, withDue: Boolean, onToggle: (ApiGoal) -> Unit, onAdd: (String, String?) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val open = items.count { !it.completed }
            SectionLabel(title + if (open > 0) " · $open" else "", Modifier.weight(1f))
            Row(Modifier.heightIn(min = 32.dp).clickable { adding = true }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add), style = TherapyType.emphasis, color = TherapyColors.navy)
            }
        }
        if (items.isEmpty()) Text(stringResource(R.string.rec_none_yet), style = TherapyType.body, color = TherapyColors.muted)
        else CompactCard {
            items.forEachIndexed { i, g ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.title, style = TherapyType.body, color = TherapyColors.ink, textDecoration = if (g.completed) TextDecoration.LineThrough else null)
                        g.dueDate?.let { ApiTimestamp.parse(it) }?.let { Text(stringResource(R.string.due, it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy", appLocale()))),
                            style = TherapyType.caption, color = TherapyColors.muted) }
                    }
                    Spacer(Modifier.width(8.dp))
                    // Tapping it records the change.
                    val color = if (g.completed) TherapyColors.success else TherapyColors.navy
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).clickable { onToggle(g) }.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (g.completed) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = color, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (g.completed) R.string.rec_completed else R.string.rec_in_progress), style = TherapyType.caption, color = color)
                    }
                }
                if (i < items.lastIndex) RowDivider()
            }
        }
    }
    if (adding) {
        var text by remember { mutableStateOf("") }
        var due by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.goal_title)) })
                    if (withDue) OutlinedTextField(due, { due = it }, label = { Text(stringResource(R.string.due_date)) }, singleLine = true)
                }
            },
            confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onAdd(text.trim(), due.trim().ifBlank { null }); adding = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ adding = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = Color.White)
    }
}

// Histórico -------------------------------------------------------------------

private enum class Kind { SESSION, QUESTIONNAIRE, JOURNAL, TOPIC }
private data class Event(val key: String, val at: Instant, val kind: Kind, val title: String, val detail: String?, val byClient: Boolean, val session: ApiSession? = null)

/** One dated timeline — sessions and notes, questionnaires, shared entries — each saying who it came from. */
@Composable
private fun History(dated: List<Pair<ApiSession, Instant>>, notes: List<ApiSessionNote>, pending: List<PendingSessionNote>, journal: List<ApiJournalEntry>, brief: ApiBrief?,
                    results: Map<Api.Instrument, List<ApiAssessmentResult>>, zone: ZoneId, onSession: (ApiSession, Boolean) -> Unit, onQuestionnaire: () -> Unit) {
    val locale = appLocale()
    val sessionWord = stringResource(R.string.rec_session)
    val noteDone = stringResource(R.string.note_done)
    val noNote = stringResource(R.string.rec_no_note)
    val scheduled = stringResource(R.string.status_scheduled)
    val waiting = stringResource(R.string.waiting_to_send)
    val journalWord = stringResource(R.string.rec_journal_entry)
    val wantsWord = stringResource(R.string.rec_wants_to_talk)
    val bands = mapOf("minimal" to stringResource(R.string.band_minimal), "mild" to stringResource(R.string.band_mild), "moderate" to stringResource(R.string.band_moderate),
        "moderately_severe" to stringResource(R.string.band_moderately_severe), "severe" to stringResource(R.string.band_severe))
    val events = buildList {
        dated.forEach { (s, at) ->
            val day = at.atZone(zone).toLocalDate().toString()
            val note = notes.noteFor(s.id, day)
            val queued = pending.any { it.sessionId == s.id || (it.sessionId == null && it.sessionDate == day) }
            val detail = note?.let { n -> n.focus?.takeIf { it.isNotBlank() } ?: noteDone } ?: if (queued) waiting else if (ended(s, at)) noNote else scheduled
            add(Event("s${s.id}", at, Kind.SESSION, sessionWord, detail, false, s))
        }
        results.forEach { (inst, list) -> list.forEach { r -> ApiTimestamp.parse(r.assessmentDate)?.let { at ->
            val score = if (inst == Api.Instrument.WAISR) String.format(locale, "%.1f", r.score) else r.score.toInt().toString()
            val max = when (inst) { Api.Instrument.PHQ9 -> 27; Api.Instrument.GAD7 -> 21; Api.Instrument.WAISR -> 5 }
            add(Event("${inst.path}${r.id}", at, Kind.QUESTIONNAIRE, "${inst.displayName} · $score/$max", r.severity?.let { bands[it] ?: it }, true))
        } } }
        journal.forEach { e -> ApiTimestamp.parse(e.createdAt)?.let { at ->
            add(Event("j${e.id}", at, Kind.JOURNAL, e.title?.takeIf { it.isNotBlank() } ?: journalWord, e.content.take(140), true))
        } }
        brief?.clientTopics?.forEach { t -> add(Event("t${t.id}", ApiTimestamp.parse(t.createdAt) ?: Instant.now(), Kind.TOPIC, wantsWord, t.text, true)) }
    }.sortedByDescending { it.at }

    if (events.isEmpty()) { Text(stringResource(R.string.cpd_nothing), style = TherapyType.body, color = TherapyColors.muted); return }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        events.groupBy { it.at.atZone(zone).let { z -> z.year * 100 + z.monthValue } }.forEach { (_, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(items.first().at.atZone(zone).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)))
                CompactCard {
                    items.forEachIndexed { i, e ->
                        val clickable = e.kind == Kind.SESSION || e.kind == Kind.QUESTIONNAIRE
                        Row(Modifier.fillMaxWidth().clickable(enabled = clickable) {
                            // The same Preparar / Nota as from Hoje and the Agenda: one history.
                            if (e.kind == Kind.SESSION) e.session?.let { s -> onSession(s, ended(s, e.at)) } else onQuestionnaire()
                        }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                            Text(e.at.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM", locale)), style = TherapyType.caption, color = TherapyColors.muted,
                                modifier = Modifier.width(40.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.title, style = TherapyType.emphasis, color = TherapyColors.ink, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(Modifier.width(6.dp))
                                    val tint = if (e.byClient) TherapyColors.navy else TherapyColors.muted
                                    Text(stringResource(if (e.byClient) R.string.rec_by_client else R.string.rec_by_therapist), style = TherapyType.caption, color = tint,
                                        modifier = Modifier.clip(CircleShape).background(tint.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 1.dp))
                                }
                                e.detail?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = TherapyType.caption, color = TherapyColors.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (clickable) Box(Modifier.padding(top = 2.dp)) { Chevron() }
                        }
                        if (i < items.lastIndex) RowDivider(64.dp)
                    }
                }
            }
        }
    }
}

// Avaliações ------------------------------------------------------------------

/** An index of measures, one row each: the latest result, when, and a factual comparison. Each row opens its history. */
@Composable
private fun Assessments(checkIns: List<ApiEmaResponse>, results: Map<Api.Instrument, List<ApiAssessmentResult>>) {
    val locale = appLocale()
    val zone = ZoneId.systemDefault()
    val dm = DateTimeFormatter.ofPattern("dd/MM", locale)
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = checkIns.mapNotNull { c -> ApiTimestamp.parse(c.doneAt)?.let { it to c } }.sortedBy { it.first }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.sf_check_ins))
            entries.lastOrNull()?.let { (at, _) ->
                val recent = entries.count { it.first.isAfter(Instant.now().minus(30, ChronoUnit.DAYS)) }
                Text(stringResource(R.string.rec_latest_checkin, at.atZone(zone).format(DateTimeFormatter.ofPattern("dd/MM, HH:mm", locale))) + " · " +
                    (if (recent == 1) stringResource(R.string.rec_1_entry_30) else stringResource(R.string.rec_n_entries_30, recent)),
                    style = TherapyType.caption, color = TherapyColors.muted)
            }
            CompactCard {
                val metrics = listOf(Triple("mood", R.string.legend_mood, TherapyColors.navy), Triple("anxiety", R.string.legend_anxiety, TherapyColors.warning),
                    Triple("sleep", R.string.legend_sleep, TherapyColors.success))
                metrics.forEachIndexed { i, (key, label, color) ->
                    fun value(c: ApiEmaResponse) = when (key) { "mood" -> c.mood; "anxiety" -> c.anxiety; else -> c.sleep }
                    val last = entries.lastOrNull { value(it.second) != null }
                    IndexRow(stringResource(label).replaceFirstChar { it.titlecase(locale) }, last?.let { "${value(it.second)}/7" }, null, open == key, enabled = last != null) { open = if (open == key) null else key }
                    if (open == key) {
                        val start = Instant.now().minus(30, ChronoUnit.DAYS)
                        val pts = entries.filter { it.first.isAfter(start) }.mapNotNull { (t, c) -> value(c)?.let { v -> frac(t, start, Instant.now()) to v.toFloat() } }
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            if (pts.isEmpty()) Text(stringResource(R.string.rec_none_30), style = TherapyType.caption, color = TherapyColors.muted)
                            else LineChart(listOf(Series(stringResource(label), color, pts)), minY = 1f, maxY = 7f)
                        }
                    }
                    if (i < metrics.lastIndex) RowDivider()
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.sf_questionnaires))
            CompactCard {
                val list = listOf(Api.Instrument.PHQ9 to 27, Api.Instrument.GAD7 to 21, Api.Instrument.WAISR to 5)
                list.forEachIndexed { i, (inst, max) ->
                    val decimals = if (inst == Api.Instrument.WAISR) 1 else 0
                    fun f(v: Double) = String.format(locale, "%.${decimals}f", v)
                    val obs = results[inst].orEmpty().mapNotNull { r -> ApiTimestamp.parse(r.assessmentDate)?.let { it to r } }.sortedBy { it.first }
                    val last = obs.lastOrNull()
                    val previous = obs.getOrNull(obs.size - 2)
                    val detail = last?.let { (at, _) ->
                        at.atZone(zone).format(dm) + (previous?.let { (pat, p) -> " · " + stringResource(R.string.sf_previous, f(p.score), max, ", " + pat.atZone(zone).format(dm)) } ?: "")
                    }
                    IndexRow(inst.displayName, last?.let { "${f(it.second.score)}/$max" }, detail, open == inst.path, enabled = last != null) { open = if (open == inst.path) null else inst.path }
                    if (open == inst.path && obs.isNotEmpty()) Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val start = obs.first().first.minus(1, ChronoUnit.DAYS); val end = maxOf(obs.last().first, start.plus(2, ChronoUnit.DAYS))
                        LineChart(listOf(Series(inst.displayName, TherapyColors.navy, obs.map { (t, r) -> frac(t, start, end) to r.score.toFloat() })),
                            minY = if (inst == Api.Instrument.WAISR) 1f else 0f, maxY = max.toFloat())
                        obs.reversed().forEach { (t, r) ->
                            Row {
                                Text(t.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)), style = TherapyType.caption, color = TherapyColors.muted, modifier = Modifier.weight(1f))
                                Text("${f(r.score)}/$max", style = TherapyType.caption, color = TherapyColors.ink)
                            }
                        }
                    }
                    if (i < list.lastIndex) RowDivider()
                }
            }
        }
    }
}

private fun frac(t: Instant, start: Instant, end: Instant): Float {
    val span = ChronoUnit.SECONDS.between(start, end).toFloat().coerceAtLeast(1f)
    return (ChronoUnit.SECONDS.between(start, t).toFloat() / span).coerceIn(0f, 1f)
}

@Composable
private fun IndexRow(name: String, value: String?, detail: String?, open: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, style = TherapyType.emphasis, color = TherapyColors.ink)
            detail?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
        }
        Text(value ?: stringResource(R.string.rec_no_results), style = if (value != null) TherapyType.emphasis else TherapyType.caption,
            color = if (value != null) TherapyColors.ink else TherapyColors.neutral)
        if (enabled) Icon(if (open) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral,
            modifier = Modifier.padding(start = 6.dp).size(18.dp))
    }
}

// Detalhes --------------------------------------------------------------------

/** What used to crowd the top of the record: details, the client's app, the invitation, AI drafts. */
@Composable
private fun DetailsPage(patient: ApiPatient, onBack: () -> Unit, onNewNote: () -> Unit, onAiTools: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf<ApiInviteIssued?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackRow(onBack)
        Text(patient.name, style = TherapyType.display, color = TherapyColors.navy)
        CompactCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                patient.diagnosis?.takeIf { it.isNotBlank() && it != "—" }?.let { LabelledValue(stringResource(R.string.rec_diagnosis), it) }
                LabelledValue(stringResource(R.string.rec_risk_on_record), riskLabel(patient.riskLevel))
            }
        }
        // What the badge on the old header never said.
        Text(stringResource(R.string.rec_risk_footnote), style = TherapyType.caption, color = TherapyColors.muted)
        CompactCard {
            CompactLinkRow(Icons.Outlined.PersonOutline, stringResource(R.string.edit_client)) { editing = true }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.EditNote, stringResource(R.string.rec_note_outside), stringResource(R.string.rec_note_outside_sub), onClick = onNewNote)
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.AutoAwesome, stringResource(R.string.ai_section), onClick = onAiTools)
        }
        ClientFeaturesSection(patient.id)
        SectionLabel(stringResource(R.string.reissue_invite))
        CompactCard {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.reissue_invite_detail), style = TherapyType.caption, color = TherapyColors.muted)
                val issued = invite
                if (issued != null) {
                    Text(issued.inviteCode, style = TherapyType.display, color = TherapyColors.navy, modifier = Modifier.padding(top = 8.dp))
                    Text(ApiTimestamp.parse(issued.inviteExpiresAt)?.shortDate() ?: issued.inviteExpiresAt, style = TherapyType.caption, color = TherapyColors.muted)
                    TextButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", issued.inviteCode)) }) {
                        Text(stringResource(R.string.copy_code))
                    }
                } else TextButton({ scope.launch { runCatching { invite = container.api.reissueInvite(patient.id) }.onFailure { error = it.message } } }) {
                    Text(stringResource(R.string.reissue_invite))
                }
                error?.let { Text(it, style = TherapyType.caption, color = TherapyColors.critical) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (editing) EditClientDialog(patient) { saved -> editing = false; if (saved) onBack() }
}
