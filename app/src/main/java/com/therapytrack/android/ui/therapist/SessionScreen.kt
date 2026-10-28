package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiBrief
import com.therapytrack.android.core.ApiClinicalAlert
import com.therapytrack.android.core.ApiSessionNote
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.BriefMeasure
import com.therapytrack.android.core.noteFor
import com.therapytrack.android.ui.client.appLocale
import com.therapytrack.android.ui.common.BackRow
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.LabelledValue
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * One session, before and after: what to prepare, then the note. Same as iOS
 * SessionPrepView. Opened from its row on Hoje or the Agenda.
 *
 * "Preparar" is the brief read quietly: what was planned, what the client
 * wants to raise, the questionnaires, and any answer still to review —
 * recorded as reviewed here, by the therapist, never by opening the page.
 * "Nota" is this session's note: the one linked to the session, or for older
 * notes the one filed that day.
 *
 * [sessionAt] is null when there is no session to prepare (opened for a
 * review): then there are no tabs and no note to write from here.
 */
@Composable
fun SessionScreen(patientId: Int, patientName: String, sessionAt: Instant?, durationMinutes: Int?, sessionId: Int?, startOnNote: Boolean = false,
                  onBack: () -> Unit, onHistory: () -> Unit, onSeePlan: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var noteTab by rememberSaveable { mutableStateOf(startOnNote) }
    var brief by remember { mutableStateOf<ApiBrief?>(null) }
    var failed by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf<List<ApiSessionNote>>(emptyList()) }
    // Alerts as first loaded: a reviewed one stays on the page, marked reviewed, rather than vanishing under the thumb.
    var alerts by remember { mutableStateOf<List<ApiClinicalAlert>>(emptyList()) }
    var reviewed by remember { mutableStateOf(setOf<Int>()) }
    var reviewing by remember { mutableStateOf(setOf<Int>()) }
    var reviewError by remember { mutableStateOf<String?>(null) }
    var writing by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        try {
            val loaded = container.api.brief(patientId)
            brief = loaded
            val kept = alerts.filter { it.id in reviewed }
            alerts = kept + loaded.openAlerts.filter { a -> kept.none { it.id == a.id } }
            failed = false
            // Opening it is reading what the client added.
            runCatching { container.api.markNotificationsRead("client_record", patientId) }
        } catch (e: Exception) { if (brief == null) failed = true }
        runCatching { notes = container.api.sessionNotes(patientId) }
    }

    val zone = ZoneId.systemDefault()
    val day = sessionAt?.atZone(zone)?.toLocalDate()?.toString()
    val sessionNote = day?.let { notes.noteFor(sessionId, it) }

    if (writing && sessionAt != null) {
        NoteEditorScreen(patientId, patientName, sessionAt, sessionId, onSeePlan = onSeePlan, onClose = {
            writing = false
            // Filed: back on this session's Nota, showing what was written.
            scope.launch {
                runCatching { notes = container.api.sessionNotes(patientId) }
                if (notes.noteFor(sessionId, day!!) != null) noteTab = true
                reload++
            }
        })
        return
    }

    Column(Modifier.fillMaxSize().background(TherapyColors.canvas)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BackRow(onBack)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(patientName, style = TherapyType.display, color = TherapyColors.navy)
                Row {
                    Text(whenLine(sessionAt, durationMinutes), style = TherapyType.body, color = TherapyColors.muted)
                    val inSlot = sessionAt != null && Instant.now().let { it >= sessionAt && it < sessionAt.plusSeconds(60L * (durationMinutes ?: 50)) }
                    if (inSlot) Text(" · " + stringResource(R.string.slot_in_progress), style = TherapyType.body, color = TherapyColors.navy)
                }
            }
            if (sessionAt != null) Segmented(listOf("prepare" to stringResource(R.string.sf_prepare), "note" to stringResource(R.string.sf_note)),
                if (noteTab) "note" else "prepare", { noteTab = it == "note" })

            val b = brief
            when {
                b == null && failed -> Column {
                    Text(stringResource(R.string.sf_build_failed), style = TherapyType.body, color = TherapyColors.muted)
                    TextButton({ reload++ }) { Text(stringResource(R.string.retry)) }
                }
                b == null -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TherapyColors.navy) }
                noteTab -> NoteTab(sessionNote)
                else -> Prepare(b, sessionAt == null, alerts, reviewed, reviewing, reviewError, onHistory) { alert ->
                    reviewing = reviewing + alert.id; reviewError = null
                    scope.launch {
                        try { container.api.acknowledgeAlert(alert.id); reviewed = reviewed + alert.id }
                        catch (e: Exception) { reviewError = context.getString(R.string.sf_ack_failed) }
                        reviewing = reviewing - alert.id
                    }
                }
            }
        }

        // The one action: write this session's note, or go to it once filed.
        if (sessionAt != null && brief != null && !(noteTab && sessionNote != null)) {
            Text(stringResource(if (sessionNote != null) R.string.sf_see_note else R.string.sf_write_note), style = TherapyType.emphasis, color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(20.dp, 8.dp, 20.dp, 12.dp).clip(RoundedCornerShape(14.dp)).background(TherapyColors.navy)
                    .clickable { if (sessionNote != null) noteTab = true else writing = true }.padding(vertical = 14.dp))
        }
    }
}

