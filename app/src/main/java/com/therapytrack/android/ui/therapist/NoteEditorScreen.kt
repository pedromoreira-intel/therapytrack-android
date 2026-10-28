package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiSessionTopic
import com.therapytrack.android.core.BriefLastSession
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.offline.SessionNoteDraft
import com.therapytrack.android.ui.common.LabelledValue
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What Portuguese clinicians write, not acronyms in English. */
private val interventionOptions = listOf(
    "Psicoeducação", "Reestruturação cognitiva", "Exposição", "Ativação comportamental",
    "Resolução de problemas", "Mindfulness", "Relaxamento", "Regulação emocional"
)

/**
 * The note as a page to write on rather than a form to fill, opened from a
 * session, so the client and the time are already known. Same as iOS
 * SessionNoteEditor:
 *  · the draft saves itself on this phone as it is typed ("Rascunho guardado");
 *  · "Concluir" files the note in the client's record, through the same
 *    offline-safe outbox as before;
 *  · closing keeps the draft; throwing it away is a separate, confirmed act;
 *  · risk is an explicit choice the note cannot be finished without, at the
 *    end, once written — never defaulted.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(patientId: Int, patientName: String, sessionAt: Instant, sessionId: Int?, onClose: () -> Unit, onSeePlan: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The session's day where the therapist is, not in UTC: an evening session must not be filed under tomorrow.
    val day = remember(sessionAt) { sessionAt.atZone(ZoneId.systemDefault()).toLocalDate().toString() }
    val userId = container.client.currentUserId
    val key = remember(sessionId, day) { container.noteDrafts.key(userId, patientId, sessionId, day) }

    var draft by remember { mutableStateOf(SessionNoteDraft()) }
    var loaded by remember { mutableStateOf(false) }
    var savedOnce by remember { mutableStateOf(false) }
    var topics by remember { mutableStateOf<List<ApiSessionTopic>>(emptyList()) }
    var last by remember { mutableStateOf<BriefLastSession?>(null) }
    var showLast by remember { mutableStateOf(true) }
    var showMissing by remember { mutableStateOf(false) }
    var filing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var queued by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    var riskY by remember { mutableIntStateOf(0) }
    var notesY by remember { mutableIntStateOf(0) }

    LaunchedEffect(key) {
        val cached = container.noteDrafts.load(key)
            // A draft begun before drafts were kept per session sits under the day; it moves on the next save.
            ?: if (sessionId != null) container.noteDrafts.key(userId, patientId, null, day).let { legacy ->
                container.noteDrafts.load(legacy)?.also { container.noteDrafts.remove(legacy) }
            } else null
        if (cached != null) { draft = cached; savedOnce = true }
        loaded = true
        runCatching { topics = container.api.sessionTopics(patientId).open }
        runCatching { last = container.api.brief(patientId).lastSession }
    }
    // Saved a moment after typing stops, not on every keystroke.
    LaunchedEffect(draft) {
        if (!loaded) return@LaunchedEffect
        delay(600)
        runCatching { container.noteDrafts.save(key, draft) }
        savedOnce = !draft.isEmpty
    }

    val missingNotes = draft.notes.isBlank()
    val missingRisk = draft.risk == null

    fun file() {
        if (missingNotes || missingRisk) {
            showMissing = true
            // Bring what is missing into view: the risk level sits at the end, out of sight while the top is read.
            scope.launch { scroll.animateScrollTo(if (missingNotes) notesY else riskY) }
            return
        }
        filing = true; errorText = null
        scope.launch {
            val outcome = container.sessionNotes.save(
                patientId, patientName, day, draft.focus.trim().ifBlank { null },
                // The server requires something here; an unticked list is said plainly.
                if (draft.interventions.isEmpty()) context.getString(R.string.ne_not_recorded) else draft.interventions.joinToString(", "),
                draft.notes.trim(), draft.homework.trim().ifBlank { null }, draft.risk ?: "LOW", draft.plan.trim().ifBlank { null }, sessionId)
            filing = false
            if (outcome != SaveOutcome.NOT_STORED) {
                // The topics ticked as talked about leave the client's open list.
                draft.discussedTopics.filter { id -> topics.any { it.id == id } }.forEach { runCatching { container.api.setSessionTopicDiscussed(it, true) } }
                container.noteDrafts.remove(key)
            }
            when (outcome) {
                SaveOutcome.SENT -> onClose()
                SaveOutcome.QUEUED -> queued = true
                // The draft stays: it is the only copy.
                SaveOutcome.NOT_STORED -> errorText = context.getString(R.string.ne_not_stored)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        // Top bar: close keeps the draft; the state of the draft; discard; Concluir.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.07f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, stringResource(R.string.ne_close_keep), tint = TherapyColors.navy, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(when { filing -> R.string.ne_filing; savedOnce -> R.string.ne_draft_saved; else -> R.string.ne_draft }),
                style = TherapyType.caption, color = TherapyColors.neutral, modifier = Modifier.weight(1f))
            Box {
                Box(Modifier.size(36.dp).clip(CircleShape).clickable(enabled = !draft.isEmpty) { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MoreHoriz, null, tint = if (draft.isEmpty) TherapyColors.neutral else TherapyColors.navy)
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.ne_discard_draft), color = TherapyColors.critical) }, onClick = { menu = false; confirmDiscard = true })
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ne_finish), style = TherapyType.emphasis, color = Color.White,
                modifier = Modifier.clip(CircleShape).background(TherapyColors.navy).clickable(enabled = !filing) { file() }.padding(horizontal = 16.dp, vertical = 9.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.padding(top = 4.dp)) {
                Text(patientName, style = TherapyType.display, color = TherapyColors.navy, maxLines = 2)
                Text(sessionAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE, d MMMM, HH:mm", com.therapytrack.android.ui.client.appLocale())),
                    style = TherapyType.caption, color = TherapyColors.neutral)
            }

            // An AI draft, where the plan allows it, fills the fields below as editable text; nothing is filed until Concluir.
            TranscriptDraftPanel(patientId, onSeePlan) { d ->
                draft = draft.copy(
                    focus = d.focus ?: draft.focus,
                    interventions = d.interventions?.split(',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: draft.interventions,
                    notes = d.progressNotes ?: draft.notes, homework = d.homework ?: draft.homework, plan = d.plan ?: draft.plan)
            }

            // Context first: what was agreed, what the client brought; then the note.
            last?.takeIf { !it.plan.isNullOrBlank() || !it.homeworkSet.isNullOrBlank() }?.let { l ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { showLast = !showLast }, verticalAlignment = Alignment.CenterVertically) {
                        Overline(stringResource(R.string.ne_from_last), modifier = Modifier.weight(1f))
                        Icon(if (showLast) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = TherapyColors.neutral, modifier = Modifier.size(18.dp))
                    }
                    if (showLast) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TherapyColors.canvas).padding(12.dp)) {
                        l.plan?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_agreed_next_steps), it) }
                        l.homeworkSet?.takeIf { it.isNotBlank() }?.let { LabelledValue(stringResource(R.string.sf_homework_set), it) }
                    }
                }
            }

            if (topics.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Overline(stringResource(R.string.ne_wanted_to_talk, patientName.substringBefore(' ')))
                topics.forEach { topic ->
                    val ticked = topic.id in draft.discussedTopics
                    Row(verticalAlignment = Alignment.Top) {
                        val label = stringResource(if (ticked) R.string.ne_talked_about else R.string.ne_not_talked)
                        Icon(if (ticked) Icons.Filled.CheckCircle else Icons.Outlined.Circle, label,
                            tint = if (ticked) TherapyColors.navy else TherapyColors.neutral,
                            modifier = Modifier.size(28.dp).clip(CircleShape).clickable {
                                draft = draft.copy(discussedTopics = if (ticked) draft.discussedTopics - topic.id else draft.discussedTopics + topic.id)
                            }.padding(3.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(topic.text, style = TherapyType.body, color = TherapyColors.ink, modifier = Modifier.weight(1f).padding(top = 4.dp).clickable {
                            // Tapping a topic adds it to the focus and ticks it.
                            val focus = if (draft.focus.contains(topic.text)) draft.focus else if (draft.focus.isEmpty()) topic.text else draft.focus + "; " + topic.text
                            draft = draft.copy(focus = focus, discussedTopics = if (ticked) draft.discussedTopics else draft.discussedTopics + topic.id)
                        })
                    }
                }
                Text(stringResource(R.string.ne_tick_hint), style = TherapyType.caption, color = TherapyColors.neutral)
            }

            NoteSection(stringResource(R.string.sf_focus), draft.focus, stringResource(R.string.ne_focus_prompt), 1) { draft = draft.copy(focus = it) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Overline(stringResource(R.string.sf_interventions))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    interventionOptions.forEach { option ->
                        val on = option in draft.interventions
                        Text(option, style = TherapyType.caption, color = if (on) Color.White else TherapyColors.navy,
                            modifier = Modifier.clip(CircleShape).background(if (on) TherapyColors.navy else TherapyColors.navy.copy(alpha = 0.07f))
                                .clickable { draft = draft.copy(interventions = if (on) draft.interventions - option else draft.interventions + option) }
                                .padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            Column(Modifier.onPlaced { notesY = it }) {
                NoteSection(stringResource(R.string.sf_notes), draft.notes, stringResource(R.string.ne_notes_prompt), 6) { draft = draft.copy(notes = it) }
                if (showMissing && missingNotes) Text(stringResource(R.string.ne_write_notes), style = TherapyType.caption, color = TherapyColors.critical)
            }
            NoteSection(stringResource(R.string.sf_homework), draft.homework, stringResource(R.string.ne_homework_prompt), 2) { draft = draft.copy(homework = it) }
            NoteSection(stringResource(R.string.sf_next_session), draft.plan, stringResource(R.string.ne_plan_prompt), 2) { draft = draft.copy(plan = it) }

            // The closing judgement, after the note. Neutral until chosen: coloured chips at the top of an empty note read as an alarm.
            Column(Modifier.onPlaced { riskY = it }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Overline(stringResource(R.string.ne_risk))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW" to R.string.risk_low_c, "MODERATE" to R.string.risk_moderate_c, "HIGH" to R.string.risk_high_c, "CRITICAL" to R.string.risk_critical_c).forEach { (value, label) ->
                        val on = draft.risk == value
                        val tint = when (value) { "CRITICAL", "HIGH" -> TherapyColors.critical; "MODERATE" -> TherapyColors.warning; else -> TherapyColors.success }
                        Text(stringResource(label), style = TherapyType.emphasis, color = if (on) Color.White else TherapyColors.ink,
                            modifier = Modifier.clip(CircleShape).background(if (on) tint else Color.Transparent)
                                .border(1.dp, if (on) Color.Transparent else TherapyColors.hairline, CircleShape)
                                .clickable { draft = draft.copy(risk = value) }.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                if (missingRisk) Text(stringResource(R.string.ne_choose_risk), style = TherapyType.caption, color = if (showMissing) TherapyColors.critical else TherapyColors.neutral)
            }
            errorText?.let { Text(it, style = TherapyType.caption, color = TherapyColors.critical) }
            Spacer(Modifier.heightIn(min = 40.dp))
        }
    }

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.ne_discard_q)) },
        text = { Text(stringResource(R.string.ne_discard_detail)) },
        confirmButton = { TextButton({ confirmDiscard = false; container.noteDrafts.remove(key); onClose() }) { Text(stringResource(R.string.ne_discard_draft), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
        containerColor = Color.White
    )
    if (queued) AlertDialog(
        onDismissRequest = { queued = false; onClose() },
        title = { Text(stringResource(R.string.ne_saved_device)) },
        text = { Text(stringResource(R.string.ne_saved_device_detail)) },
        confirmButton = { TextButton({ queued = false; onClose() }) { Text(stringResource(R.string.ok)) } },
        containerColor = Color.White
    )
}

/** A heading and a place to write under it, growing with the text. */
@Composable
private fun NoteSection(title: String, value: String, prompt: String, minLines: Int, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Overline(title)
        Box {
            if (value.isEmpty()) Text(prompt, style = TherapyType.bodyLarge, color = TherapyColors.neutral)
            BasicTextField(value, onChange, minLines = minLines, textStyle = TherapyType.bodyLarge.copy(color = TherapyColors.ink),
                cursorBrush = SolidColor(TherapyColors.navy), modifier = Modifier.fillMaxWidth().semantics { contentDescription = title })
        }
    }
}

/** Where a block sits in the scrolling column, so Concluir can bring it into view. */
private fun Modifier.onPlaced(onY: (Int) -> Unit) =
    this.onGloballyPositioned { coords -> onY(coords.positionInParent().y.toInt()) }
