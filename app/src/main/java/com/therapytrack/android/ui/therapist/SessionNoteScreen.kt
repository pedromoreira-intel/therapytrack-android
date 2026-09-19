package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A session note. Goes through the outbox, so it is on disk before the
 * network is tried and the text on screen is never cleared until it is.
 */
@Composable
fun SessionNoteScreen(patientId: Int, patientName: String, onDone: () -> Unit, onSeePlan: () -> Unit = {}) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var focus by rememberSaveable { mutableStateOf("") }
    var interventions by rememberSaveable { mutableStateOf("") }
    var progress by rememberSaveable { mutableStateOf("") }
    var homework by rememberSaveable { mutableStateOf("") }
    var plan by rememberSaveable { mutableStateOf("") }
    var risk by rememberSaveable { mutableStateOf("LOW") }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<SaveOutcome?>(null) }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.new_session_note), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Muted(patientName)

        outcome?.let { o ->
            val (title, detail, tint) = when (o) {
                SaveOutcome.SENT -> Triple(R.string.note_saved, null, TherapyColors.success)
                SaveOutcome.QUEUED -> Triple(R.string.note_queued, R.string.note_queued_detail, TherapyColors.warning)
                SaveOutcome.NOT_STORED -> Triple(R.string.not_stored, R.string.note_not_stored, TherapyColors.critical)
            }
            Card(tint = tint.copy(alpha = 0.15f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = tint)
                detail?.let { Muted(stringResource(it), Modifier.padding(top = 6.dp)) }
            }
            if (o != SaveOutcome.NOT_STORED) { PrimaryButton(stringResource(R.string.done), onClick = onDone); return@Column }
        }

        // A draft fills the fields below as editable text; nothing is saved until the therapist saves.
        TranscriptDraftPanel(patientId, onSeePlan) { d ->
            d.focus?.let { focus = it }; d.interventions?.let { interventions = it }; d.progressNotes?.let { progress = it }
            d.homework?.let { homework = it }; d.plan?.let { plan = it }
        }
        OutlinedTextField(date, { date = it }, label = { Text(stringResource(R.string.session_date)) }, singleLine = true,
            isError = runCatching { LocalDate.parse(date) }.isFailure, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(focus, { focus = it }, label = { Text(stringResource(R.string.focus)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(interventions, { interventions = it }, label = { Text(stringResource(R.string.interventions)) }, modifier = Modifier.fillMaxWidth().height(110.dp))
        OutlinedTextField(progress, { progress = it }, label = { Text(stringResource(R.string.progress_notes)) }, modifier = Modifier.fillMaxWidth().height(160.dp))
        OutlinedTextField(homework, { homework = it }, label = { Text(stringResource(R.string.homework)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(plan, { plan = it }, label = { Text(stringResource(R.string.next_session_plan)) }, modifier = Modifier.fillMaxWidth())
        Muted(stringResource(R.string.risk_level))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LOW" to R.string.risk_low, "MEDIUM" to R.string.risk_medium, "HIGH" to R.string.risk_high).forEach { (v, l) ->
                FilterChip(selected = risk == v, onClick = { risk = v }, label = { Text(stringResource(l)) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onDone, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            PrimaryButton(stringResource(R.string.save), Modifier.weight(2f), loading = busy,
                enabled = interventions.isNotBlank() && progress.isNotBlank() && runCatching { LocalDate.parse(date) }.isSuccess) {
                busy = true
                scope.launch {
                    outcome = container.sessionNotes.save(patientId, patientName, date, focus.ifBlank { null }, interventions, progress,
                        homework.ifBlank { null }, risk, plan.ifBlank { null })
                    busy = false
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A scheduled appointment. Plain date and time fields: no picker to fight with, and the server validates. */
@Composable
fun ScheduleSessionScreen(patientId: Int, onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var time by rememberSaveable { mutableStateOf("10:00") }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var notes by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var done by remember { mutableStateOf(false) }

    val valid = runCatching { java.time.LocalDateTime.parse("${date}T$time:00") }.isSuccess && minutes.toIntOrNull() != null

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.schedule_session), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        if (done) { Card(tint = TherapyColors.success.copy(alpha = 0.15f)) { Text(stringResource(R.string.session_scheduled), color = TherapyColors.success) }; PrimaryButton(stringResource(R.string.done), onClick = onDone); return@Column }
        OutlinedTextField(date, { date = it }, label = { Text(stringResource(R.string.date_yyyy_mm_dd)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(time, { time = it }, label = { Text(stringResource(R.string.time_hh_mm)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(minutes, { minutes = it }, label = { Text(stringResource(R.string.duration_minutes)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes_question)) }, modifier = Modifier.fillMaxWidth())
        error?.let { ErrorText(errorText(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onDone, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            PrimaryButton(stringResource(R.string.schedule_session), Modifier.weight(2f), enabled = valid, loading = busy) {
                busy = true; error = null
                scope.launch {
                    try {
                        // Sent as local wall-clock time in ISO form; the server stores it as given.
                        val local = java.time.LocalDateTime.parse("${date}T$time:00").atZone(java.time.ZoneId.systemDefault()).toInstant()
                        container.api.createSession(patientId, com.therapytrack.android.core.ApiTimestamp.iso8601(local), minutes.toInt(), notes)
                        done = true
                    } catch (e: Exception) { error = e } finally { busy = false }
                }
            }
        }
    }
}