@Composable
private fun whenLine(at: Instant?, minutes: Int?): String {
    val locale = appLocale()
    if (at == null) return stringResource(R.string.no_session_scheduled)
    val local = at.atZone(ZoneId.systemDefault())
    val time = local.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val day = if (local.toLocalDate() == LocalDate.now()) stringResource(R.string.today)
        else local.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale)).replaceFirstChar { it.titlecase(locale) }
    return "$day, $time" + (minutes?.let { " · $it min" } ?: "")
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        CompactCard(content = content)
    }
}

/** Quiet sections in the record's style, each a fact, not a tile. */
@Composable
private fun Prepare(b: ApiBrief, noSession: Boolean, alerts: List<ApiClinicalAlert>, reviewed: Set<Int>, reviewing: Set<Int>, reviewError: String?,
                    onHistory: () -> Unit, onReview: (ApiClinicalAlert) -> Unit) {
    val locale = appLocale()
    val dm = DateTimeFormatter.ofPattern("dd/MM", locale)
    fun date(s: String?) = s?.let { ApiTimestamp.parse(it) }?.atZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(date(b.lastSession?.date)?.let { stringResource(R.string.sf_since_last_on, it.format(DateTimeFormatter.ofPattern("d MMMM", locale))) }
            ?: stringResource(R.string.sf_last_30_days), style = TherapyType.caption, color = TherapyColors.muted)

        Group(stringResource(if (noSession) R.string.sf_for_next_session else R.string.sf_where_left_off)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val last = b.lastSession
                if (last == null) Text(stringResource(R.string.sf_no_previous), style = TherapyType.body, color = TherapyColors.muted)
                else {
                    if (last.plan.isNullOrBlank() && last.focus.isNullOrBlank() && last.homeworkSet.isNullOrBlank())
                        Text(stringResource(R.string.sf_nothing_planned), style = TherapyType.body, color = TherapyColors.muted)
                    date(last.date)?.let { d ->
                        Text(listOfNotNull(d.format(DateTimeFormatter.ofPattern("EEE, dd/MM", locale)), last.sessionNumber?.let { stringResource(R.string.sf_session_n, it) }).joinToString(" · "),
                            style = TherapyType.caption, color = TherapyColors.muted)
                    }
                    last.plan?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_agreed_next_steps), it, emphasis = true) }
                    last.focus?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_focus), it) }
                    last.homeworkSet?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_homework_set), it) }
                }
            }
        }

        if (b.clientTopics.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Group(stringResource(R.string.sf_client_wants)) {
                b.clientTopics.forEachIndexed { i, topic ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(topic.text, style = TherapyType.body, color = TherapyColors.ink)
                        date(topic.createdAt)?.let { Text(it.format(dm), style = TherapyType.caption, color = TherapyColors.neutral) }
                    }
                    if (i < b.clientTopics.lastIndex) RowDivider()
                }
            }
            Text(stringResource(R.string.sf_tick_in_note), style = TherapyType.caption, color = TherapyColors.neutral)
        }

        // One row per instrument with a result; answers to review sit under the PHQ-9 they were given on.
        Group(stringResource(R.string.sf_questionnaires)) {
            val rows = listOf(Triple("PHQ-9", "phq9", 27), Triple("GAD-7", "gad7", 21), Triple("WAI-SR", "wai_sr", 5))
            val measured = rows.filter { b.measures[it.second]?.latest != null }
            val unmeasured = rows.filter { b.measures[it.second]?.latest == null }.map { it.first }
            measured.forEach { (name, key, max) ->
                QuestionnaireLine(name, b.measures.getValue(key), max, if (key == "wai_sr") 1 else 0)
                if (key == "phq9") alerts.filter { it.alertType == "suicidal_ideation" }.forEach {
                    ReviewBlock(it, it.id in reviewed, it.id in reviewing, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) { onReview(it) }
                }
                RowDivider()
            }
            alerts.filter { it.alertType != "suicidal_ideation" }.forEach {
                ReviewBlock(it, it.id in reviewed, it.id in reviewing, Modifier.padding(14.dp)) { onReview(it) }
            }
            reviewError?.let { Text(it, style = TherapyType.caption, color = TherapyColors.critical, modifier = Modifier.padding(horizontal = 14.dp)) }
            Text(when {
                unmeasured.isEmpty() -> stringResource(R.string.sf_all_three)
                measured.isEmpty() -> stringResource(R.string.sf_no_questionnaires)
                else -> stringResource(R.string.sf_not_completed, unmeasured.joinToString(", "))
            }, style = TherapyType.caption, color = TherapyColors.neutral, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }

        // Concerns not already shown as an answer to review above.
        val alerted = alerts.filter { it.id !in reviewed }.map { "${it.alertType}:${it.source}" }.toSet()
        val concerns = b.trajectory.filter { "${it.type}:${it.instrument}" !in alerted }
        if (concerns.isNotEmpty()) Group(stringResource(R.string.sf_trajectory)) {
            concerns.forEach { c ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(reviewTitle(c.type)), style = TherapyType.emphasis, color = TherapyColors.ink)
                    Text(c.detail, style = TherapyType.caption, color = TherapyColors.muted)
                }
            }
        }

        // Facts, each with where to look: no averages of a handful of check-ins.
        Group(stringResource(R.string.sf_since_last)) {
            val n = b.checkIns.count
            Row(Modifier.fillMaxWidth().clickable(enabled = n > 0, onClick = onHistory).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sf_check_ins), style = TherapyType.emphasis, color = TherapyColors.ink)
                    val count = if (n == 0) stringResource(R.string.sf_none_since) else if (n == 1) stringResource(R.string.sf_one_check_in) else stringResource(R.string.sf_n_check_ins, n)
                    val latest = date(b.checkIns.latest)?.takeIf { n > 0 }?.let { " · " + stringResource(R.string.sf_latest, it.format(DateTimeFormatter.ofPattern("dd/MM, HH:mm", locale))) } ?: ""
                    Text(count + latest, style = TherapyType.caption, color = TherapyColors.muted)
                }
                if (n > 0) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral, modifier = Modifier.size(18.dp))
            }
            RowDivider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.sf_homework), style = TherapyType.emphasis, color = TherapyColors.ink)
                Text(when {
                    b.homework.outstandingCount > 0 -> stringResource(R.string.sf_outstanding, b.homework.outstanding.joinToString(", ") { it.title })
                    b.homework.completedTotal > 0 -> stringResource(R.string.sf_all_complete)
                    else -> stringResource(R.string.sf_none_assigned)
                }, style = TherapyType.caption, color = TherapyColors.muted)
            }
            val latestJournal = b.journal.latest
            if (b.journal.sharedCount > 0 && latestJournal != null) {
                RowDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.sf_journal), style = TherapyType.emphasis, color = TherapyColors.ink)
                    Text(latestJournal.excerpt.ifBlank { latestJournal.title ?: "—" }, style = TherapyType.caption, color = TherapyColors.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        CompactCard { CompactLinkRow(Icons.Filled.History, stringResource(R.string.sf_see_history), onClick = onHistory) }
    }
}

