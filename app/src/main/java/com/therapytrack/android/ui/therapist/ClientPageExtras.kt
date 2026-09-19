package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.Api
import com.therapytrack.android.core.ApiAssessmentResult
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiGoal
import com.therapytrack.android.core.ApiJournalEntry
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.LineChart
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.Series
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

val goalCategories = listOf("general" to R.string.gc_general, "homework" to R.string.gc_homework, "behavioral" to R.string.gc_behavioral, "emotional" to R.string.gc_emotional)

/** Goals and homework: the between-session work the therapist sets and the client ticks off. */
@Composable
fun GoalsSection(patientId: Int) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var goals by remember { mutableStateOf<List<ApiGoal>>(emptyList()) }
    var reload by remember { mutableStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("homework") }
    var due by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(patientId, reload) { runCatching { goals = container.api.goals(patientId) } }

    SectionTitle(stringResource(R.string.goals_section))
    TextButton({ adding = true }) { Text(stringResource(R.string.new_goal)) }
    if (goals.isEmpty()) Muted(stringResource(R.string.no_goals))
    goals.sortedBy { it.completed }.forEach { g ->
        Card {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (g.completed) TextDecoration.LineThrough else null)
                    g.description?.takeIf { it.isNotBlank() }?.let { Muted(it) }
                    Muted(listOfNotNull(stringResource(goalCategories.firstOrNull { it.first == g.category }?.second ?: R.string.gc_general),
                        g.dueDate?.let { stringResource(R.string.due, ApiTimestamp.parse(it)?.shortDate() ?: it) }).joinToString(" · "))
                }
                Pill(stringResource(if (g.completed) R.string.mark_done else R.string.st_pending), if (g.completed) TherapyColors.success else TherapyColors.warning)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton({ scope.launch { runCatching { container.api.deleteGoal(g.id) }; reload++ } }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                TextButton({ scope.launch { runCatching { container.api.setGoalCompleted(g.id, !g.completed) }; reload++ } }) { Text(stringResource(if (g.completed) R.string.reopen else R.string.mark_done)) }
            }
        }
    }
    if (adding) AlertDialog(onDismissRequest = { adding = false }, title = { Text(stringResource(R.string.new_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.goal_title)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.description)) })
                ChipRow(goalCategories, category) { category = it }
                OutlinedTextField(due, { due = it }, label = { Text(stringResource(R.string.due_date)) }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
            // Captured before the fields are cleared: the coroutine runs after this lambda returns.
            val (t, d, c, dd) = listOf(title, description.ifBlank { "" }, category, due)
            scope.launch { runCatching { container.api.createGoal(patientId, t, d.ifBlank { null }, c, dd.ifBlank { null }) }; reload++ }
            adding = false; title = ""; description = ""; due = ""
        }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ adding = false }) { Text(stringResource(R.string.cancel)) } })
}

private fun xOf(t: Instant?, start: Instant, end: Instant): Float? {
    t ?: return null
    val span = ChronoUnit.SECONDS.between(start, end).toFloat().coerceAtLeast(1f)
    return (ChronoUnit.SECONDS.between(start, t).toFloat() / span).coerceIn(0f, 1f)
}

