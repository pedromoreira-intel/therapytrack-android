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