@Composable
private fun QuestionnaireLine(name: String, m: BriefMeasure, max: Int, decimals: Int) {
    val locale = appLocale()
    fun fmt(v: Double) = String.format(locale, "%.${decimals}f", v)
    fun dm(s: String?) = s?.let { ApiTimestamp.parse(it) }?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd/MM", locale))
    val latest = m.latest ?: return
    val detail = buildList {
        latest.severity?.takeIf { it.isNotBlank() }?.let { add(stringResource(severityBand(it)).lowercase(locale)) }
        dm(latest.date)?.let { add(it) }
        m.previous?.let { p -> add(stringResource(R.string.sf_previous, fmt(p.score), max, dm(p.date)?.let { ", $it" } ?: "")) }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = TherapyType.emphasis, color = TherapyColors.ink)
            Text(detail.joinToString(" · "), style = TherapyType.caption, color = TherapyColors.muted)
        }
        Text("${fmt(latest.score)}/$max", style = TherapyType.emphasis, color = TherapyColors.ink)
    }
}

private fun severityBand(key: String) = when (key) {
    "minimal" -> R.string.band_minimal; "mild" -> R.string.band_mild; "moderate" -> R.string.band_moderate
    "moderately_severe" -> R.string.band_moderately_severe; "severe" -> R.string.band_severe
    else -> R.string.sf_worth_reviewing
}

