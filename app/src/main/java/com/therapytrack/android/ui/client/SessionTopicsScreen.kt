package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiSessionTopic
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDate
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.util.UUID

// What the client wants to bring to their next session (REA-22), as on iOS.
//
// The client's half of "where we left off": noted whenever it comes up, on the
// therapist's brief before the session. Written to be read, so there is no
// private choice; the words are the client's, so only they edit or delete them.

/** The home-screen card: the first few topics, and the way in. */
@Composable
fun SessionTopicsCard(topics: List<ApiSessionTopic>, onOpen: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Chat, null, tint = TherapyColors.navy, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.topics_title), style = TherapyType.emphasisLarge, color = TherapyColors.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.AddCircle, null, tint = TherapyColors.navy, modifier = Modifier.size(24.dp))
        }
        if (topics.isEmpty()) Muted(stringResource(R.string.topics_empty_hint), Modifier.padding(top = 10.dp))
        else {
            topics.take(3).forEach { t ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Spacer(Modifier.padding(top = 8.dp).size(6.dp).background(TherapyColors.navy.copy(alpha = 0.5f), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(t.text, style = TherapyType.bodyLarge, color = TherapyColors.ink, maxLines = 2)
                }
            }
            if (topics.size > 3) Muted(stringResource(R.string.topics_more, topics.size - 3), Modifier.padding(top = 6.dp))
        }
    }
}

/** The client's list: add, edit, delete, and mark as talked about. */
@Composable
fun SessionTopicsScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf<List<ApiSessionTopic>>(emptyList()) }
    var discussed by remember { mutableStateOf<List<ApiSessionTopic>>(emptyList()) }
    var draft by rememberSaveable { mutableStateOf("") }
    // The same id on a retry: if the first attempt arrived and only the reply
    // was lost, the server keeps one topic.
    var draftId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var editing by remember { mutableStateOf<ApiSessionTopic?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        try { container.api.sessionTopics().let { open = it.open; discussed = it.discussed }; error = null } catch (e: Exception) { error = e }
    }

    fun act(block: suspend () -> Unit) {
        scope.launch { try { block(); error = null; reload++ } catch (e: Exception) { error = e } }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("‹ " + stringResource(R.string.back), style = TherapyType.bodyLarge, color = TherapyColors.navy)
        }
        Text(stringResource(R.string.topics_title), style = TherapyType.display, color = TherapyColors.navy)

        OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), minLines = 2, maxLines = 6,
            placeholder = { Text(stringResource(R.string.topics_prompt), color = TherapyColors.neutral) })
        PrimaryButton(stringResource(R.string.add), enabled = draft.isNotBlank(), loading = working) {
            working = true
            scope.launch {
                try {
                    container.api.addSessionTopic(draft.trim(), draftId)
                    draft = ""; draftId = UUID.randomUUID().toString(); error = null; reload++
                } catch (e: Exception) { error = e } finally { working = false }
            }
        }
        Muted(stringResource(R.string.topics_footer))
        error?.let { ErrorText(errorText(it)) }

        Overline(stringResource(R.string.topics_open), modifier = Modifier.padding(top = 8.dp))
        if (open.isEmpty()) Muted(stringResource(R.string.topics_none))
        open.forEach { t ->
            Card {
                TopicText(t)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ editing = t }) { Text(stringResource(R.string.edit), color = TherapyColors.navy) }
                    TextButton({ act { container.api.deleteSessionTopic(t.id) } }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                    TextButton({ act { container.api.setSessionTopicDiscussed(t.id, true) } }) { Text(stringResource(R.string.topics_discussed), color = TherapyColors.success) }
                }
            }
        }

        if (discussed.isNotEmpty()) {
            Overline(stringResource(R.string.topics_discussed), modifier = Modifier.padding(top = 8.dp))
            discussed.forEach { t ->
                Card(tint = TherapyColors.canvas) {
                    TopicText(t, faded = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ act { container.api.setSessionTopicDiscussed(t.id, false) } }) { Text(stringResource(R.string.topics_bring_back), color = TherapyColors.navy) }
                    }
                }
            }
        }
    }

    editing?.let { t ->
        var text by remember(t.id) { mutableStateOf(t.text) }
        AlertDialog(onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.topics_edit)) },
            text = { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 8) },
            confirmButton = { TextButton({ editing = null; act { container.api.editSessionTopic(t.id, text.trim()) } }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ editing = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun TopicText(t: ApiSessionTopic, faded: Boolean = false) {
    Column {
        Text(t.text, style = TherapyType.bodyLarge, color = if (faded) TherapyColors.muted else TherapyColors.ink)
        ApiTimestamp.parse(t.createdAt)?.let { Text(it.shortDate(), style = TherapyType.caption, color = TherapyColors.muted) }
    }
}
