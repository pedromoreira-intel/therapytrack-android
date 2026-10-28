package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiJournalEntry
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.PendingJournalEntry
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.CapsuleButton
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.PageTitle
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.common.SupportButton
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val prompts = listOf(R.string.journal_prompt_1, R.string.journal_prompt_2, R.string.journal_prompt_3)

/**
 * Writing first: the page opens on a place to write, with who will read it
 * chosen in the open before saving, and the entries in one list underneath.
 *
 * Was a list of cards with a + button that opened a separate form, where who
 * reads it was a switch labelled "Só para mim". Same page as iOS JournalView.
 */
@Composable
fun JournalScreen(onCrisis: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<ApiJournalEntry>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingJournalEntry>>(emptyList()) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.journal.changes.collectAsState()

    var text by rememberSaveable { mutableStateOf("") }
    var prompt by rememberSaveable { mutableStateOf<Int?>(null) }
    // Who reads it: shared by default, as on iOS, and said in the open.
    var share by rememberSaveable { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<ApiJournalEntry?>(null) }

    LaunchedEffect(reload, outboxTick) {
        pending = container.journal.mine()
        runCatching { entries = container.api.journalEntries() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(stringResource(R.string.journal_title)) { SupportButton(onCrisis) }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // One title on the page: the question is ordinary text, not a second heading.
            Text(stringResource(prompt?.let { prompts[it] } ?: R.string.journal_question), style = TherapyType.bodyLarge, color = TherapyColors.muted)
            val label = stringResource(R.string.journal_entry_label)
            BasicTextField(
                text, { text = it; savedNote = null },
                textStyle = TherapyType.bodyLarge.copy(color = TherapyColors.ink),
                cursorBrush = SolidColor(TherapyColors.navy),
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .border(1.dp, if (focused) TherapyColors.navy.copy(alpha = 0.4f) else TherapyColors.hairline, RoundedCornerShape(12.dp))
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label }
                    .padding(14.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clickable { prompt = ((prompt ?: -1) + 1) % prompts.size }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (prompt == null) R.string.journal_suggestion else R.string.journal_another_suggestion), style = TherapyType.body, color = TherapyColors.navy)
                }
                if (prompt != null) Text(stringResource(R.string.journal_write_freely), style = TherapyType.body, color = TherapyColors.muted,
                    modifier = Modifier.padding(start = 16.dp).clickable { prompt = null }.padding(vertical = 8.dp))
            }
        }

        // Who will read it, before saving.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Segmented(listOf("shared" to stringResource(R.string.share_with_therapist), "private" to stringResource(R.string.only_for_me)),
                if (share) "shared" else "private", { share = it == "shared" })
            Text(stringResource(if (share) R.string.entry_will_be_shared else R.string.entry_only_you), style = TherapyType.caption, color = TherapyColors.muted)
        }

        if (failed) Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Warning, null, tint = TherapyColors.critical, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.journal_not_saved), style = TherapyType.caption, color = TherapyColors.critical)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val note = savedNote
            if (note != null) {
                Icon(Icons.Filled.CheckCircle, null, tint = TherapyColors.success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(note, style = TherapyType.caption, color = TherapyColors.success, modifier = Modifier.weight(1f))
            } else Spacer(Modifier.weight(1f))
            CapsuleButton(stringResource(R.string.save_entry), enabled = text.isNotBlank() && !saving) {
                saving = true; failed = false
                scope.launch {
                    val promptTitle = prompt?.let { context.getString(prompts[it]) }
                    val outcome = container.journal.save(promptTitle, text.trim(), !share)
                    saving = false
                    val time = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                    when (outcome) {
                        // The draft stays on screen: nothing is cleared until the outbox has it.
                        SaveOutcome.NOT_STORED -> failed = true
                        SaveOutcome.SENT -> { text = ""; prompt = null; savedNote = context.getString(R.string.journal_saved_at, time); reload++ }
                        SaveOutcome.QUEUED -> { text = ""; prompt = null; savedNote = context.getString(R.string.journal_saved_on_phone, time); reload++ }
                    }
                }
            }
        }

        if (pending.isNotEmpty() || entries.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.your_entries), Modifier.padding(top = 8.dp))
            CompactCard {
                // What was written but not yet delivered, shown so the person sees what they wrote, not only what arrived.
                pending.forEach { p ->
                    EntryRow(Instant.ofEpochMilli(p.createdAtMillis).shortDateTime(), stringResource(R.string.waiting_to_send), TherapyColors.warning, p.title, p.content)
                    RowDivider()
                }
                entries.forEachIndexed { i, e ->
                    EntryRow(ApiTimestamp.parse(e.createdAt)?.shortDateTime() ?: e.createdAt,
                        stringResource(if (e.isPrivate) R.string.private_label else R.string.shared_label),
                        if (e.isPrivate) TherapyColors.muted else TherapyColors.navy, e.title, e.content) { opened = e }
                    if (i < entries.lastIndex) RowDivider()
                }
            }
        }
    }

    opened?.let { e ->
        var confirmDelete by remember(e.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { opened = null },
            title = { Text(e.title?.takeIf { it.isNotBlank() } ?: (ApiTimestamp.parse(e.createdAt)?.shortDateTime() ?: "")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(e.content, style = TherapyType.bodyLarge) } },
            confirmButton = { TextButton({ opened = null }) { Text(stringResource(R.string.close)) } },
            dismissButton = {
                // Deleting asks twice: the first tap turns the button into the question.
                TextButton({
                    if (!confirmDelete) confirmDelete = true
                    else { scope.launch { runCatching { container.api.deleteJournalEntry(e.id) }; reload++ }; opened = null }
                }) { Text(stringResource(if (confirmDelete) R.string.delete_entry else R.string.delete), color = TherapyColors.critical) }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun EntryRow(date: String, status: String, statusColor: Color, title: String?, content: String, onClick: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, style = TherapyType.caption, color = TherapyColors.muted, modifier = Modifier.weight(1f))
            Text(status, style = TherapyType.caption, color = statusColor)
        }
        title?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.emphasis, color = TherapyColors.ink) }
        Text(content, style = TherapyType.body, color = TherapyColors.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
