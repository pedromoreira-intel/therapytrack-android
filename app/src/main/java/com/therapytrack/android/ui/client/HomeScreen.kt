package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiGoal
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.IconCircle
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType

@Composable
fun HomeScreen(state: ClientState, onCheckIn: () -> Unit, onCrisis: () -> Unit, onReload: () -> Unit) {
    val container = LocalContainer.current
    var checkIns by remember { mutableStateOf<List<ApiEmaResponse>>(emptyList()) }
    var goals by remember { mutableStateOf<List<ApiGoal>>(emptyList()) }
    var offline by remember { mutableStateOf(false) }
    val outboxTick by container.checkIns.changes.collectAsState()

    LaunchedEffect(state.patient, outboxTick) {
        try { checkIns = container.api.checkIns().sortedByDescending { ApiTimestamp.parse(it.doneAt) }; offline = false } catch (e: Exception) { android.util.Log.w("Home", "check-ins: $e"); offline = true }
        state.patient?.let { p -> runCatching { goals = container.api.goals(p.id).filter { !it.completed } } }
    }

    val name = state.patient?.name?.substringBefore(' ') ?: ""
    val latest = checkIns.firstOrNull()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // "Obter ajuda" sits top-left on iOS, always one tap away.
        Row(Modifier.clip(CircleShape).background(TherapyColors.critical.copy(alpha = 0.12f)).clickable(onClick = onCrisis).padding(12.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Error, null, tint = TherapyColors.critical, modifier = Modifier.height(16.dp)); Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.get_help), style = TherapyType.emphasis, color = TherapyColors.critical)
        }
        Column {
            Text(stringResource(R.string.welcome_back), style = TherapyType.bodyLarge, color = TherapyColors.muted)
            Text(name, style = TherapyType.display, color = TherapyColors.ink)
        }
        if (offline) Muted(stringResource(R.string.offline_showing_saved))
        state.loadError?.let { Row { Muted(stringResource(R.string.could_not_load)); TextButton(onReload) { Text(stringResource(R.string.refresh)) } } }

        StuckWorkBanner(refreshKey = outboxTick)

        Card {
            Row(verticalAlignment = Alignment.Top) {
                IconCircle(Icons.Filled.Groups, tint = TherapyColors.champagne.copy(alpha = 1f).let { Color(0xFFB99A3C) })
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.how_it_works), style = TherapyType.emphasis, color = TherapyColors.ink)
                    Text(stringResource(R.string.how_it_works_client), style = TherapyType.body, color = TherapyColors.muted)
                }
            }
        }

        Card(modifier = Modifier.clickable(onClick = onCheckIn)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(Icons.Filled.EditNote)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.daily_check_in), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
                    Text(stringResource(R.string.how_are_you_today), style = TherapyType.body, color = TherapyColors.muted)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral)
            }
        }

        Card {
            Overline(stringResource(R.string.last_check_in), color = Color(0xFFCFA84A))
            Spacer(Modifier.height(14.dp))
            if (latest == null) Muted(stringResource(R.string.no_check_ins_yet))
            else {
                Row(Modifier.fillMaxWidth()) {
                    Score(Icons.Filled.Favorite, TherapyColors.navy, latest.mood, stringResource(R.string.legend_mood))
                    Score(Icons.Filled.Air, Color(0xFFCFA84A), latest.anxiety, stringResource(R.string.legend_anxiety))
                    Score(Icons.Filled.NightsStay, TherapyColors.success, latest.sleep, stringResource(R.string.legend_sleep))
                }
                Spacer(Modifier.height(12.dp))
                Muted(ApiTimestamp.parse(latest.doneAt)?.shortDateTime() ?: latest.doneAt)
            }
        }

        if (goals.isNotEmpty()) Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.height(20.dp)); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.homework_title), style = TherapyType.emphasisLarge, color = TherapyColors.ink, modifier = Modifier.weight(1f))
            }
            goals.take(3).forEach { Text(it.title, style = TherapyType.bodyLarge, color = TherapyColors.muted, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun RowScope.Score(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: Int?, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        IconCircle(icon, tint = tint, size = 44.dp)
        Spacer(Modifier.height(8.dp))
        Text("${value ?: "–"}/10", style = TherapyType.emphasisLarge, color = TherapyColors.ink)
        Text(label.replaceFirstChar { it.uppercase() }, style = TherapyType.caption, color = TherapyColors.muted)
    }
}
