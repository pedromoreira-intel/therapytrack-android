package com.therapytrack.android.ui.therapist

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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiTherapistProfile
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Loaded
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.NotInPlanCard
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.load
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

/** Network-plan feature: the first place the 402 card is exercised for real. */
@Composable
fun DirectoryScreen(onBack: () -> Unit, onSeePlan: () -> Unit, onRefer: (Int, String) -> Unit) {
    val container = LocalContainer.current
    var query by rememberSaveable { mutableStateOf("") }
    var accepting by rememberSaveable { mutableStateOf(false) }
    var online by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<Loaded<List<ApiTherapistProfile>>>(Loaded.Loading) }
    var selected by remember { mutableStateOf<ApiTherapistProfile?>(null) }

    LaunchedEffect(query, accepting, online) { result = load { container.api.directory(query, null, accepting, online) } }

    selected?.let { p -> ProfileScreen(p, onBack = { selected = null }, onRefer = { onRefer(p.therapistId, p.name) }); return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.directory_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        OutlinedTextField(query, { query = it }, placeholder = { Text(stringResource(R.string.directory_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(accepting, { accepting = !accepting }, label = { Text(stringResource(R.string.accepting_clients)) })
            FilterChip(online, { online = !online }, label = { Text(stringResource(R.string.offers_online)) })
        }
        when (val r = result) {
            is Loaded.Loading -> Muted(stringResource(R.string.loading))
            is Loaded.NotInPlan -> NotInPlanCard(r.error, onSeePlan)
            is Loaded.Failed -> Muted(stringResource(R.string.could_not_load))
            is Loaded.Ok -> {
                if (r.value.isEmpty()) Muted(stringResource(R.string.no_colleagues))
                r.value.forEach { p -> ProfileRow(p) { selected = p } }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileRow(p: ApiTherapistProfile, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                p.headline?.let { Muted(it) }
                Muted(listOfNotNull(p.city, p.specialty.take(3).joinToString(", ").ifBlank { null }).joinToString(" · "))
            }
            if (p.acceptingClients) Pill(stringResource(R.string.accepting_clients), TherapyColors.success)
        }
    }
}

@Composable
private fun ProfileScreen(p: ApiTherapistProfile, onBack: () -> Unit, onRefer: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(p.name, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        p.headline?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        Muted(listOfNotNull(p.city, p.region, p.country).joinToString(", "))
        p.yearsExperience?.let { Muted(stringResource(R.string.years_experience, it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (p.acceptingClients) Pill(stringResource(R.string.accepting_clients), TherapyColors.success)
            if (p.offersOnline) Pill(stringResource(R.string.offers_online), TherapyColors.navy)
            if (p.offersInPerson) Pill(stringResource(R.string.offers_in_person), TherapyColors.navy)
            if (p.offersSupervision) Pill(stringResource(R.string.offers_supervision), TherapyColors.navy)
        }
        p.bio?.let { Card { Text(it) } }
        if (p.specialty.isNotEmpty()) Card { Muted(stringResource(R.string.specialties).substringBefore(" (")); Text(p.specialty.joinToString(", ")) }
        if (p.language.isNotEmpty()) Card { Muted(stringResource(R.string.languages).substringBefore(" (")); Text(p.language.joinToString(", ")) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(stringResource(R.string.refer_client), onClick = onRefer)
    }
}

/** The therapist's own directory entry. Tags are typed comma-separated: simplest thing that round-trips. */
@Composable
fun MyProfileScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<ApiTherapistProfile?>(null) }
    var specialties by rememberSaveable { mutableStateOf("") }
    var languages by rememberSaveable { mutableStateOf("") }
    var years by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { container.api.myProfile() }.onSuccess { loaded ->
            p = loaded; specialties = loaded.specialty.joinToString(", "); languages = loaded.language.joinToString(", "); years = loaded.yearsExperience?.toString() ?: ""
        }.onFailure { error = it }
    }
    val current = p ?: run {
        Column(Modifier.padding(20.dp)) { TextButton(onBack) { Text(stringResource(R.string.back)) }; Muted(if (error == null) stringResource(R.string.loading) else stringResource(R.string.could_not_load)) }
        return
    }

    FormFrame(stringResource(R.string.my_profile), onCancel = onBack, canSave = years.isBlank() || years.toIntOrNull() != null, busy = busy, error = error, onSave = {
        busy = true; error = null; saved = false
        scope.launch {
            try {
                p = container.api.updateMyProfile(current.copy(
                    yearsExperience = years.toIntOrNull(),
                    specialty = specialties.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    language = languages.split(',').map { it.trim() }.filter { it.isNotEmpty() }))
                saved = true
            } catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        if (saved) Card(tint = TherapyColors.success.copy(alpha = 0.15f)) { Text(stringResource(R.string.profile_saved), color = TherapyColors.success) }
        Field(current.headline ?: "", { p = current.copy(headline = it) }, R.string.headline)
        Field(current.bio ?: "", { p = current.copy(bio = it) }, R.string.bio, single = false)
        Field(years, { years = it }, R.string.years_experience_field, keyboard = KeyboardType.Number)
        Field(current.city ?: "", { p = current.copy(city = it) }, R.string.city)
        Field(specialties, { specialties = it }, R.string.specialties)
        Field(languages, { languages = it }, R.string.languages)
        listOf(
            R.string.accepting_clients to (current.acceptingClients to { v: Boolean -> p = current.copy(acceptingClients = v) }),
            R.string.offers_online to (current.offersOnline to { v: Boolean -> p = current.copy(offersOnline = v) }),
            R.string.offers_in_person to (current.offersInPerson to { v: Boolean -> p = current.copy(offersInPerson = v) }),
            R.string.offers_supervision to (current.offersSupervision to { v: Boolean -> p = current.copy(offersSupervision = v) }),
            R.string.is_listed to (current.isListed to { v: Boolean -> p = current.copy(isListed = v) })
        ).forEach { (label, pair) ->
            Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(label), Modifier.weight(1f)); Switch(pair.first, pair.second) }
        }
    }
}
