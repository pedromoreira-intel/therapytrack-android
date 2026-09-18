package com.therapytrack.android.ui.client

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun HomeScreen(state: ClientState, onCheckIn: () -> Unit, onCrisis: () -> Unit, onReload: () -> Unit) {
    val container = LocalContainer.current
    var checkIns by remember { mutableStateOf<List<ApiEmaResponse>>(emptyList()) }
    var pendingCount by remember { mutableStateOf(0) }
    var offline by remember { mutableStateOf(false) }
    val outboxTick by container.checkIns.changes.collectAsState()

    LaunchedEffect(state.patient, outboxTick) {
        pendingCount = container.checkIns.pendingCount()
        try { checkIns = container.api.checkIns().take(7); offline = false } catch (e: Exception) { offline = true }
    }

    val name = state.patient?.name?.substringBefore(' ') ?: ""
    val hour = LocalTime.now().hour
    val greeting = stringResource(when { hour < 12 -> R.string.greeting_morning; hour < 19 -> R.string.greeting_afternoon; else -> R.string.greeting_evening }, name)
    val today = LocalDate.now()
    val doneToday = pendingCount > 0 || checkIns.any { ApiTimestamp.parse(it.createdAt)?.atZone(ZoneId.systemDefault())?.toLocalDate() == today }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(greeting.trim().trimEnd(','), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        if (offline) Muted(stringResource(R.string.offline_showing_saved))
        state.loadError?.let { Row { Muted(stringResource(R.string.could_not_load)); TextButton(onReload) { Text(stringResource(R.string.refresh)) } } }

        StuckWorkBanner(refreshKey = outboxTick)

        Card(tint = TherapyColors.champagne.copy(alpha = 0.35f)) {
            Text(stringResource(R.string.daily_check_in), style = MaterialTheme.typography.titleMedium)
            Muted(if (doneToday) stringResource(R.string.check_in_done_today) else stringResource(R.string.how_are_you_today))
            Spacer(Modifier.height(12.dp))
            PrimaryButton(stringResource(R.string.start_check_in), onClick = onCheckIn)
        }

        state.patient?.therapistName?.let {
            Card { Muted(stringResource(R.string.your_therapist)); Text(it, style = MaterialTheme.typography.titleMedium) }
        }

        SectionTitle(stringResource(R.string.recent_check_ins))
        if (checkIns.isEmpty()) Muted(stringResource(R.string.no_check_ins_yet))
        checkIns.forEach { c ->
            Card {
                Muted(ApiTimestamp.parse(c.createdAt)?.shortDateTime() ?: c.createdAt)
                Text("Humor ${c.mood ?: "–"} · Ansiedade ${c.anxiety ?: "–"} · Sono ${c.sleep ?: "–"}", style = MaterialTheme.typography.bodyLarge)
                c.notes?.takeIf { it.isNotBlank() }?.let { Muted(it) }
            }
        }

        Card(tint = TherapyColors.rose.copy(alpha = 0.2f), modifier = Modifier.clickable(onClick = onCrisis)) {
            Text(stringResource(R.string.need_help_now), style = MaterialTheme.typography.titleMedium, color = TherapyColors.critical)
            Muted(stringResource(R.string.crisis_support))
        }
    }
}
