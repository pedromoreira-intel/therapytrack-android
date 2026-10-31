package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import com.therapytrack.android.core.ApiCredential
import com.therapytrack.android.core.ApiIntervisionGroup
import com.therapytrack.android.core.ApiSupervisionSession
import com.therapytrack.android.core.ApiTraining
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.PageTitle
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
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

/**
 * The iOS "Prática": DPC, Licenças, Pares, Recursos — and inside each, one
 * level: rows that open their pages, never a second segmented control.
 * The plan lives on Perfil.
 */
@Composable
fun PracticeScreen(onOpen: (String) -> Unit) {
    val container = LocalContainer.current
    var summary by remember { mutableStateOf<ApiProfessionalSummary?>(null) }
    var credentials by remember { mutableStateOf<List<ApiCredential>>(emptyList()) }
    var training by remember { mutableStateOf<List<ApiTraining>>(emptyList()) }
    var supervision by remember { mutableStateOf<List<ApiSupervisionSession>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ApiIntervisionGroup>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var segment by rememberSaveable { mutableStateOf("cpd") }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        failed = runCatching { summary = container.api.professionalSummary() }.isFailure
        runCatching { credentials = container.api.credentials() }
        runCatching { training = container.api.training() }
        runCatching { supervision = container.api.supervisionSessions() }
        runCatching { groups = container.api.myIntervisionGroups() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageTitle(stringResource(R.string.practice_title))
        Segmented(listOf("cpd" to stringResource(R.string.seg_cpd), "licences" to stringResource(R.string.seg_licences), "peers" to stringResource(R.string.seg_peers),
            "resources" to stringResource(R.string.seg_resources)), segment, onSelect = { segment = it })

        when (segment) {
            "cpd" -> if (failed && summary == null) LoadFailed { reload++ } else CpdPanel(summary, training, supervision, onOpen)
            "licences" -> if (failed && summary == null) LoadFailed { reload++ } else LicencesPanel(credentials, onOpen)
            "peers" -> PeersPanel(groups, onOpen)
            else -> ResourcesPanel(onOpen)
        }
    }
}

@Composable
private fun LoadFailed(retry: () -> Unit) {
    Column {
        Text(stringResource(R.string.records_failed), style = TherapyType.body, color = TherapyColors.muted)
        TextButton(retry) { Text(stringResource(R.string.retry)) }
    }
}

/** "1,5 h" in Portuguese, "1.5 h" in English. */
@Composable
private fun hoursText(h: Double): String {
    val locale = com.therapytrack.android.ui.client.appLocale()
    return (if (h == h.toLong().toDouble()) h.toLong().toString() else String.format(locale, "%.1f", h)) + " h"
}

/** One dated list of supervision and training, newest first. */
private data class Activity(val key: String, val date: java.time.Instant?, val title: String, val detail: String, val hours: Double)

/** The year in one line, then everything recorded, newest first (REA-35). */
@Composable
private fun CpdPanel(summary: ApiProfessionalSummary?, training: List<ApiTraining>, supervision: List<ApiSupervisionSession>, onOpen: (String) -> Unit) {
    var showDefinition by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val trainingWord = stringResource(R.string.training_short)
    val supervisionWord = stringResource(R.string.supervision_section)
    val intervisionWord = stringResource(R.string.intervision_title)
    val activities = (training.map { Activity("t-${it.id}", ApiTimestamp.parse(it.completedOn), it.title, listOfNotNull(trainingWord, it.institution?.takeIf { s -> s.isNotBlank() }).joinToString(" · "), it.hours) } +
        supervision.map { Activity("s-${it.id}", ApiTimestamp.parse(it.sessionDate), it.supervisorName,
            if (it.supervisionType == "peer") intervisionWord else supervisionWord, it.hours) })
        .sortedByDescending { it.date ?: java.time.Instant.EPOCH }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // "DPC" alone means nothing to most people: one short line, the definition behind ⓘ.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cpd_short), style = TherapyType.caption, color = TherapyColors.muted)
            Box(Modifier.size(32.dp, 28.dp).clickable { showDefinition = !showDefinition }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, stringResource(R.string.cpd_what), tint = TherapyColors.navy, modifier = Modifier.size(16.dp))
            }
        }
        if (showDefinition) Text(stringResource(R.string.cpd_explained), style = TherapyType.body, color = TherapyColors.ink)

        summary?.let { s ->
            val c = s.credentials
            (c.expired + c.expiringSoon + c.renewalDue).forEach { RenewalWarning(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${s.year}", style = TherapyType.emphasis, color = TherapyColors.ink)
                Text("  ·  ", color = TherapyColors.neutral)
                val line = s.intervisionHours?.let { stringResource(R.string.cpd_year_line_intervision, hoursText(s.supervisionHours.total), hoursText(it.total), hoursText(s.trainingHours.total)) }
                    ?: stringResource(R.string.cpd_year_line, hoursText(s.supervisionHours.total), hoursText(s.trainingHours.total))
                Text(line, style = TherapyType.body, color = TherapyColors.muted)
            }
        }

        if (activities.isEmpty()) {
            Text(stringResource(R.string.cpd_nothing), style = TherapyType.body, color = TherapyColors.muted)
            PrimaryButton(stringResource(R.string.cpd_record_activity), icon = Icons.Filled.Add) { choosing = true }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.cpd_activity), Modifier.weight(1f))
                AddLink(stringResource(R.string.cpd_record)) { choosing = true }
            }
            CompactCard {
                val locale = com.therapytrack.android.ui.client.appLocale()
                val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", locale)
                activities.forEachIndexed { i, a ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(a.date?.atZone(java.time.ZoneId.systemDefault())?.format(fmt) ?: "—", style = TherapyType.caption, color = TherapyColors.muted, modifier = Modifier.width(50.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, style = TherapyType.emphasis, color = TherapyColors.ink, maxLines = 2)
                            Text(a.detail, style = TherapyType.caption, color = TherapyColors.muted)
                        }
                        if (a.hours > 0) Text(hoursText(a.hours), style = TherapyType.emphasis, color = TherapyColors.navy)
                    }
                    if (i < activities.lastIndex) RowDivider(78.dp)
                }
            }
        }
    }
    if (choosing) androidx.compose.material3.AlertDialog(
        onDismissRequest = { choosing = false },
        title = { Text(stringResource(R.string.cpd_record_activity)) },
        text = {
            Column {
                TextButton({ choosing = false; onOpen("supervision") }) { Text(supervisionWord) }
                TextButton({ choosing = false; onOpen("training") }) { Text(trainingWord) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ choosing = false }) { Text(stringResource(R.string.cancel)) } },
        containerColor = Color.White
    )
}