private fun reviewTitle(type: String) = when (type) {
    "suicidal_ideation" -> R.string.sf_item9
    "deterioration" -> R.string.sf_getting_worse
    "alliance_rupture", "low_alliance" -> R.string.sf_alliance_rupture
    "not_on_track", "no_progress" -> R.string.sf_not_on_track
    else -> R.string.sf_worth_reviewing
}

/**
 * An answer or change still to review: what it is in one line, the full
 * wording on request, and the therapist's own record that it was seen.
 */
@Composable
private fun ReviewBlock(alert: ApiClinicalAlert, done: Boolean, busy: Boolean, modifier: Modifier, onReview: () -> Unit) {
    var open by remember(alert.id) { mutableStateOf(false) }
    val item9 = alert.alertType == "suicidal_ideation"
    // "Item 9: vários dias. PHQ-9 14." reads as "Resposta assinalada: vários dias." — the score is on the row above.
    val line = alert.summary?.takeIf { item9 && it.startsWith("Item 9: ") }
        ?.removePrefix("Item 9: ")?.substringBefore(". ")?.trimEnd('.')?.takeIf { it.isNotEmpty() }
        ?.let { stringResource(R.string.sf_answer_given, it) } ?: (alert.summary ?: alert.detail)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(1.5.dp)).background(if (done) TherapyColors.hairline else TherapyColors.champagne))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(reviewTitle(alert.alertType)), style = TherapyType.emphasis, color = TherapyColors.ink)
                Text(line, style = TherapyType.body, color = TherapyColors.muted)
                if (open) {
                    Text(alert.detail, style = TherapyType.body, color = TherapyColors.ink, modifier = Modifier.padding(top = 4.dp))
                }
                Row(Modifier.heightIn(min = 32.dp).clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (item9) (if (open) R.string.sf_hide_answer else R.string.sf_see_answer) else (if (open) R.string.sf_hide_details else R.string.sf_see_details)),
                        style = TherapyType.body, color = TherapyColors.navy)
                    Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.navy, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (done) {
                Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.sf_reviewed), style = TherapyType.caption, color = TherapyColors.success)
            } else Text(stringResource(R.string.sf_not_reviewed), style = TherapyType.caption, color = TherapyColors.neutral)
            Spacer(Modifier.weight(1f))
            if (!done) Row(Modifier.heightIn(min = 44.dp).clickable(enabled = !busy, onClick = onReview), verticalAlignment = Alignment.CenterVertically) {
                if (busy) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = TherapyColors.navy); Spacer(Modifier.width(6.dp)) }
                Text(stringResource(R.string.sf_record_review), style = TherapyType.emphasis, color = TherapyColors.navy)
            }
        }
    }
}

/** What was filed, field by field, in the record's style. */
@Composable
private fun NoteTab(note: ApiSessionNote?) {
    if (note == null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.sf_no_note_yet), style = TherapyType.body, color = TherapyColors.muted)
            Text(stringResource(R.string.sf_draft_kept), style = TherapyType.caption, color = TherapyColors.neutral)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(note.riskLevel?.let { stringResource(R.string.sf_filed_risk, stringResource(riskWord(it))) } ?: stringResource(R.string.sf_filed),
                style = TherapyType.caption, color = TherapyColors.success)
        }
        listOf(R.string.sf_focus to note.focus, R.string.sf_interventions to note.interventions, R.string.sf_notes to note.progressNotes,
            R.string.sf_homework to note.homework, R.string.sf_next_session to note.nextSessionPlan).forEach { (title, value) ->
            if (!value.isNullOrBlank()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(title))
                CompactCard { Text(value, style = TherapyType.bodyLarge, color = TherapyColors.ink, modifier = Modifier.padding(14.dp)) }
            }
        }
    }
}

private fun riskWord(level: String) = when (level.uppercase()) {
    "CRITICAL" -> R.string.risk_word_critical; "HIGH" -> R.string.risk_word_high
    "MODERATE", "MEDIUM" -> R.string.risk_word_moderate; else -> R.string.risk_word_low
}
