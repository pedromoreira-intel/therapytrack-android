package com.therapytrack.android.ui.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiGoal
import com.therapytrack.android.core.ApiSessionTopic
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.BellButton
import com.therapytrack.android.ui.common.CapsuleButton
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
import com.therapytrack.android.ui.common.SupportButton
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The client's day: how they are, what was agreed, what to bring next time.
 * Everything else — history, questionnaires, how the app works — is a link.
 *
 * Was a stack of cards: an explanation, a check-in nudge, scores, tasks, and a
 * red "Obter ajuda" pill, with the immediate things buried among them. Same
 * page as iOS PatientHomeView.
 */
@Composable
fun HomeScreen(
    state: ClientState, onActivity: () -> Unit, onCheckIn: () -> Unit, onCrisis: () -> Unit, onReload: () -> Unit,
    onTopics: () -> Unit, onBreathing: () -> Unit, onMyCheckIns: () -> Unit, onQuestionnaires: () -> Unit
) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var latest by remember { mutableStateOf<ApiEmaResponse?>(null) }
    var agreed by remember { mutableStateOf<List<ApiGoal>>(emptyList()) }
    var topics by remember { mutableStateOf<List<ApiSessionTopic>>(emptyList()) }
    var nextSession by remember { mutableStateOf<Instant?>(null) }
    var updating by remember { mutableStateOf(setOf<Int>()) }
    var offline by remember { mutableStateOf(false) }
    var howItWorks by remember { mutableStateOf(false) }
    val outboxTick by container.checkIns.changes.collectAsState()

    suspend fun loadGoals(patientId: Int) {
        runCatching { agreed = container.api.goals(patientId).sortedBy { it.completed } }
    }

    LaunchedEffect(state.patient, outboxTick) {
        try {
            latest = container.api.checkIns().maxByOrNull { ApiTimestamp.parse(it.doneAt) ?: Instant.EPOCH }
            offline = false
        } catch (e: Exception) { android.util.Log.w("Home", "check-ins: $e"); offline = true }
        state.patient?.let { loadGoals(it.id) }
        runCatching { topics = container.api.sessionTopics().open }
        runCatching {
            val now = Instant.now()
            nextSession = container.api.sessions().filter { !it.status.equals("cancelled", true) }
                .mapNotNull { ApiTimestamp.parse(it.sessionDate) }.filter { it >= now }.minOrNull()
        }
    }

    val name = state.patient?.name?.substringBefore(' ') ?: ""

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 4.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        // Support on the left and the bell on the right, as the iOS toolbar.
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SupportButton(onCrisis)
                Spacer(Modifier.weight(1f))
                BellButton(onActivity)
            }
            Text(stringResource(R.string.welcome_back), style = TherapyType.body, color = TherapyColors.muted)
            if (name.isNotEmpty()) Text(name, style = TherapyType.display, color = TherapyColors.navy, maxLines = 1)
        }
        if (offline) Muted(stringResource(R.string.offline_showing_saved))
        state.loadError?.let { Row(verticalAlignment = Alignment.CenterVertically) { Muted(stringResource(R.string.could_not_load)); TextButton(onReload) { Text(stringResource(R.string.refresh)) } } }

        StuckWorkBanner(refreshKey = outboxTick)

        CheckInSection(latest, onCheckIn)
        nextSession?.let { NextSessionLine(it) }

        if (agreed.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.agreed_in_session))
            CompactCard {
                agreed.forEachIndexed { i, goal ->
                    Row(Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            updating = updating + goal.id
                            runCatching { container.api.setGoalCompleted(goal.id, !goal.completed) }
                            state.patient?.let { loadGoals(it.id) }
                            updating = updating - goal.id
                        }
                    }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                        Icon(if (goal.completed) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            stringResource(if (goal.completed) R.string.goal_done else R.string.goal_not_done),
                            tint = if (goal.completed) TherapyColors.success else TherapyColors.neutral, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(goal.title, style = TherapyType.body, color = if (goal.completed) TherapyColors.muted else TherapyColors.ink,
                                textDecoration = if (goal.completed) TextDecoration.LineThrough else null)
                            goal.dueDate?.let { ApiTimestamp.parse(it) }?.let {
                                Text(stringResource(R.string.due_on, it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", appLocale()))),
                                    style = TherapyType.caption, color = TherapyColors.neutral)
                            }
                        }
                        if (goal.id in updating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TherapyColors.navy)
                    }
                    if (i < agreed.lastIndex) RowDivider(inset = 48.dp)
                }
            }
        }

        // What to bring to the next session (REA-22): the client's half of the therapy.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.for_next_session))
            CompactCard {
                topics.forEach { t ->
                    Text(t.text, style = TherapyType.body, color = TherapyColors.ink, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp))
                    RowDivider()
                }
                Row(Modifier.fillMaxWidth().clickable(onClick = onTopics).padding(horizontal = 14.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, null, tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (topics.isEmpty()) R.string.add_something_to_talk_about else R.string.add_or_edit), style = TherapyType.body, color = TherapyColors.navy)
                }
            }
        }

        CompactCard {
            CompactLinkRow(Icons.Outlined.Air, stringResource(R.string.a_breathing_exercise), stringResource(R.string.about_two_minutes_no_sound), onClick = onBreathing)
        }

        CompactCard {
            CompactLinkRow(Icons.Outlined.History, stringResource(R.string.my_check_ins), onClick = onMyCheckIns)
            RowDivider(inset = 48.dp)
            CompactLinkRow(Icons.Outlined.Assignment, stringResource(R.string.questionnaires), onClick = onQuestionnaires)
            RowDivider(inset = 48.dp)
            CompactLinkRow(Icons.Outlined.Info, stringResource(R.string.how_it_works)) { howItWorks = true }
        }
    }

    if (howItWorks) AlertDialog(
        onDismissRequest = { howItWorks = false },
        title = { Text(stringResource(R.string.how_it_works)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.how_it_works_records), style = TherapyType.bodyLarge)
                Text(stringResource(R.string.how_it_works_journal), style = TherapyType.bodyLarge)
                Text(stringResource(R.string.how_it_works_help), style = TherapyType.bodyLarge)
            }
        },
        confirmButton = { TextButton({ howItWorks = false }) { Text(stringResource(R.string.close)) } },
        containerColor = androidx.compose.ui.graphics.Color.White
    )
}

