package com.therapytrack.android.ui.therapist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.CreatedPatient
import com.therapytrack.android.ui.auth.LabelledField
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Avatar
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch

fun riskColor(level: String?) = when (level?.uppercase()) {
    "HIGH", "CRITICAL" -> TherapyColors.critical
    "MEDIUM", "MODERATE" -> TherapyColors.warning
    else -> TherapyColors.success
}

@Composable
fun riskLabel(level: String?): String = stringResource(when (level?.uppercase()) {
    "HIGH", "CRITICAL" -> R.string.risk_high
    "MEDIUM", "MODERATE" -> R.string.risk_medium
    else -> R.string.risk_low
})

/** The caseload, as iOS lays it out: sans h1 title, "+ New client" top-right, one card per person. */
@Composable
fun PatientsScreen(onPatient: (Int) -> Unit, startCreating: Boolean = false) {
    val container = LocalContainer.current
    var patients by remember { mutableStateOf<List<ApiPatient>>(emptyList()) }
    var creating by rememberSaveable { mutableStateOf(startCreating) }
    var reload by remember { mutableStateOf(0) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(reload) { loadFailed = runCatching { patients = container.api.patients() }.isFailure }
    if (creating) { NewPatientScreen(onDone = { creating = false; reload++ }); return }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton({ creating = true }) {
                    Icon(Icons.Filled.AddCircle, null, tint = TherapyColors.navy); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.new_client), style = TherapyType.emphasisLarge, color = TherapyColors.navy)
                }
            }
            Text(stringResource(R.string.clients_title), style = TherapyType.h1, color = TherapyColors.ink, modifier = Modifier.padding(bottom = 8.dp))
        }
        // An empty caseload and a failed load are different things to say.
        if (loadFailed) item { Row { Muted(stringResource(R.string.could_not_load)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } } }
        else if (patients.isEmpty()) item { Muted(stringResource(R.string.no_clients)) }
        items(patients, key = { it.id }) { p ->
            Card(modifier = Modifier.clickable { onPatient(p.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p.name, 44.dp); Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = TherapyType.emphasisLarge, color = TherapyColors.ink)
                        p.diagnosis?.takeIf { it.isNotBlank() && it != "—" }?.let { Text(it, style = TherapyType.body, color = TherapyColors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        p.nextSession?.let { Text(stringResource(R.string.next_session, ApiTimestamp.parse(it)?.shortDateTime() ?: it), style = TherapyType.caption, color = TherapyColors.muted) }
                            ?: p.lastEma?.let { Text(stringResource(R.string.last_check_in) + ": " + (ApiTimestamp.parse(it)?.shortDate() ?: it), style = TherapyType.caption, color = TherapyColors.muted) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Pill(riskLabel(p.riskLevel), riskColor(p.riskLevel))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Create the record, then show the one-time invite the therapist hands over. */
@Composable
private fun NewPatientScreen(onDone: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var diagnosis by rememberSaveable { mutableStateOf("") }
    var risk by rememberSaveable { mutableStateOf("LOW") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var created by remember { mutableStateOf<CreatedPatient?>(null) }
    var copied by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        created?.let { c ->
            Text(stringResource(R.string.invite_ready_title), style = TherapyType.display, color = TherapyColors.navy)
            Muted(stringResource(R.string.invite_ready_body, c.patient.name, c.inviteExpiresAt?.let { ApiTimestamp.parse(it)?.shortDate() } ?: ""))
            Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) {
                Overline(stringResource(R.string.invite_code))
                Text(c.inviteCode ?: "—", style = TherapyType.display, color = TherapyColors.navy)
            }
            TextButton({
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", c.inviteCode ?: ""))
                copied = true
            }) { Text(stringResource(if (copied) R.string.code_copied else R.string.copy_code)) }
            PrimaryButton(stringResource(R.string.done), onClick = onDone)
            return@Column
        }

        Text(stringResource(R.string.new_client), style = TherapyType.display, color = TherapyColors.navy)
        Card(padding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabelledField(stringResource(R.string.name), name, { name = it })
                LabelledField(stringResource(R.string.email), email, { email = it }, keyboard = KeyboardType.Email)
                LabelledField(stringResource(R.string.diagnosis), diagnosis, { diagnosis = it })
                Overline(stringResource(R.string.risk_level))
                ChipRow(listOf("LOW" to R.string.risk_low, "MEDIUM" to R.string.risk_medium, "HIGH" to R.string.risk_high), risk) { risk = it }
            }
        }
        error?.let { ErrorText(errorText(it)) }
        PrimaryButton(stringResource(R.string.create_client), enabled = name.isNotBlank() && email.contains('@'), loading = busy) {
            busy = true; error = null
            scope.launch { try { created = container.api.createPatient(name.trim(), email.trim(), diagnosis, risk) } catch (e: Exception) { error = e } finally { busy = false } }
        }
        TextButton(onDone, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.cancel), color = TherapyColors.navy) }
    }
}
