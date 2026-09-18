package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.clinical.CrisisResources
import com.therapytrack.android.clinical.Instrument
import com.therapytrack.android.clinical.Instruments
import com.therapytrack.android.clinical.Scoring
import com.therapytrack.android.core.Api
import com.therapytrack.android.core.ApiAssessmentResult
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

@Composable
fun AssessmentsScreen(state: ClientState, onStart: (Api.Instrument) -> Unit) {
    val container = LocalContainer.current
    var latest by remember { mutableStateOf<Map<Api.Instrument, ApiAssessmentResult?>>(emptyMap()) }

    LaunchedEffect(state.patient) {
        val id = state.patient?.id ?: return@LaunchedEffect
        latest = Api.Instrument.entries.associateWith { runCatching { container.api.assessmentResults(it, id).maxByOrNull { r -> r.id } }.getOrNull() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.assessments_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Muted(stringResource(R.string.assessments_intro))
        Instruments.all.forEach { instrument ->
            val subtitle = when (instrument.api) { Api.Instrument.PHQ9 -> R.string.phq9_subtitle; Api.Instrument.GAD7 -> R.string.gad7_subtitle; Api.Instrument.WAISR -> R.string.waisr_subtitle }
            Card(modifier = Modifier.clickable { onStart(instrument.api) }) {
                Text(stringResource(instrument.title), style = MaterialTheme.typography.titleMedium)
                Muted(stringResource(subtitle))
                val last = latest[instrument.api]
                Muted(if (last == null) stringResource(R.string.no_results_yet)
                      else stringResource(R.string.last_result, scoreText(instrument, last.score), ApiTimestamp.parse(last.assessmentDate)?.shortDate() ?: last.assessmentDate),
                    Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(10.dp))
                PrimaryButton(stringResource(R.string.start)) { onStart(instrument.api) }
            }
        }
    }
}

private fun scoreText(instrument: Instrument, score: Double): String =
    if (instrument.api == Api.Instrument.WAISR) String.format("%.1f", score) else score.toInt().toString()

@Composable
fun bandLabel(instrument: Instrument, responses: List<Int>): String = when (instrument.api) {
    Api.Instrument.PHQ9 -> stringResource(bandRes(Scoring.phq9Band(responses.sum())))
    Api.Instrument.GAD7 -> stringResource(bandRes(Scoring.gad7Band(responses.sum())))
    Api.Instrument.WAISR -> stringResource(when (Scoring.waiSrAlliance(Scoring.waiSrMean(responses))) {
        Scoring.Alliance.WEAK -> R.string.alliance_weak; Scoring.Alliance.FAIR -> R.string.alliance_fair
        Scoring.Alliance.MODERATE -> R.string.alliance_moderate; Scoring.Alliance.GOOD -> R.string.alliance_good
        Scoring.Alliance.STRONG -> R.string.alliance_strong })
}

private fun bandRes(band: Scoring.Band) = when (band) {
    Scoring.Band.MINIMAL -> R.string.band_minimal; Scoring.Band.MILD -> R.string.band_mild
    Scoring.Band.MODERATE -> R.string.band_moderate; Scoring.Band.MODERATELY_SEVERE -> R.string.band_moderately_severe
    Scoring.Band.SEVERE -> R.string.band_severe
}

/** One item per screen, answers kept until the outbox has them. */
@Composable
fun QuestionnaireScreen(state: ClientState, which: Api.Instrument, onDone: () -> Unit, onCrisis: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val instrument = Instruments.byApi(which)
    var index by rememberSaveable { mutableStateOf(0) }
    var answers by rememberSaveable { mutableStateOf(List<Int?>(instrument.items.size) { null }) }
    var outcome by remember { mutableStateOf<SaveOutcome?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row { TextButton(onDone) { Text(stringResource(R.string.cancel)) }; Spacer(Modifier.weight(1f)); Muted(stringResource(instrument.title)) }

        outcome?.let { o ->
            val responses = answers.map { it ?: 0 }
            val display = if (which == Api.Instrument.WAISR) String.format("%.1f", Scoring.waiSrMean(responses)) else responses.sum().toString()
            Card {
                Muted(stringResource(R.string.your_score))
                Text(display, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
                Text(bandLabel(instrument, responses), style = MaterialTheme.typography.titleMedium)
                Muted(stringResource(when (o) { SaveOutcome.SENT -> R.string.result_sent; SaveOutcome.QUEUED -> R.string.result_queued; SaveOutcome.NOT_STORED -> R.string.result_not_stored }), Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            // Item 9 answered above zero: say so, and put the numbers in front of them now.
            if (which == Api.Instrument.PHQ9 && (answers[Scoring.PHQ9_ITEM9_INDEX] ?: 0) > 0) {
                Card(tint = TherapyColors.rose.copy(alpha = 0.25f)) {
                    Text(stringResource(R.string.item9_notice), style = MaterialTheme.typography.bodyLarge)
                    CrisisResources.resources().forEach { Text("${it.name}: ${it.number}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(stringResource(R.string.crisis_support), onClick = onCrisis)
                }
                Spacer(Modifier.height(12.dp))
            }
            Muted(stringResource(R.string.result_disclaimer))
            Spacer(Modifier.height(16.dp))
            if (o == SaveOutcome.NOT_STORED) PrimaryButton(stringResource(R.string.retry), loading = busy) { outcome = null; index = instrument.items.size - 1 }
            else PrimaryButton(stringResource(R.string.done), onClick = onDone)
            return@Column
        }

        LinearProgressIndicator(progress = { (index + 1f) / instrument.items.size }, modifier = Modifier.fillMaxWidth(), color = TherapyColors.navy, trackColor = TherapyColors.hairline)
        Muted(stringResource(R.string.question_of, index + 1, instrument.items.size), Modifier.padding(top = 8.dp))
        if (index == 0) Muted(stringResource(instrument.intro), Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(instrument.items[index]), style = MaterialTheme.typography.titleLarge, color = TherapyColors.navy)
        Spacer(Modifier.height(20.dp))
        instrument.options.forEach { (value, label) ->
            val picked = answers[index] == value
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(MaterialTheme.shapes.medium)
                    .background(if (picked) TherapyColors.navy else TherapyColors.pearl)
                    .border(1.dp, if (picked) TherapyColors.navy else TherapyColors.hairline, MaterialTheme.shapes.medium)
                    .clickable { answers = answers.toMutableList().also { it[index] = value } }
                    .padding(16.dp)
            ) { Text(stringResource(label), color = if (picked) androidx.compose.ui.graphics.Color.White else TherapyColors.ink, style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (index > 0) TextButton({ index-- }, Modifier.weight(1f)) { Text(stringResource(R.string.back)) }
            val last = index == instrument.items.size - 1
            PrimaryButton(stringResource(if (last) R.string.submit else R.string.next), Modifier.weight(2f), enabled = answers[index] != null, loading = busy) {
                if (!last) { index++; return@PrimaryButton }
                val patientId = state.patient?.id ?: return@PrimaryButton
                busy = true
                scope.launch {
                    outcome = container.assessments.submit(which, patientId, answers.map { it ?: 0 })
                    busy = false
                }
            }
        }
    }
}