/** "Como se sente hoje?": an invitation, or, once done today, when it was saved and what it said. */
@Composable
private fun CheckInSection(latest: ApiEmaResponse?, onCheckIn: () -> Unit) {
    val at = latest?.let { ApiTimestamp.parse(it.doneAt) }
    val today = at?.atZone(ZoneId.systemDefault())?.toLocalDate() == LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.how_are_you_feeling_today))
        CompactCard {
            if (today && latest != null && at != null) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.entry_saved_at, at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))),
                            style = TherapyType.emphasis, color = TherapyColors.success)
                    }
                    Text(scoresLine(latest), style = TherapyType.body, color = TherapyColors.ink)
                    Text(stringResource(R.string.check_in_again), style = TherapyType.body, color = TherapyColors.navy,
                        modifier = Modifier.clickable(onClick = onCheckIn).padding(vertical = 6.dp))
                }
            } else {
                Row(Modifier.fillMaxWidth().clickable(onClick = onCheckIn).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.record_how_you_feel), style = TherapyType.emphasis, color = TherapyColors.ink)
                        Text(stringResource(R.string.three_questions_under_a_minute), style = TherapyType.caption, color = TherapyColors.muted)
                    }
                    CapsuleButton(stringResource(R.string.start), onClick = onCheckIn)
                }
            }
        }
    }
}

@Composable
fun scoresLine(e: ApiEmaResponse): String = listOfNotNull(
    e.mood?.let { stringResource(R.string.mood_of_7, it) },
    e.anxiety?.let { stringResource(R.string.anxiety_of_7, it) },
    e.sleep?.let { stringResource(R.string.sleep_of_7, it) }
).joinToString(" · ")

@Composable
private fun NextSessionLine(at: Instant) {
    val local = at.atZone(ZoneId.systemDefault())
    val day = if (local.toLocalDate() == LocalDate.now()) stringResource(R.string.today)
        else local.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", appLocale()))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CalendarToday, null, tint = TherapyColors.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.next_session_at, day, local.format(DateTimeFormatter.ofPattern("HH:mm"))), style = TherapyType.body, color = TherapyColors.muted)
    }
}

/** The language the app is showing, for dates written out in words. */
@Composable
fun appLocale(): Locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
