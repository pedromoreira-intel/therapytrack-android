package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiSession
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.noteFor
import com.therapytrack.android.ui.client.appLocale
import com.therapytrack.android.ui.common.BackRow
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.PageIconButton
import com.therapytrack.android.ui.common.PageTitle
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/**
 * The Agenda, as iOS ScheduleView: Dia / Semana / Mês to move through the
 * days, and the selected day's sessions as the rows from Hoje — time, who,
 * and once a session has ended, whether its note is written. A session opens
 * its Preparar/Nota screen; an ended one opens on Nota.
 */
@Composable
fun AgendaScreen(initialDay: LocalDate? = null, onBack: () -> Unit, onSession: (ApiSession, Boolean) -> Unit, onNew: (LocalDate) -> Unit) {
    val container = LocalContainer.current
    val zone = ZoneId.systemDefault()
    var mode by rememberSaveable { mutableStateOf("week") }
    var selectedText by rememberSaveable { mutableStateOf((initialDay ?: LocalDate.now()).toString()) }
    val selected = LocalDate.parse(selectedText)
    fun select(d: LocalDate) { selectedText = d.toString() }
    var sessions by remember { mutableStateOf<List<ApiSession>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var noted by remember { mutableStateOf(setOf<Int>()) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(selectedText, reload) {
        loading = true; failed = false
        try {
            // The local day's bounds, filtered again here on a real instant: the
            // server's date is UTC, which put a session just after midnight on the day before.
            val start = selected.atStartOfDay(zone).toInstant()
            val end = selected.plusDays(1).atStartOfDay(zone).toInstant()
            sessions = container.api.sessions(from = start, to = end)
                .filter { s -> ApiTimestamp.parse(s.sessionDate)?.let { it >= start && it < end } == true }
                .sortedBy { it.sessionDate }
        } catch (e: Exception) {
            // Cleared rather than kept: another day's sessions under this date are worse than an honest error.
            sessions = emptyList(); failed = true
        }
        loading = false
        // Which of the day's sessions already have their note.
        val found = mutableSetOf<Int>()
        sessions.groupBy { it.patientId }.forEach { (patient, list) ->
            val notes = runCatching { container.api.sessionNotes(patient) }.getOrNull() ?: return@forEach
            list.forEach { s ->
                val day = ApiTimestamp.parse(s.sessionDate)?.atZone(zone)?.toLocalDate()?.toString() ?: return@forEach
                if (notes.noteFor(s.id, day) != null) found += s.id
            }
        }
        noted = found
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(onBack)
        // The same title as Hoje and Clientes, with "+" where Clientes has it.
        PageTitle(stringResource(R.string.agenda_title)) { PageIconButton(Icons.Filled.Add, stringResource(R.string.new_session)) { onNew(selected) } }
        Segmented(listOf("day" to stringResource(R.string.mode_day), "week" to stringResource(R.string.mode_week), "month" to stringResource(R.string.mode_month)), mode, { mode = it })

        val locale = appLocale()
        when (mode) {
            "day" -> DateStepper(selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.titlecase(locale) },
                { select(selected.minusDays(1)) }, { select(selected.plusDays(1)) })
            "week" -> {
                val first = WeekFields.of(locale).firstDayOfWeek
                val start = selected.with(TemporalAdjusters.previousOrSame(first))
                val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
                DateStepper("${start.format(fmt)} – ${start.plusDays(6).format(fmt)}", { select(selected.minusWeeks(1)) }, { select(selected.plusWeeks(1)) })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0L..6L).map { start.plusDays(it) }.forEach { d -> DayCell(d, d == selected, Modifier.weight(1f)) { select(d) } }
                }
            }
            else -> MonthGrid(selected, ::select)
        }

        when {
            loading && sessions.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TherapyColors.navy) }
            failed -> Column {
                Text(stringResource(R.string.agenda_failed), style = TherapyType.body, color = TherapyColors.muted)
                TextButton({ reload++ }) { Text(stringResource(R.string.retry)) }
            }
            sessions.isEmpty() -> Text(stringResource(R.string.no_sessions_day), style = TherapyType.body, color = TherapyColors.muted, modifier = Modifier.padding(vertical = 8.dp))
            else -> CompactCard {
                sessions.forEachIndexed { i, s ->
                    SessionLine(s, s.id in noted, onSession)
                    if (i < sessions.lastIndex) RowDivider(80.dp)
                }
            }
        }
    }
}

