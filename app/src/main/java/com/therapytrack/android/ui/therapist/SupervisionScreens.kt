package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiSupervisionSession
import com.therapytrack.android.core.ApiSupervisor
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.PendingSupervision
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

val supervisionTypes = listOf("individual" to R.string.sv_individual, "group" to R.string.sv_group, "peer" to R.string.sv_peer)

@Composable
fun SupervisionScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<ApiSupervisionSession>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingSupervision>>(emptyList()) }
    var supervisors by remember { mutableStateOf<List<ApiSupervisor>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var form by rememberSaveable { mutableStateOf("") }   // "", "session", "supervisor"
    var deleting by remember { mutableStateOf<ApiSupervisionSession?>(null) }
    val outboxTick by container.supervision.changes.collectAsState()

    LaunchedEffect(reload, outboxTick) {
        pending = container.supervision.mine()
        failed = runCatching { sessions = container.api.supervisionSessions(); supervisors = container.api.supervisors() }.isFailure
    }

    when (form) {
        "session" -> { SupervisionForm { form = ""; reload++ }; return }
        "supervisor" -> { SupervisorForm { form = ""; reload++ }; return }
    }

    val year = LocalDate.now().year
    val hoursThisYear = sessions.filter { it.sessionDate.startsWith("$year") }.sumOf { it.hours }

    ListScreen(stringResource(R.string.supervision_title), onBack, failed, { reload++ }, stringResource(R.string.log_supervision),
        form = null, showForm = false, setShowForm = {}) {
        PrimaryButton(stringResource(R.string.log_supervision)) { form = "session" }
        Muted(stringResource(R.string.hours_this_year, fmtHours(hoursThisYear)))
        pending.forEach { p ->
            Card(tint = TherapyColors.champagne.copy(alpha = 0.25f)) {
                Row { Text(p.supervisorName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Pill(stringResource(R.string.waiting_to_send), TherapyColors.warning) }
                Muted("${p.date} · ${fmtHours(p.hours)} h")
                p.topics?.let { Text(it) }
            }
        }
        if (sessions.isEmpty() && pending.isEmpty() && !failed) Muted(stringResource(R.string.no_supervision))
        sessions.forEach { s ->
            Card {
                Row(Modifier.fillMaxWidth()) {
                    Text(s.supervisorName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill(stringResource(supervisionTypes.firstOrNull { it.first == s.supervisionType }?.second ?: R.string.sv_individual), TherapyColors.navy)
                }
                Muted("${ApiTimestamp.parse(s.sessionDate)?.shortDate() ?: s.sessionDate} · ${fmtHours(s.hours)} h" + (s.rating?.let { " · $it/5" } ?: ""))
                s.topics?.takeIf { it.isNotBlank() }?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
                s.notes?.takeIf { it.isNotBlank() }?.let { Muted(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ deleting = s }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                }
            }
        }

        SectionTitle(stringResource(R.string.supervisors_title))
        TextButton({ form = "supervisor" }) { Text(stringResource(R.string.add_supervisor)) }
        if (supervisors.isEmpty() && !failed) Muted(stringResource(R.string.no_supervisors))
        supervisors.forEach { sv ->
            Card {
                Text(sv.professionalName, style = MaterialTheme.typography.titleMedium)
                Muted(listOfNotNull(sv.credentials, sv.specialization, if (sv.isOnline) stringResource(R.string.offers_online) else null,
                    sv.hourlyRate?.let { "${fmtHours(it)} €/h" }, sv.contactEmail).joinToString(" · "))
            }
        }
    }
    deleting?.let { s ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.delete)) }, text = { Text(s.supervisorName + " · " + s.sessionDate) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.deleteSupervisionSession(s.id) }; reload++ }; deleting = null }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

/** Goes through the outbox: the reflection is the therapist's own writing. */
@Composable
private fun SupervisionForm(onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var credentials by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("individual") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var hours by rememberSaveable { mutableStateOf("1") }
    var topics by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<SaveOutcome?>(null) }
    val dateOk = runCatching { LocalDate.parse(date) }.isSuccess
    val ratingOk = rating.isBlank() || rating.toIntOrNull() in 1..5

    outcome?.let { o ->
        if (o != SaveOutcome.NOT_STORED) {
            FormFrame(stringResource(R.string.log_supervision), onCancel = onDone, canSave = true, busy = false, error = null, onSave = onDone, saveLabel = stringResource(R.string.done)) {
                val tint = if (o == SaveOutcome.SENT) TherapyColors.success else TherapyColors.warning
                Card(tint = tint.copy(alpha = 0.15f)) {
                    Text(stringResource(if (o == SaveOutcome.SENT) R.string.supervision_saved else R.string.supervision_queued), color = tint, style = MaterialTheme.typography.titleMedium)
                }
            }
            return
        }
    }

    FormFrame(stringResource(R.string.log_supervision), onCancel = onDone, busy = busy, error = null,
        canSave = name.isNotBlank() && dateOk && (hours.toDoubleOrNull() ?: -1.0) > 0 && ratingOk, onSave = {
            busy = true
            scope.launch {
                outcome = container.supervision.save(name, credentials.ifBlank { null }, type, date, hours.toDoubleOrNull() ?: 1.0,
                    topics.ifBlank { null }, notes.ifBlank { null }, rating.toIntOrNull())
                busy = false
            }
        }) {
        if (outcome == SaveOutcome.NOT_STORED) Card(tint = TherapyColors.critical.copy(alpha = 0.15f)) { Text(stringResource(R.string.supervision_not_stored), color = TherapyColors.critical) }
        Field(name, { name = it }, R.string.supervisor_name)
        Field(credentials, { credentials = it }, R.string.supervisor_credentials)
        Muted(stringResource(R.string.supervision_type))
        ChipRow(supervisionTypes, type) { type = it }
        Field(date, { date = it }, R.string.session_date, isError = !dateOk)
        Field(hours, { hours = it }, R.string.hours, keyboard = KeyboardType.Decimal)
        Field(topics, { topics = it }, R.string.topics)
        Field(notes, { notes = it }, R.string.reflection, single = false)
        Field(rating, { rating = it }, R.string.rating, isError = !ratingOk, keyboard = KeyboardType.Number)
    }
}

@Composable
private fun SupervisorForm(onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var credentials by rememberSaveable { mutableStateOf("") }
    var specialization by rememberSaveable { mutableStateOf("") }
    var online by rememberSaveable { mutableStateOf(true) }
    var rate by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    FormFrame(stringResource(R.string.add_supervisor), onCancel = onDone, canSave = name.isNotBlank(), busy = busy, error = error, onSave = {
        busy = true; error = null
        scope.launch {
            try { container.api.addSupervisor(name, credentials.ifBlank { null }, specialization.ifBlank { null }, online, rate.toDoubleOrNull(), email.ifBlank { null }); onDone() }
            catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Field(name, { name = it }, R.string.supervisor_name)
        Field(credentials, { credentials = it }, R.string.supervisor_credentials)
        Field(specialization, { specialization = it }, R.string.specialization)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.offers_online), Modifier.weight(1f)); Switch(online, { online = it }) }
        Field(rate, { rate = it }, R.string.hourly_rate, keyboard = KeyboardType.Decimal)
        Field(email, { email = it }, R.string.contact_email, keyboard = KeyboardType.Email)
        Spacer(Modifier.height(4.dp))
    }
}
