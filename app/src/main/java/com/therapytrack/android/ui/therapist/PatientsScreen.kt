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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.CreatedPatient
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
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

@Composable
fun PatientsScreen(onPatient: (Int) -> Unit) {
    val container = LocalContainer.current
    var patients by remember { mutableStateOf<List<ApiPatient>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var creating by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(reload) { loadFailed = runCatching { patients = container.api.patients() }.isFailure }

    if (creating) { NewPatientScreen(onDone = { creating = false; reload++ }); return }

    val shown = patients.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    Scaffold(containerColor = TherapyColors.canvas, floatingActionButton = {
        FloatingActionButton({ creating = true }, containerColor = TherapyColors.navy) { Icon(Icons.Filled.Add, stringResource(R.string.new_client), tint = Color.White) }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.clients_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(query, { query = it }, placeholder = { Text(stringResource(R.string.search_clients)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
            // An empty caseload and a failed load are different things to say.
            if (loadFailed) item { Row { Muted(stringResource(R.string.could_not_load)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } } }
            else if (patients.isEmpty()) item { Muted(stringResource(R.string.no_clients)) }
            items(shown, key = { it.id }) { p ->
                Card(modifier = Modifier.clickable { onPatient(p.id) }) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            p.diagnosis?.takeIf { it.isNotBlank() && it != "—" }?.let { Muted(it) }
                            p.lastEma?.let { Muted("Check-in: ${ApiTimestamp.parse(it)?.shortDate() ?: it}") }
                        }
                        Pill(riskLabel(p.riskLevel), riskColor(p.riskLevel))
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        created?.let { c ->
            Text(stringResource(R.string.invite_ready_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
            Muted(stringResource(R.string.invite_ready_body, c.patient.name, c.inviteExpiresAt?.let { ApiTimestamp.parse(it)?.shortDate() } ?: ""))
            Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) {
                Text(c.inviteCode ?: "—", style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
            }
            TextButton({
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("invite", c.inviteCode ?: ""))
                copied = true
            }) { Text(stringResource(if (copied) R.string.code_copied else R.string.copy_code)) }
            PrimaryButton(stringResource(R.string.done), onClick = onDone)
            return@Column
        }

        Text(stringResource(R.string.new_client), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(diagnosis, { diagnosis = it }, label = { Text(stringResource(R.string.diagnosis)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Muted(stringResource(R.string.risk_level))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LOW" to R.string.risk_low, "MEDIUM" to R.string.risk_medium, "HIGH" to R.string.risk_high).forEach { (v, l) ->
                FilterChip(selected = risk == v, onClick = { risk = v }, label = { Text(stringResource(l)) })
            }
        }
        error?.let { ErrorText(errorText(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onDone, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            PrimaryButton(stringResource(R.string.create_client), Modifier.weight(2f), enabled = name.isNotBlank() && email.contains('@'), loading = busy) {
                busy = true; error = null
                scope.launch {
                    try { created = container.api.createPatient(name.trim(), email.trim(), diagnosis, risk) } catch (e: Exception) { error = e } finally { busy = false }
                }
            }
        }
    }
}
