package com.therapytrack.android.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiMessage
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.PendingMessage
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.Instant

private sealed class Bubble(val mine: Boolean, val text: String, val whenMillis: Long) {
    class Delivered(m: ApiMessage, mine: Boolean) : Bubble(mine, m.message, ApiTimestamp.parse(m.createdAt)?.toEpochMilli() ?: 0)
    class Pending(p: PendingMessage) : Bubble(true, p.text, p.createdAtMillis)
}

/** The client's one conversation: with their therapist. */
@Composable
fun MessagesScreen(state: ClientState) {
    ConversationScreen(otherUserId = state.patient?.therapistId, title = stringResource(R.string.messages_title), showNotRealtime = true)
}

/** One thread, used by both roles. `otherUserId` null means the other party is not known yet. */
@Composable
fun ConversationScreen(otherUserId: Int?, title: String, showNotRealtime: Boolean = false, onBack: (() -> Unit)? = null) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val me = container.client.currentUserId
    val therapist = otherUserId
    var delivered by remember { mutableStateOf<List<ApiMessage>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingMessage>>(emptyList()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val outboxTick by container.messages.changes.collectAsState()

    LaunchedEffect(therapist, reload, outboxTick) {
        val t = therapist ?: return@LaunchedEffect
        pending = container.messages.pending(t)
        runCatching { delivered = container.api.conversation(t).messages; container.api.markConversationRead(t) }
    }

    val bubbles = (delivered.map { Bubble.Delivered(it, it.fromUserId == me) } + pending.map { Bubble.Pending(it) }).sortedBy { it.whenMillis }
    LaunchedEffect(bubbles.size) { if (bubbles.isNotEmpty()) listState.animateScrollToItem(bubbles.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        if (onBack != null) TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(20.dp, if (onBack == null) 16.dp else 0.dp, 20.dp, 4.dp))
        if (showNotRealtime) Muted(stringResource(R.string.messages_not_realtime), Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (bubbles.isEmpty()) item { Muted(stringResource(R.string.no_messages), Modifier.padding(top = 24.dp)) }
            items(bubbles) { b ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (b.mine) Alignment.End else Alignment.Start) {
                    Box(
                        Modifier.widthIn(max = 300.dp).clip(MaterialTheme.shapes.medium)
                            .background(if (b.mine) TherapyColors.navy else TherapyColors.pearl).padding(12.dp)
                    ) { Text(b.text, color = if (b.mine) Color.White else TherapyColors.ink) }
                    Muted(if (b is Bubble.Pending) stringResource(R.string.message_pending) else Instant.ofEpochMilli(b.whenMillis).shortDateTime())
                }
            }
        }
        if (failed) ErrorText(stringResource(R.string.message_failed))
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it }, placeholder = { Text(stringResource(R.string.message_hint)) }, modifier = Modifier.weight(1f), maxLines = 4)
            TextButton(enabled = draft.isNotBlank() && therapist != null, onClick = {
                val t = therapist ?: return@TextButton
                val text = draft
                scope.launch {
                    val outcome = container.messages.send(t, text)
                    // The draft is cleared only once the outbox has it.
                    if (outcome == SaveOutcome.NOT_STORED) failed = true else { failed = false; draft = ""; reload++ }
                }
            }) { Text(stringResource(R.string.send)) }
        }
    }
}
