package com.therapytrack.android.ui.therapist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiError
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.NotInPlanCard
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** What one AI call can come back with, as the screen renders it. */
sealed class AiOutcome {
    object Idle : AiOutcome()
    data class Ready(val json: JsonObject) : AiOutcome()
    /** 403: the client has not consented. Only they can change that. */
    object ConsentMissing : AiOutcome()
    data class NotInPlan(val error: ApiError.NotInPlan) : AiOutcome()
    data class Failed(val error: Throwable) : AiOutcome()
}

suspend fun aiCall(block: suspend () -> JsonObject): AiOutcome = try {
    val json = block()
    // A 200 that says it did not draft is not a draft. Never offer empty fields as one.
    val drafted = (json["drafted"] as? JsonPrimitive)?.contentOrNull ?: (json["ai_generated"] as? JsonPrimitive)?.contentOrNull
    if (drafted == "false") AiOutcome.Failed(ApiError.Server(200, (json["error"] as? JsonPrimitive)?.contentOrNull ?: (json["message"] as? JsonPrimitive)?.contentOrNull ?: ""))
    else AiOutcome.Ready(json)
} catch (e: ApiError.NotInPlan) { AiOutcome.NotInPlan(e) }
  catch (e: ApiError.Server) { if (e.status == 403) AiOutcome.ConsentMissing else AiOutcome.Failed(e) }
  catch (e: Exception) { AiOutcome.Failed(e) }

/** The JSON the model returned, rendered generically: strings as paragraphs, arrays as bullets, objects nested. */
@Composable
fun JsonView(element: JsonElement, depth: Int = 0) {
    when (element) {
        is JsonPrimitive -> Text(element.contentOrNull ?: element.toString(), style = MaterialTheme.typography.bodyMedium)
        is JsonArray -> element.forEach { item ->
            Row { Text("• ", style = MaterialTheme.typography.bodyMedium); Column { JsonView(item, depth + 1) } }
        }
        is JsonObject -> element.entries.filter { it.key !in hiddenKeys }.forEach { (key, value) ->
            Text(key.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = TherapyColors.navy,
                modifier = Modifier.padding(top = if (depth == 0) 10.dp else 4.dp))
            JsonView(value, depth + 1)
        }
    }
}
private val hiddenKeys = setOf("drafted", "ai_generated", "model", "generated_at", "disclaimer", "patient", "sources")

@Composable
fun AiResult(outcome: AiOutcome, onSeePlan: () -> Unit, extra: @Composable (JsonObject) -> Unit = {}) {
    val context = LocalContext.current
    when (outcome) {
        AiOutcome.Idle -> {}
        AiOutcome.ConsentMissing -> Card(tint = TherapyColors.rose.copy(alpha = 0.25f)) { Text(stringResource(R.string.ai_consent_missing)) }
        is AiOutcome.NotInPlan -> NotInPlanCard(outcome.error, onSeePlan)
        is AiOutcome.Failed -> ErrorText(errorText(outcome.error))
        is AiOutcome.Ready -> {
            val json = outcome.json
            Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Muted((json["disclaimer"] as? JsonPrimitive)?.contentOrNull ?: stringResource(R.string.ai_disclaimer)) }
            Card {
                JsonView(json)
                (json["sources"] as? JsonArray)?.takeIf { it.isNotEmpty() }?.let { s ->
                    Muted(stringResource(R.string.ai_sources) + ": " + s.joinToString(", ") { (it as? JsonPrimitive)?.contentOrNull ?: it.toString() }, Modifier.padding(top = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("draft", json.toString())) }) { Text(stringResource(R.string.copy)) }
                }
            }
            extra(json)
        }
    }
}

/** Summary, interventions and progress report for one client. Nothing here is saved anywhere. */
@Composable
fun AiToolsScreen(patientId: Int, patientName: String, onBack: () -> Unit, onSeePlan: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var notes by rememberSaveable { mutableStateOf("") }
    var concerns by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<AiOutcome>(AiOutcome.Idle) }
    fun run(block: suspend () -> JsonObject) { busy = true; scope.launch { outcome = aiCall(block); busy = false } }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.ai_section), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Muted(patientName)
        Muted(stringResource(R.string.ai_disclaimer))

        PrimaryButton(stringResource(R.string.ai_progress_report), loading = busy) { run { container.api.progressReport(patientId) } }
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.ai_notes_hint)) }, modifier = Modifier.fillMaxWidth().height(120.dp))
        PrimaryButton(stringResource(R.string.ai_summarize), enabled = notes.isNotBlank(), loading = busy) { run { container.api.summarizeSession(patientId, notes) } }
        OutlinedTextField(concerns, { concerns = it }, label = { Text(stringResource(R.string.ai_concerns_hint)) }, modifier = Modifier.fillMaxWidth())
        PrimaryButton(stringResource(R.string.ai_interventions), loading = busy) { run { container.api.suggestInterventions(patientId, concerns.ifBlank { null }) } }

        AiResult(outcome, onSeePlan)
        Spacer(Modifier.height(24.dp))
    }
}

/** Draft field values the note editor can take over — editable text, never auto-saved. */
data class NoteDraft(val focus: String?, val interventions: String?, val progressNotes: String?, val homework: String?, val plan: String?)

fun JsonObject.toNoteDraft(): NoteDraft {
    fun str(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull
    return NoteDraft(str("focus"), str("interventions"), str("progress_notes"), str("homework"), str("next_session_plan") ?: str("plan"))
}

/** Transcript → draft, shown inside the note editor. The therapist decides what to keep. */
@Composable
fun TranscriptDraftPanel(patientId: Int, onSeePlan: () -> Unit, onUse: (NoteDraft) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var transcript by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<AiOutcome>(AiOutcome.Idle) }

    Card(tint = TherapyColors.champagne.copy(alpha = 0.2f)) {
        Text(stringResource(R.string.ai_draft_from_transcript), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(transcript, { transcript = it }, label = { Text(stringResource(R.string.ai_transcript_hint)) }, modifier = Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.height(8.dp))
        PrimaryButton(stringResource(R.string.ai_generate), enabled = transcript.isNotBlank(), loading = busy) {
            busy = true; scope.launch { outcome = aiCall { container.api.draftNoteFromTranscript(patientId, transcript) }; busy = false }
        }
        Spacer(Modifier.height(8.dp))
        AiResult(outcome, onSeePlan) { json ->
            Muted(stringResource(R.string.ai_draft_ready), Modifier.padding(top = 6.dp))
            PrimaryButton(stringResource(R.string.ai_use_draft)) { onUse(json.toNoteDraft()) }
        }
    }
}