/** Time, who, and — once it has ended — whether the note is written. */
@Composable
private fun SessionLine(s: ApiSession, noted: Boolean, onSession: (ApiSession, Boolean) -> Unit) {
    val at = ApiTimestamp.parse(s.sessionDate) ?: return
    val minutes = s.durationMinutes ?: 50
    val endAt = at.plusSeconds(60L * minutes)
    val now = Instant.now()
    val ended = endAt < now
    val cancelled = s.status == "cancelled"
    val hm = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.systemDefault()
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = !cancelled) { onSession(s, ended) }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(at.atZone(zone).format(hm), style = TherapyType.emphasis, color = if (ended || cancelled) TherapyColors.neutral else TherapyColors.navy, modifier = Modifier.width(52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(s.patientName ?: "#${s.patientId}", style = TherapyType.body, color = if (cancelled) TherapyColors.neutral else TherapyColors.ink,
                textDecoration = if (cancelled) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis)
            when {
                cancelled -> Text(stringResource(R.string.status_cancelled), style = TherapyType.caption, color = TherapyColors.neutral)
                // As on Hoje: the slot the clock is in.
                !ended && at <= now -> Text(stringResource(R.string.slot_until, endAt.atZone(zone).format(hm)), style = TherapyType.caption, color = TherapyColors.navy)
                else -> Text("$minutes min", style = TherapyType.caption, color = TherapyColors.neutral)
            }
            s.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        Spacer(Modifier.width(8.dp))
        when {
            cancelled -> {}
            ended && noted -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.note_done), style = TherapyType.caption, color = TherapyColors.success)
            }
            ended -> Row(Modifier.clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.08f)).clickable { onSession(s, true) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EditNote, null, tint = TherapyColors.navy, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.sf_note), style = TherapyType.emphasis, color = TherapyColors.navy)
            }
            else -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral, modifier = Modifier.size(18.dp))
        }
    }
}

/** ‹ label › in one row: how every date control here moves. */
@Composable
private fun DateStepper(label: String, back: () -> Unit, forward: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp, 36.dp).clip(CircleShape).clickable(onClick = back), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous), tint = TherapyColors.navy)
        }
        Text(label, style = TherapyType.emphasis, color = TherapyColors.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp, 36.dp).clip(CircleShape).clickable(onClick = forward), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next), tint = TherapyColors.navy)
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val locale = appLocale()
    val today = d == LocalDate.now()
    Column(modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(12.dp))
        .background(if (selected) TherapyColors.navy else if (today) TherapyColors.navy.copy(alpha = 0.08f) else Color.Transparent)
        .clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), style = TherapyType.caption, color = if (selected) Color.White.copy(alpha = 0.85f) else TherapyColors.muted)
        Text("${d.dayOfMonth}", style = TherapyType.emphasis, color = if (selected) Color.White else if (today) TherapyColors.navy else TherapyColors.ink)
    }
}

/** The month as a grid, weeks starting where the calendar of the app's language does. */
@Composable
private fun MonthGrid(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val locale = appLocale()
    val first = WeekFields.of(locale).firstDayOfWeek
    val monthStart = selected.withDayOfMonth(1)
    DateStepper(monthStart.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
        { onSelect(selected.minusMonths(1)) }, { onSelect(selected.plusMonths(1)) })
    val days = (0 until 7).map { DayOfWeek.of(((first.value - 1 + it) % 7) + 1) }
    Row { days.forEach { Text(it.getDisplayName(TextStyle.NARROW, locale), style = TherapyType.caption, color = TherapyColors.muted,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)) } }
    val offset = (monthStart.dayOfWeek.value - first.value + 7) % 7
    val cells = List(offset) { null } + (1..selected.lengthOfMonth()).map { monthStart.withDayOfMonth(it) }
    cells.chunked(7).forEach { week ->
        Row {
            (0 until 7).forEach { i ->
                val d = week.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(1.2f), contentAlignment = Alignment.Center) {
                    if (d != null) {
                        val on = d == selected; val today = d == LocalDate.now()
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (on) TherapyColors.navy else if (today) TherapyColors.navy.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable { onSelect(d) }, contentAlignment = Alignment.Center) {
                            Text("${d.dayOfMonth}", style = TherapyType.body, color = if (on) Color.White else if (today) TherapyColors.navy else TherapyColors.ink)
                        }
                    }
                }
            }
        }
    }
}
