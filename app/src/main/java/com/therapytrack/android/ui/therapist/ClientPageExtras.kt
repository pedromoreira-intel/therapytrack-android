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
