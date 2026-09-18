package com.therapytrack.android.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiJournalEntry
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.offline.PendingJournalEntry
import com.therapytrack.android.offline.SaveOutcome
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun JournalScreen() {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<ApiJournalEntry>>(emptyList()) }
    var pending by remember { mutableStateOf<List<PendingJournalEntry>>(emptyList()) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ApiJournalEntry?>(null) }
    var reload by remember { mutableStateOf(0) }
    val outboxTick by container.journal.changes.collectAsState()

    LaunchedEffect(reload, outboxTick) {
        pending = container.journal.mine()
        runCatching { entries = container.api.journalEntries() }
    }

    if (composing) {
        NewEntry(onSaved = { composing = false; reload++ }, onCancel = { composing = false })
        return
    }

    Scaffold(containerColor = TherapyColors.canvas, floatingActionButton = {
        FloatingActionButton({ composing = true }, containerColor = TherapyColors.navy) { Icon(Icons.Filled.Add, stringResource(R.string.new_entry), tint = androidx.compose.ui.graphics.Color.White) }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(top = 16.dp)) }
            if (entries.isEmpty() && pending.isEmpty()) item { Muted(stringResource(R.string.no_entries)) }
            // What was written but has not been delivered, shown so the person sees what they wrote, not only what arrived.
            items(pending, key = { "p" + it.id }) { p ->
                Card(tint = TherapyColors.champagne.copy(alpha = 0.25f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Muted(Instant.ofEpochMilli(p.createdAtMillis).shortDateTime()); Spacer(Modifier.weight(1f))
                        Pill(stringResource(R.string.waiting_to_send), TherapyColors.warning)
                    }
                    p.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Text(p.content, style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(entries, key = { it.id }) { e ->
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Muted(ApiTimestamp.parse(e.createdAt)?.shortDateTime() ?: e.createdAt); Spacer(Modifier.weight(1f))
                        Pill(stringResource(if (e.isPrivate) R.string.private_label else R.string.shared_label), if (e.isPrivate) TherapyColors.muted else TherapyColors.success)
                    }
                    e.title?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Text(e.content, style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ deleting = e }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    deleting?.let { e ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_entry)) }, text = { Text(stringResource(R.string.delete_entry_confirm)) },
            confirmButton = { TextButton({ scope.launch { runCatching { container.api.deleteJournalEntry(e.id) }; reload++ }; deleting = null }) { Text(stringResource(R.string.delete), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun NewEntry(onSaved: () -> Unit, onCancel: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var isPrivate by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.new_entry), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        OutlinedTextField(title, { title = it }, placeholder = { Text(stringResource(R.string.entry_title_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(content, { content = it }, placeholder = { Text(stringResource(R.string.entry_content_hint)) }, modifier = Modifier.fillMaxWidth().weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.private_entry), style = MaterialTheme.typography.titleMedium)
                Muted(stringResource(if (isPrivate) R.string.private_entry_detail else R.string.shared_entry_detail))
            }
            Switch(isPrivate, { isPrivate = it })
        }
        // The draft stays on screen: nothing is cleared until the outbox has it.
        if (failed) ErrorText(stringResource(R.string.entry_not_saved))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onCancel, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            PrimaryButton(stringResource(R.string.save), Modifier.weight(2f), enabled = content.isNotBlank(), loading = busy) {
                busy = true; failed = false
                scope.launch {
                    val outcome = container.journal.save(title.takeIf { it.isNotBlank() }, content, isPrivate)
                    busy = false
                    if (outcome == SaveOutcome.NOT_STORED) failed = true else onSaved()
                }
            }
        }
    }
}
