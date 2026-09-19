package com.therapytrack.android.ui.therapist

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiReferral
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Loaded
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.NotInPlanCard
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.load
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

val deliveryOptions = listOf("either" to R.string.dl_either, "in_person" to R.string.dl_in_person, "online" to R.string.dl_online)
val urgencyOptions = listOf("routine" to R.string.ur_routine, "soon" to R.string.ur_soon, "urgent" to R.string.ur_urgent)

@Composable
fun ReferralsScreen(onBack: () -> Unit, onSeePlan: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var direction by rememberSaveable { mutableStateOf("received") }
    var result by remember { mutableStateOf<Loaded<List<ApiReferral>>>(Loaded.Loading) }
    var reload by remember { mutableStateOf(0) }
    var responding by remember { mutableStateOf<ApiReferral?>(null) }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(direction, reload) { result = load { container.api.referrals(direction) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.referrals_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(direction == "received", { direction = "received" }, label = { Text(stringResource(R.string.received)) })
            FilterChip(direction == "sent", { direction = "sent" }, label = { Text(stringResource(R.string.sent_tab)) })
        }
        when (val r = result) {
            is Loaded.Loading -> Muted(stringResource(R.string.loading))
            is Loaded.NotInPlan -> NotInPlanCard(r.error, onSeePlan)
            is Loaded.Failed -> Muted(stringResource(R.string.could_not_load))
            is Loaded.Ok -> {
                if (r.value.isEmpty()) Muted(stringResource(R.string.no_referrals))
                r.value.forEach { ref ->
                    Card {
                        Row(Modifier.fillMaxWidth()) {
                            Text(ref.counterpartyName ?: "#${ref.counterpartyId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Pill(stringResource(when (ref.status) { "accepted" -> R.string.st_accepted; "declined" -> R.string.st_declined; "withdrawn" -> R.string.st_withdrawn; else -> R.string.st_pending }),
                                 when (ref.status) { "accepted" -> TherapyColors.success; "pending" -> TherapyColors.warning; else -> TherapyColors.muted })
                        }
                        Muted(listOfNotNull(ref.presentingIssue, ref.population, ref.language, ref.city).joinToString(" · "))
                        Muted(stringResource(deliveryOptions.first { it.first == ref.delivery }.second) + " · " + stringResource(urgencyOptions.first { it.first == ref.urgency }.second)
                              + " · " + (ApiTimestamp.parse(ref.createdAt)?.shortDate() ?: ""))
                        ref.note?.takeIf { it.isNotBlank() }?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
                        ref.responseNote?.takeIf { it.isNotBlank() }?.let { Muted("↳ $it") }
                        // Email only on an accepted referral: the point at which contact was agreed.
                        ref.counterpartyEmail?.let { Text(stringResource(R.string.contact_now, it), color = TherapyColors.navy) }
                        if (ref.status == "pending") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (ref.direction == "received") {
                                TextButton({ responding = ref; note = "" }) { Text(stringResource(R.string.decline), color = TherapyColors.critical) }
                                TextButton({ scope.launch { runCatching { container.api.respondToReferral(ref.id, true, null) }; reload++ } }) { Text(stringResource(R.string.accept)) }
                            } else TextButton({ scope.launch { runCatching { container.api.withdrawReferral(ref.id) }; reload++ } }) { Text(stringResource(R.string.withdraw)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    responding?.let { ref ->
        androidx.compose.material3.AlertDialog(onDismissRequest = { responding = null },
            title = { Text(stringResource(R.string.decline)) },
            text = { OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.response_note)) }) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.respondToReferral(ref.id, false, note.ifBlank { null }) }; reload++ }; responding = null }) { Text(stringResource(R.string.decline), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ responding = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

/** No client identity travels: the referral is a description of a case, not a record. */
@Composable
fun NewReferralScreen(toTherapistId: Int, toName: String, onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var issue by rememberSaveable { mutableStateOf("") }
    var population by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var delivery by rememberSaveable { mutableStateOf("either") }
    var urgency by rememberSaveable { mutableStateOf("routine") }
    var note by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var sent by remember { mutableStateOf(false) }

    if (sent) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(tint = TherapyColors.success.copy(alpha = 0.15f)) { Text(stringResource(R.string.referral_sent), color = TherapyColors.success, style = MaterialTheme.typography.titleMedium) }
            PrimaryButton(stringResource(R.string.done), onClick = onDone)
        }
        return
    }
    FormFrame(stringResource(R.string.new_referral), onCancel = onDone, canSave = issue.isNotBlank(), busy = busy, error = error, saveLabel = stringResource(R.string.send), onSave = {
        busy = true; error = null
        scope.launch {
            try { container.api.sendReferral(toTherapistId, delivery, urgency, issue, population.ifBlank { null }, language.ifBlank { null }, city.ifBlank { null }, note.ifBlank { null }); sent = true }
            catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Muted(stringResource(R.string.referral_to, toName))
        Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Text(stringResource(R.string.referral_privacy)) }
        Field(issue, { issue = it }, R.string.presenting_issue)
        Field(population, { population = it }, R.string.population)
        Field(language, { language = it }, R.string.language)
        Field(city, { city = it }, R.string.city)
        Muted(stringResource(R.string.delivery)); ChipRow(deliveryOptions, delivery) { delivery = it }
        Muted(stringResource(R.string.urgency)); ChipRow(urgencyOptions, urgency) { urgency = it }
        Field(note, { note = it }, R.string.notes, single = false)
    }
}
