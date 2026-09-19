package com.therapytrack.android.ui.therapist

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiDiscussion
import com.therapytrack.android.core.ApiDiscussionMessage
import com.therapytrack.android.core.ApiGroupDetail
import com.therapytrack.android.core.ApiIntervisionGroup
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Bubble
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Composer
import com.therapytrack.android.ui.common.Loaded
import com.therapytrack.android.ui.common.MessageList
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.NotInPlanCard
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.SectionTitle
import com.therapytrack.android.ui.common.load
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun IntervisionScreen(onBack: () -> Unit, onSeePlan: () -> Unit, onGroup: (Int) -> Unit) {
    val container = LocalContainer.current
    var tab by rememberSaveable { mutableStateOf("mine") }
    var mine by remember { mutableStateOf<Loaded<List<ApiIntervisionGroup>>>(Loaded.Loading) }
    var all by remember { mutableStateOf<Loaded<List<ApiIntervisionGroup>>>(Loaded.Loading) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var joining by remember { mutableStateOf<ApiIntervisionGroup?>(null) }
    var joinMessage by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        mine = load { container.api.myIntervisionGroups() }
        all = load { container.api.intervisionGroups() }
    }
    if (creating) { NewGroupForm { creating = false; reload++ }; return }

    val mineIds = (mine as? Loaded.Ok)?.value?.map { it.id }?.toSet() ?: emptySet()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.intervision_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(tab == "mine", { tab = "mine" }, label = { Text(stringResource(R.string.my_groups)) })
            FilterChip(tab == "all", { tab = "all" }, label = { Text(stringResource(R.string.discover_groups)) })
        }
        PrimaryButton(stringResource(R.string.new_group)) { creating = true }
        val shown = if (tab == "mine") mine else all
        when (val r = shown) {
            is Loaded.Loading -> Muted(stringResource(R.string.loading))
            is Loaded.NotInPlan -> NotInPlanCard(r.error, onSeePlan)
            is Loaded.Failed -> Muted(stringResource(R.string.could_not_load))
            is Loaded.Ok -> {
                val groups = if (tab == "all") r.value.filter { it.id !in mineIds } else r.value
                if (groups.isEmpty()) Muted(stringResource(if (tab == "mine") R.string.no_my_groups else R.string.no_groups))
                groups.forEach { g ->
                    Card(modifier = Modifier.clickable(enabled = tab == "mine") { onGroup(g.id) }) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (g.isOnline) Pill(stringResource(R.string.offers_online), TherapyColors.navy)
                        }
                        g.focusArea?.let { Muted(it) }
                        g.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Muted(listOfNotNull(g.meetingSchedule, g.memberCount.takeIf { it > 0 }?.let { stringResource(R.string.members_count, it, g.maxMembers) }, g.moderatorName?.let { stringResource(R.string.moderator, it) }).joinToString(" · "))
                        if (tab == "all") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton({ joining = g; joinMessage = "" }) { Text(stringResource(R.string.request_to_join)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    joining?.let { g ->
        AlertDialog(onDismissRequest = { joining = null }, title = { Text(g.name) },
            text = { OutlinedTextField(joinMessage, { joinMessage = it }, label = { Text(stringResource(R.string.join_message)) }) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.requestToJoin(g.id, joinMessage.ifBlank { null }) }; reload++ }; joining = null }) { Text(stringResource(R.string.request_to_join)) } },
            dismissButton = { TextButton({ joining = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun NewGroupForm(onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var focus by rememberSaveable { mutableStateOf("") }
    var schedule by rememberSaveable { mutableStateOf("") }
    var online by rememberSaveable { mutableStateOf(true) }
    var max by rememberSaveable { mutableStateOf("8") }
    var link by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    FormFrame(stringResource(R.string.new_group), onCancel = onDone, canSave = name.isNotBlank() && (max.toIntOrNull() ?: 0) > 1, busy = busy, error = error, saveLabel = stringResource(R.string.create_group), onSave = {
        busy = true; error = null
        scope.launch {
            try { container.api.createIntervisionGroup(name, description.ifBlank { null }, focus.ifBlank { null }, schedule.ifBlank { null }, online, max.toInt(), link.ifBlank { null }); onDone() }
            catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Field(name, { name = it }, R.string.group_name)
        Field(description, { description = it }, R.string.description, single = false)
        Field(focus, { focus = it }, R.string.focus_area)
        Field(schedule, { schedule = it }, R.string.meeting_schedule)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.offers_online), Modifier.weight(1f)); Switch(online, { online = it }) }
        Field(max, { max = it }, R.string.max_members, keyboard = KeyboardType.Number)
        Field(link, { link = it }, R.string.meeting_link)
    }
}

@Composable
fun GroupScreen(groupId: Int, onBack: () -> Unit, onDiscussion: (Int, String) -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf<ApiGroupDetail?>(null) }
    var discussions by remember { mutableStateOf<List<ApiDiscussion>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var newDiscussion by remember { mutableStateOf<String?>(null) }
    var newMeeting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(groupId, reload) {
        failed = runCatching { group = container.api.intervisionGroup(groupId); discussions = container.api.discussions(groupId) }.isFailure
        runCatching { container.api.markNotificationsRead("join_request", groupId); container.api.markNotificationsRead("join_response", groupId) }; container.refreshInbox()
    }
    if (newMeeting) { NewMeetingForm(groupId) { newMeeting = false; reload++ }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        val g = group
        Text(g?.name ?: "…", style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        if (failed) Muted(stringResource(R.string.could_not_load))
        if (g == null) return@Column
        g.description?.let { Text(it) }
        Muted(listOfNotNull(g.focusArea, g.meetingSchedule, g.moderatorName?.let { stringResource(R.string.moderator, it) }).joinToString(" · "))
        g.meetingLink?.takeIf { it.isNotBlank() }?.let { link ->
            TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }) { Text(stringResource(R.string.open_link)) }
        }
        Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Muted(stringResource(R.string.intervision_confidentiality)) }

        if (g.myRole == "moderator" && g.requests.isNotEmpty()) {
            SectionTitle(stringResource(R.string.pending_requests))
            g.requests.forEach { r ->
                Card {
                    Text(r.userName ?: "#${r.userId}", style = MaterialTheme.typography.titleMedium)
                    r.message?.let { Muted(it) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ scope.launch { runCatching { container.api.reviewJoinRequest(r.id, false) }; reload++ } }) { Text(stringResource(R.string.reject), color = TherapyColors.critical) }
                        TextButton({ scope.launch { runCatching { container.api.reviewJoinRequest(r.id, true) }; reload++ } }) { Text(stringResource(R.string.approve)) }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.discussions))
        TextButton({ newDiscussion = "" }) { Text(stringResource(R.string.new_discussion)) }
        if (discussions.isEmpty()) Muted(stringResource(R.string.no_discussions))
        discussions.forEach { d ->
            Card(modifier = Modifier.clickable { onDiscussion(d.id, d.title) }) {
                Text(d.title, style = MaterialTheme.typography.titleMedium)
                Muted(listOfNotNull(d.creatorName, stringResource(R.string.messages_count, d.messageCount), ApiTimestamp.parse(d.createdAt)?.shortDateTime()).joinToString(" · "))
            }
        }

        SectionTitle(stringResource(R.string.meetings))
        TextButton({ newMeeting = true }) { Text(stringResource(R.string.new_meeting)) }
        if (g.meetings.isEmpty()) Muted(stringResource(R.string.no_meetings))
        g.meetings.forEach { m ->
            Card {
                Text(m.title, style = MaterialTheme.typography.titleMedium)
                Muted("${ApiTimestamp.parse(m.scheduledAt)?.shortDateTime() ?: m.scheduledAt} · ${m.durationMinutes} min")
                m.agenda?.let { Text(it) }
                m.meetingLink?.takeIf { it.isNotBlank() }?.let { link -> TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }) { Text(stringResource(R.string.open_link)) } }
            }
        }

        SectionTitle(stringResource(R.string.members))
        g.members.forEach { m -> Row { Text(m.userName ?: "#${m.userId}", Modifier.weight(1f)); if (m.role == "moderator") Pill(stringResource(R.string.moderator, "").trim(' ', ':'), TherapyColors.navy) } }
        Spacer(Modifier.height(24.dp))
    }

    newDiscussion?.let { title ->
        AlertDialog(onDismissRequest = { newDiscussion = null }, title = { Text(stringResource(R.string.new_discussion)) },
            text = { OutlinedTextField(title, { newDiscussion = it }, label = { Text(stringResource(R.string.discussion_title)) }) },
            confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { scope.launch { runCatching { container.api.createDiscussion(groupId, title) }; reload++ }; newDiscussion = null }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ newDiscussion = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun NewMeetingForm(groupId: Int, onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var agenda by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var time by rememberSaveable { mutableStateOf("19:00") }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val whenOk = runCatching { java.time.LocalDateTime.parse("${date}T$time:00") }.isSuccess

    FormFrame(stringResource(R.string.new_meeting), onCancel = onDone, canSave = title.isNotBlank() && whenOk && minutes.toIntOrNull() != null, busy = busy, error = error, onSave = {
        busy = true; error = null
        scope.launch {
            try {
                val at = java.time.LocalDateTime.parse("${date}T$time:00").atZone(java.time.ZoneId.systemDefault()).toInstant()
                container.api.createMeeting(groupId, title, agenda.ifBlank { null }, link.ifBlank { null }, ApiTimestamp.iso8601(at), minutes.toInt()); onDone()
            } catch (e: Exception) { error = e } finally { busy = false }
        }
    }) {
        Field(title, { title = it }, R.string.training_title)
        Field(agenda, { agenda = it }, R.string.agenda, single = false)
        Field(date, { date = it }, R.string.date_yyyy_mm_dd, isError = !whenOk)
        Field(time, { time = it }, R.string.time_hh_mm, isError = !whenOk)
        Field(minutes, { minutes = it }, R.string.duration_minutes, keyboard = KeyboardType.Number)
        Field(link, { link = it }, R.string.meeting_link)
    }
}

/** A group discussion. Plain requests: conversational text, cheaply retyped — no outbox, as on iOS. */
@Composable
fun DiscussionScreen(discussionId: Int, title: String, onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val me = container.client.currentUserId
    var messages by remember { mutableStateOf<List<ApiDiscussionMessage>>(emptyList()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(discussionId, reload) { runCatching { messages = container.api.discussionMessages(discussionId) } }
    val bubbles = messages.map { Bubble("m${it.id}", it.userId == me, it.content, ApiTimestamp.parse(it.postedAt)?.shortDateTime() ?: "", it.userName) }
    LaunchedEffect(bubbles.size) { if (bubbles.isNotEmpty()) listState.animateScrollToItem(bubbles.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 4.dp))
        Muted(stringResource(R.string.intervision_confidentiality), Modifier.padding(horizontal = 20.dp))
        MessageList(bubbles, listState, Modifier.weight(1f)) { Muted(stringResource(R.string.no_messages), Modifier.padding(top = 24.dp)) }
        if (failed) com.therapytrack.android.ui.common.ErrorText(stringResource(R.string.error_generic))
        Composer(draft, { draft = it }, stringResource(R.string.write_reply), stringResource(R.string.post), enabled = draft.isNotBlank()) {
            val text = draft
            scope.launch {
                failed = runCatching { container.api.postDiscussionMessage(discussionId, text) }.isFailure
                if (!failed) { draft = ""; reload++ }
            }
        }
    }
}
