package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiPlan
import com.therapytrack.android.core.ApiProfessionalSummary
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ListCard
import com.therapytrack.android.ui.common.ListRow
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType

/** The iOS "Prática": serif title, a segmented control, and one panel per segment. */
@Composable
fun PracticeScreen(onOpen: (String) -> Unit) {
    val container = LocalContainer.current
    val plan by container.plan.collectAsState()
    var summary by remember { mutableStateOf<ApiProfessionalSummary?>(null) }
    var segment by rememberSaveable { mutableStateOf("cpd") }

    LaunchedEffect(Unit) {
        container.refreshPlan()
        runCatching { summary = container.api.professionalSummary() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.practice_title), style = TherapyType.display, color = TherapyColors.navy)
        Segmented(listOf("cpd" to stringResource(R.string.seg_cpd), "licences" to stringResource(R.string.seg_licences), "peers" to stringResource(R.string.seg_peers), "plan" to stringResource(R.string.seg_plan)), segment, onSelect = { segment = it })

        when (segment) {
            "cpd" -> {
                Card(padding = 18.dp) {
                    Overline(stringResource(R.string.year_so_far, summary?.year ?: java.time.LocalDate.now().year), color = Color(0xFFCFA84A))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HoursTile(fmtHours(summary?.supervisionHours?.total ?: 0.0), stringResource(R.string.supervision_section), stringResource(R.string.sessions_count, summary?.supervisionHours?.sessionCount ?: 0), TherapyColors.navy)
                        HoursTile(fmtHours(summary?.trainingHours?.total ?: 0.0), stringResource(R.string.training_short), stringResource(R.string.records_count, summary?.trainingHours?.recordCount ?: 0), Color(0xFFCFA84A))
                    }
                }
                PrimaryButton(stringResource(R.string.log_supervision), icon = Icons.Outlined.Person) { onOpen("supervision") }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.training_courses), style = TherapyType.h2, color = TherapyColors.ink, modifier = Modifier.weight(1f))
                    TextButton({ onOpen("training") }) { Icon(Icons.Filled.AddCircle, null, tint = TherapyColors.navy); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add), style = TherapyType.emphasisLarge, color = TherapyColors.navy) }
                }
                Muted(stringResource(R.string.training_hint))
            }
            "licences" -> {
                summary?.credentials?.let { c ->
                    val urgent = c.expired.firstOrNull() ?: c.expiringSoon.minByOrNull { it.daysUntilExpiry ?: 0 } ?: c.renewalDue.minByOrNull { it.daysUntilExpiry ?: 0 }
                    if (urgent != null) { val (label, color) = credentialStatus(urgent); Card(tint = color.copy(alpha = 0.12f)) { Text(urgent.licenseType, style = TherapyType.emphasisLarge); Pill(label, color) } }
                }
                ListCard { ListRow(stringResource(R.string.credentials_section), subtitle = summary?.credentials?.let { "${it.total}" }, icon = Icons.Outlined.Badge, chevron = true, divider = false) { onOpen("credentials") } }
            }
            "peers" -> {
                ListCard {
                    ListRow(stringResource(R.string.directory_title), icon = Icons.Outlined.PersonSearch, chevron = true) { onOpen("directory") }
                    ListRow(stringResource(R.string.referrals_title), icon = Icons.Outlined.SwapHoriz, chevron = true) { onOpen("referrals") }
                    ListRow(stringResource(R.string.intervision_title), icon = Icons.Outlined.Groups, chevron = true) { onOpen("intervision") }
                    ListRow(stringResource(R.string.community_title), icon = Icons.Outlined.Forum, chevron = true) { onOpen("community") }
                    ListRow(stringResource(R.string.my_profile), icon = Icons.Outlined.Person, chevron = true, divider = false) { onOpen("myprofile") }
                }
            }
            else -> PlanCard(plan)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HoursTile(hours: String, title: String, detail: String, color: Color) {
    Column(Modifier.weight(1f).background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(14.dp)) {
        Text("${hours}h", style = TherapyType.metricLarge, color = color)
        Text(title, style = TherapyType.emphasisLarge, color = TherapyColors.ink)
        Muted(detail)
    }
}

@Composable
fun PlanCard(plan: ApiPlan?) {
    Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) {
        if (plan == null) { Muted(stringResource(R.string.loading)); return@Card }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(when (plan.plan) { "network" -> R.string.plan_network; "professional" -> R.string.plan_professional; else -> R.string.plan_free }),
                style = TherapyType.h2, color = TherapyColors.navy, modifier = Modifier.weight(1f))
            val expired = plan.effectiveReason != null
            Pill(stringResource(when { expired -> R.string.plan_status_expired; plan.status == "cancelled" -> R.string.plan_status_cancelled; else -> R.string.plan_status_active }),
                 if (expired || plan.status == "cancelled") TherapyColors.warning else TherapyColors.success)
        }
        plan.currentPeriodEnd?.let { Muted(stringResource(R.string.plan_renews, ApiTimestamp.parse(it)?.shortDate() ?: it)) }
        if (plan.plan == "free") plan.freeClientLimit?.let { Muted(stringResource(R.string.plan_free_limit, it)) }
        Muted(stringResource(R.string.plan_arranged), Modifier.padding(top = 6.dp))
    }
}

fun fmtHours(h: Double) = if (h == h.toLong().toDouble()) h.toLong().toString() else String.format("%.1f", h)
