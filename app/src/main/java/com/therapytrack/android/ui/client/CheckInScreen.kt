package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

private data class Step(val question: Int, val hint: Int, val low: Int, val high: Int)

private val steps = listOf(
    Step(R.string.mood_question, R.string.mood_hint, R.string.very_low, R.string.excellent),
    Step(R.string.anxiety_question, R.string.anxiety_hint, R.string.very_calm, R.string.very_anxious),
    Step(R.string.sleep_question, R.string.sleep_hint, R.string.very_poor, R.string.very_well)
)

/** Three 1–10 ratings, an optional note, then an honest account of where it went. */
@Composable
fun CheckInScreen(state: ClientState, onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(0) }
    val values = rememberSaveable { mutableListOf(0, 0, 0) }
    var current by rememberSaveable { mutableStateOf(0) }
    var notes by rememberSaveable { mutableStateOf("") }
    var outcome by remember { mutableStateOf<SaveOutcome?>(null) }
    var busy by remember { mutableStateOf(false) }
    val total = steps.size + 1

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onDone) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.weight(1f))
            Muted(stringResource(R.string.step_of, minOf(step + 1, total), total))
        }
        LinearProgressIndicator(progress = { (step + 1f) / total }, modifier = Modifier.fillMaxWidth(), color = TherapyColors.navy, trackColor = TherapyColors.hairline)
        Spacer(Modifier.height(28.dp))

        outcome?.let { o ->
            val (title, detail) = when (o) {
                SaveOutcome.SENT -> R.string.check_in_sent to R.string.check_in_sent_detail
                SaveOutcome.QUEUED -> R.string.check_in_queued to R.string.check_in_queued_detail
                SaveOutcome.NOT_STORED -> R.string.not_stored to R.string.not_stored_detail
            }
            val tint = when (o) { SaveOutcome.SENT -> TherapyColors.success; SaveOutcome.QUEUED -> TherapyColors.warning; SaveOutcome.NOT_STORED -> TherapyColors.critical }
            Card(tint = tint.copy(alpha = 0.15f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = tint)
                Muted(stringResource(detail), Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(20.dp))
            if (o == SaveOutcome.NOT_STORED) {
                // The answers are still on screen; try again rather than start over.
                PrimaryButton(stringResource(R.string.retry), loading = busy) { outcome = null; step = steps.size }
            } else PrimaryButton(stringResource(R.string.done), onClick = onDone)
            return@Column
        }

        if (step < steps.size) {
            val s = steps[step]
            Text(stringResource(s.question), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
            Muted(stringResource(s.hint), Modifier.padding(top = 6.dp, bottom = 24.dp))
            RatingRow(current) { current = it }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted(stringResource(s.low)); Muted(stringResource(s.high))
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(stringResource(R.string.next), enabled = current > 0) {
                values[step] = current
                step += 1
                current = if (step < steps.size) values[step] else 0
            }
        } else {
            Text(stringResource(R.string.notes_question), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
            Muted(stringResource(R.string.notes_hint), Modifier.padding(top = 6.dp, bottom = 16.dp))
            OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth().height(160.dp))
            Spacer(Modifier.weight(1f))
            PrimaryButton(stringResource(R.string.finish), loading = busy) {
                val patientId = state.patient?.id ?: return@PrimaryButton
                busy = true
                scope.launch {
                    outcome = container.checkIns.submit(patientId, values[0], values[1], values[2], notes.takeIf { it.isNotBlank() })
                    busy = false
                }
            }
        }
    }
}

@Composable
private fun RatingRow(value: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(1..5, 6..10).forEach { range ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                range.forEach { n ->
                    val picked = n == value
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .background(if (picked) TherapyColors.navy else TherapyColors.pearl)
                            .border(1.dp, if (picked) TherapyColors.navy else TherapyColors.hairline, CircleShape)
                            .clickable { onPick(n) },
                        contentAlignment = Alignment.Center
                    ) { Text("$n", color = if (picked) Color.White else TherapyColors.ink, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}
