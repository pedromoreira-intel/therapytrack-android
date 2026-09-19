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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiPlan
import com.therapytrack.android.core.ApiProfessionalSummary
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors

/** The iOS "Practice" tab: plan, licences, CPD, supervision, and the network features. */
@Composable
fun PracticeScreen(onOpen: (String) -> Unit) {
    val container = LocalContainer.current
    val plan by container.plan.collectAsState()
    var summary by remember { mutableStateOf<ApiProfessionalSummary?>(null) }

    LaunchedEffect(Unit) {
        container.refreshPlan()
        runCatching { summary = container.api.professionalSummary() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.practice_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)

        SectionTitle(stringResource(R.string.plan_section))
        PlanCard(plan)

        summary?.let { s ->
            Muted(stringResource(R.string.cpd_year_summary, s.year, fmtHours(s.supervisionHours.total), fmtHours(s.trainingHours.total)))
        }

        SectionTitle(stringResource(R.string.credentials_section))
        Card(modifier = Modifier.clickable { onOpen("credentials") }) {
            Text(stringResource(R.string.credentials_section), style = MaterialTheme.typography.titleMedium)
            summary?.credentials?.let { c ->
                // The most urgent licence decides the line: expired beats expiring beats due.
                val urgent = c.expired.firstOrNull() ?: c.expiringSoon.minByOrNull { it.daysUntilExpiry ?: 0 } ?: c.renewalDue.minByOrNull { it.daysUntilExpiry ?: 0 }
                if (urgent != null) { val (label, color) = credentialStatus(urgent); Pill("${urgent.licenseType} · $label", color) }
                else Muted("${c.total}")
            }
        }
        Card(modifier = Modifier.clickable { onOpen("training") }) {
            Text(stringResource(R.string.training_section), style = MaterialTheme.typography.titleMedium)
            summary?.let { Muted(stringResource(R.string.hours_this_year, fmtHours(it.trainingHours.total))) }
        }
        Card(modifier = Modifier.clickable { onOpen("supervision") }) {
            Text(stringResource(R.string.supervision_section), style = MaterialTheme.typography.titleMedium)
            summary?.let { Muted(stringResource(R.string.hours_this_year, fmtHours(it.supervisionHours.total))) }
        }

        SectionTitle(stringResource(R.string.network_section))
        Card(modifier = Modifier.clickable { onOpen("directory") }) { Text(stringResource(R.string.directory_title), style = MaterialTheme.typography.titleMedium) }
        Card(modifier = Modifier.clickable { onOpen("referrals") }) { Text(stringResource(R.string.referrals_title), style = MaterialTheme.typography.titleMedium) }
        Card(modifier = Modifier.clickable { onOpen("intervision") }) { Text(stringResource(R.string.intervision_title), style = MaterialTheme.typography.titleMedium) }
        Card(modifier = Modifier.clickable { onOpen("myprofile") }) { Text(stringResource(R.string.my_profile), style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PlanCard(plan: ApiPlan?) {
    Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) {
        if (plan == null) { Muted(stringResource(R.string.loading)); return@Card }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(when (plan.plan) { "network" -> R.string.plan_network; "professional" -> R.string.plan_professional; else -> R.string.plan_free }),
                style = MaterialTheme.typography.titleLarge, color = TherapyColors.navy, modifier = Modifier.weight(1f))
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