/** Assessment history and the 30-day check-in series, drawn without a charting library. */
@Composable
fun HistorySection(patientId: Int) {
    val container = LocalContainer.current
    var results by remember { mutableStateOf<Map<Api.Instrument, List<ApiAssessmentResult>>>(emptyMap()) }
    var checkIns by remember { mutableStateOf<List<ApiEmaResponse>>(emptyList()) }
    LaunchedEffect(patientId) {
        results = Api.Instrument.entries.associateWith { runCatching { container.api.assessmentResults(it, patientId) }.getOrDefault(emptyList()) }
        runCatching { checkIns = container.api.checkIns(patientId) }
    }
    SectionTitle(stringResource(R.string.history_section))

    val now = Instant.now()
    Card {
        Text(stringResource(R.string.check_in_chart), style = MaterialTheme.typography.titleMedium)
        val start = now.minus(30, ChronoUnit.DAYS)
        val recent = checkIns.mapNotNull { c -> ApiTimestamp.parse(c.doneAt)?.takeIf { it.isAfter(start) }?.let { it to c } }.sortedBy { it.first }
        if (recent.isEmpty()) Muted(stringResource(R.string.no_history))
        else LineChart(listOf(
            Series(stringResource(R.string.legend_mood), TherapyColors.navy, recent.mapNotNull { (t, c) -> c.mood?.let { xOf(t, start, now)!! to it.toFloat() } }),
            Series(stringResource(R.string.legend_anxiety), TherapyColors.warning, recent.mapNotNull { (t, c) -> c.anxiety?.let { xOf(t, start, now)!! to it.toFloat() } }),
            Series(stringResource(R.string.legend_sleep), TherapyColors.success, recent.mapNotNull { (t, c) -> c.sleep?.let { xOf(t, start, now)!! to it.toFloat() } })
        ), minY = 0f, maxY = 10f)
    }

    listOf(Api.Instrument.PHQ9 to 27f, Api.Instrument.GAD7 to 21f, Api.Instrument.WAISR to 5f).forEach { (inst, max) ->
        val rows = results[inst].orEmpty().mapNotNull { r -> ApiTimestamp.parse(r.assessmentDate)?.let { it to r } }.sortedBy { it.first }
        if (rows.isEmpty()) return@forEach
        Card {
            Text(inst.displayName, style = MaterialTheme.typography.titleMedium)
            val start = rows.first().first.minus(1, ChronoUnit.DAYS); val end = maxOf(rows.last().first, start.plus(2, ChronoUnit.DAYS))
            // Severity bands: mild/moderate/severe thresholds shaded so the line reads clinically.
            val bands = when (inst) {
                Api.Instrument.PHQ9 -> listOf(Triple(10f, 15f, TherapyColors.warning.copy(alpha = 0.10f)), Triple(15f, 27f, TherapyColors.critical.copy(alpha = 0.10f)))
                Api.Instrument.GAD7 -> listOf(Triple(10f, 15f, TherapyColors.warning.copy(alpha = 0.10f)), Triple(15f, 21f, TherapyColors.critical.copy(alpha = 0.10f)))
                Api.Instrument.WAISR -> listOf(Triple(1f, 3f, TherapyColors.critical.copy(alpha = 0.10f)))
            }
            LineChart(listOf(Series(inst.displayName, TherapyColors.navy, rows.map { (t, r) -> xOf(t, start, end)!! to r.score.toFloat() })),
                minY = if (inst == Api.Instrument.WAISR) 1f else 0f, maxY = max, bands = bands)
            Muted(rows.joinToString(" · ") { (t, r) -> "${t.shortDate()}: ${if (inst == Api.Instrument.WAISR) String.format("%.1f", r.score) else r.score.toInt()}" })
        }
    }
}

@Composable
fun SharedJournalSection(patientId: Int) {
    val container = LocalContainer.current
    var entries by remember { mutableStateOf<List<ApiJournalEntry>>(emptyList()) }
    LaunchedEffect(patientId) { runCatching { entries = container.api.sharedJournal(patientId) } }
    SectionTitle(stringResource(R.string.shared_journal_section))
    if (entries.isEmpty()) Muted(stringResource(R.string.no_shared_entries))
    entries.forEach { e ->
        Card {
            Muted(ApiTimestamp.parse(e.createdAt)?.shortDateTime() ?: e.createdAt)
            e.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(e.content)
        }
    }
}

val clientFeatureLabels = listOf("ema" to R.string.cf_ema, "assessments" to R.string.cf_assessments, "journal" to R.string.cf_journal,
    "messaging" to R.string.cf_messaging, "symptom_reporting" to R.string.cf_symptom_reporting)

/** Per-client switches for what their app offers. Written one at a time; the row reflects the server's answer. */
@Composable
fun ClientFeaturesSection(patientId: Int) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var features by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    LaunchedEffect(patientId) { runCatching { features = container.api.clientFeatures(patientId) } }
    SectionTitle(stringResource(R.string.client_app_section))
    Card {
        val f = features
        if (f == null) { Muted(stringResource(R.string.loading)); return@Card }
        clientFeatureLabels.forEach { (key, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), Modifier.weight(1f))
                Switch(f[key] ?: true, { on -> scope.launch { runCatching { container.api.setClientFeature(patientId, key, on); features = container.api.clientFeatures(patientId) } } })
            }
        }
    }
}

val clientStatuses = listOf("ACTIVE" to R.string.cs_active, "STABLE" to R.string.cs_stable, "AT_RISK" to R.string.cs_at_risk, "DISCHARGED" to R.string.cs_discharged)

@Composable
fun EditClientDialog(patient: ApiPatient, onDone: (Boolean) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var diagnosis by rememberSaveable { mutableStateOf(patient.diagnosis?.takeIf { it != "—" } ?: "") }
    var status by rememberSaveable { mutableStateOf(patient.status.uppercase()) }
    var risk by rememberSaveable { mutableStateOf(patient.riskLevel.uppercase()) }
    AlertDialog(onDismissRequest = { onDone(false) }, title = { Text(stringResource(R.string.edit_client)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(diagnosis, { diagnosis = it }, label = { Text(stringResource(R.string.diagnosis)) }, singleLine = true)
                Muted(stringResource(R.string.client_status)); ChipRow(clientStatuses, status) { status = it }
                Muted(stringResource(R.string.risk_level)); ChipRow(listOf("LOW" to R.string.risk_low, "MEDIUM" to R.string.risk_medium, "HIGH" to R.string.risk_high), risk) { risk = it }
            }
        },
        confirmButton = { TextButton({ scope.launch { runCatching { container.api.updatePatient(patient.id, diagnosis.ifBlank { null }, status, risk) }; onDone(true) } }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ onDone(false) }) { Text(stringResource(R.string.cancel)) } })
}
