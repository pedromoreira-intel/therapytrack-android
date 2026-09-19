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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiCredential
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.ApiTraining
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A list-with-add-form screen. Header, optional failure line, the rows, and a form that replaces the list while open. */
@Composable
fun ListScreen(title: String, onBack: () -> Unit, loadFailed: Boolean, onReload: () -> Unit, addLabel: String,
               form: (@Composable (onDone: () -> Unit) -> Unit)?, showForm: Boolean, setShowForm: (Boolean) -> Unit,
               content: @Composable () -> Unit) {
    if (showForm && form != null) { form { setShowForm(false); onReload() }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        if (loadFailed) Row { Muted(stringResource(R.string.could_not_load)); TextButton(onReload) { Text(stringResource(R.string.refresh)) } }
        if (form != null) PrimaryButton(addLabel) { setShowForm(true) }
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun credentialStatus(c: ApiCredential): Pair<String, androidx.compose.ui.graphics.Color> = when (c.status) {
    "expired" -> stringResource(R.string.cred_expired) to TherapyColors.critical
    "expiring_soon" -> stringResource(R.string.cred_expiring_soon, c.daysUntilExpiry ?: 0) to TherapyColors.critical
    "renewal_due" -> stringResource(R.string.cred_renewal_due, c.daysUntilExpiry ?: 0) to TherapyColors.warning
    else -> stringResource(R.string.cred_active) to TherapyColors.success
}

@Composable
fun CredentialsScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ApiCredential>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<ApiCredential?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ApiCredential?>(null) }
    LaunchedEffect(reload) { failed = runCatching { items = container.api.credentials() }.isFailure }

    ListScreen(stringResource(R.string.credentials_section), onBack, failed, { reload++ }, stringResource(R.string.add),
        form = { done -> CredentialForm(editing) { editing = null; done() } }, showForm = showForm, setShowForm = { showForm = it; if (!it) editing = null }) {
        if (items.isEmpty() && !failed) Muted(stringResource(R.string.no_credentials))
        items.forEach { c ->
            val (label, color) = credentialStatus(c)
            Card {
                Row(Modifier.fillMaxWidth()) {
                    Text(c.licenseType, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill(label, color)
                }
                listOfNotNull(c.licenseNumber, c.issuingBody, c.region).takeIf { it.isNotEmpty() }?.let { Muted(it.joinToString(" · ")) }
                c.expiresOn?.let { Muted("${stringResource(R.string.expires_on).substringBefore(" (")}: ${ApiTimestamp.parse(it)?.shortDate() ?: it}") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ deleting = c }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                    TextButton({ editing = c; showForm = true }) { Text(stringResource(R.string.edit)) }
                }
            }
        }
    }
    deleting?.let { c ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.delete_credential_confirm)) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.deleteCredential(c.id) }; reload++ }; deleting = null }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun CredentialForm(existing: ApiCredential?, onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(existing?.licenseType ?: "") }
    var number by rememberSaveable { mutableStateOf(existing?.licenseNumber ?: "") }
    var body by rememberSaveable { mutableStateOf(existing?.issuingBody ?: "") }
    var region by rememberSaveable { mutableStateOf(existing?.region ?: "") }
    var expires by rememberSaveable { mutableStateOf(existing?.expiresOn?.take(10) ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val dateOk = expires.isBlank() || runCatching { LocalDate.parse(expires) }.isSuccess

    FormFrame(stringResource(R.string.credentials_section), onCancel = onDone, canSave = type.isNotBlank() && dateOk, busy = busy, error = error, onSave = {
        busy = true; error = null
        scope.launch {
            try { container.api.saveCredential(existing?.id, type, number.ifBlank { null }, body.ifBlank { null }, region.ifBlank { null }, expires.ifBlank { null }, notes.ifBlank { null }); onDone() }
            catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Field(type, { type = it }, R.string.license_type)
        Field(number, { number = it }, R.string.license_number)
        Field(body, { body = it }, R.string.issuing_body)
        Field(region, { region = it }, R.string.region)
        Field(expires, { expires = it }, R.string.expires_on, isError = !dateOk)
        Field(notes, { notes = it }, R.string.notes, single = false)
    }
}

val trainingTypes = listOf("post_graduation" to R.string.tt_post_graduation, "masters" to R.string.tt_masters, "doctorate" to R.string.tt_doctorate,
    "certification" to R.string.tt_certification, "course" to R.string.tt_course, "workshop" to R.string.tt_workshop,
    "seminar" to R.string.tt_seminar, "conference" to R.string.tt_conference, "other" to R.string.tt_other)

@Composable
fun TrainingScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ApiTraining>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ApiTraining?>(null) }
    LaunchedEffect(reload) { failed = runCatching { items = container.api.training() }.isFailure }

    ListScreen(stringResource(R.string.training_section), onBack, failed, { reload++ }, stringResource(R.string.add),
        form = { done -> TrainingForm(done) }, showForm = showForm, setShowForm = { showForm = it }) {
        if (items.isEmpty() && !failed) Muted(stringResource(R.string.no_training))
        items.forEach { t ->
            Card {
                Text(t.title, style = MaterialTheme.typography.titleMedium)
                Muted(listOfNotNull(stringResource(trainingTypes.firstOrNull { it.first == t.trainingType }?.second ?: R.string.tt_other), t.institution,
                    ApiTimestamp.parse(t.completedOn)?.shortDate(), "${fmtHours(t.hours)} h").joinToString(" · "))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ deleting = t }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                }
            }
        }
    }
    deleting?.let { t ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.delete_training_confirm)) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.deleteTraining(t.id) }; reload++ }; deleting = null }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun TrainingForm(onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var institution by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("course") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var hours by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val dateOk = runCatching { LocalDate.parse(date) }.isSuccess

    FormFrame(stringResource(R.string.training_section), onCancel = onDone, canSave = title.isNotBlank() && dateOk && (hours.toDoubleOrNull() ?: -1.0) >= 0, busy = busy, error = error, onSave = {
        busy = true; error = null
        scope.launch {
            try { container.api.addTraining(title, institution.ifBlank { null }, type, date, hours.toDoubleOrNull() ?: 0.0, notes.ifBlank { null }); onDone() }
            catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Field(title, { title = it }, R.string.training_title)
        Field(institution, { institution = it }, R.string.institution)
        Muted(stringResource(R.string.training_type))
        ChipRow(trainingTypes, type) { type = it }
        Field(date, { date = it }, R.string.completed_on, isError = !dateOk)
        Field(hours, { hours = it }, R.string.hours, keyboard = KeyboardType.Decimal)
        Field(notes, { notes = it }, R.string.notes, single = false)
    }
}

// Shared form pieces -------------------------------------------------------

@Composable
fun FormFrame(title: String, onCancel: () -> Unit, canSave: Boolean, busy: Boolean, error: Throwable?, onSave: () -> Unit,
              saveLabel: String = stringResource(R.string.save), content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        content()
        error?.let { ErrorText(errorText(it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onCancel, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            PrimaryButton(saveLabel, Modifier.weight(2f), enabled = canSave, loading = busy, onClick = onSave)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: Int, single: Boolean = true, isError: Boolean = false, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, singleLine = single, isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = if (single) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(120.dp))
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChipRow(options: List<Pair<String, Int>>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, l) -> FilterChip(selected = selected == v, onClick = { onSelect(v) }, label = { Text(stringResource(l)) }) }
    }
}