@Composable
private fun AddLink(text: String, onClick: () -> Unit) {
    Row(Modifier.heightIn(min = 36.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Add, null, tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = TherapyType.emphasis, color = TherapyColors.navy)
    }
}

@Composable
private fun RenewalWarning(c: ApiCredential) {
    val (label, color) = credentialStatus(c)
    Row(Modifier.fillMaxWidth().background(color.copy(alpha = 0.10f), RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Warning, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(c.licenseType, style = TherapyType.emphasis, color = TherapyColors.ink)
            Text(label, style = TherapyType.caption, color = color)
        }
    }
}

/** The same shape as DPC: a quiet label, "+ Adicionar" beside it, one prominent button only when there is nothing yet. */
@Composable
private fun LicencesPanel(credentials: List<ApiCredential>, onOpen: (String) -> Unit) {
    if (credentials.isEmpty()) {
        Text(stringResource(R.string.lic_none), style = TherapyType.body, color = TherapyColors.muted)
        PrimaryButton(stringResource(R.string.lic_add), icon = Icons.Filled.Add) { onOpen("credentials") }
        return
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(stringResource(R.string.credentials_section), Modifier.weight(1f))
        AddLink(stringResource(R.string.add)) { onOpen("credentials") }
    }
    // The next renewal first; registrations that never expire last.
    val sorted = credentials.sortedBy { it.daysUntilExpiry ?: Int.MAX_VALUE }
    val locale = com.therapytrack.android.ui.client.appLocale()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    CompactCard {
        sorted.forEachIndexed { i, c ->
            val (label, color) = credentialStatus(c)
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onOpen("credentials") }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(c.licenseType, style = TherapyType.emphasis, color = TherapyColors.ink)
                    listOfNotNull(c.issuingBody, c.licenseNumber).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.let {
                        Text(it.joinToString(" · "), style = TherapyType.caption, color = TherapyColors.muted)
                    }
                    val day = c.expiresOn?.let { ApiTimestamp.parse(it) ?: runCatching { java.time.LocalDate.parse(it.take(10)).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull() }
                        ?.atZone(java.time.ZoneId.systemDefault())?.format(fmt)
                    Text(when { day == null -> stringResource(R.string.lic_no_expiry); c.status == "expired" -> stringResource(R.string.lic_expired_on, day); else -> stringResource(R.string.lic_renews_on, day) },
                        style = TherapyType.caption, color = if (c.status == "active") TherapyColors.muted else color)
                }
                Spacer(Modifier.width(8.dp))
                Text(label, style = TherapyType.caption, color = color,
                    modifier = Modifier.background(color.copy(alpha = 0.10f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            if (i < sorted.lastIndex) RowDivider()
        }
    }
}

/** Your groups first, directly; discovery a quieter link under them; colleagues as rows that open their pages. */
@Composable
private fun PeersPanel(groups: List<ApiIntervisionGroup>, onOpen: (String) -> Unit) {
    val shown = groups.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.intervision_title) + if (groups.isNotEmpty()) " · ${groups.size}" else "")
        CompactCard {
            if (groups.isEmpty()) Text(stringResource(R.string.peers_no_group), style = TherapyType.body, color = TherapyColors.muted, modifier = Modifier.padding(14.dp))
            shown.forEachIndexed { i, g ->
                CompactLinkRow(Icons.Outlined.Groups, g.name, listOfNotNull(g.meetingSchedule, g.focusArea, g.description).firstOrNull { it.isNotBlank() }) { onOpen("group/${g.id}") }
                if (i < shown.lastIndex || groups.size > shown.size) RowDivider(50.dp)
            }
            if (groups.size > shown.size) Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { onOpen("intervision") }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.peers_see_all, groups.size), style = TherapyType.body, color = TherapyColors.navy, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral, modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.heightIn(min = 36.dp).clickable { onOpen("intervision") }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.peers_discover), style = TherapyType.body, color = TherapyColors.navy)
        }
        SectionLabel(stringResource(R.string.peers_colleagues), Modifier.padding(top = 12.dp))
        CompactCard {
            CompactLinkRow(Icons.Outlined.PersonSearch, stringResource(R.string.peers_find), stringResource(R.string.peers_find_sub)) { onOpen("directory") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.SwapHoriz, stringResource(R.string.referrals_title), stringResource(R.string.peers_referrals_sub)) { onOpen("referrals") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.Person2, stringResource(R.string.supervision_title), stringResource(R.string.peers_supervision_sub)) { onOpen("supervision") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.Forum, stringResource(R.string.community_title)) { onOpen("community") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.Person, stringResource(R.string.my_profile)) { onOpen("myprofile") }
        }
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
