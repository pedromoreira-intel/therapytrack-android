package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.therapytrack.android.BuildConfig
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiThread
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.ApiUser
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ListRow
import com.therapytrack.android.ui.common.ListCard
import com.therapytrack.android.ui.common.Avatar
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

@Composable
fun ThreadsScreen(onThread: (Int, String) -> Unit) {
    val container = LocalContainer.current
    var threads by remember { mutableStateOf<List<ApiThread>>(emptyList()) }
    // This screen listed the conversations that already existed and offered no
    // way to begin one, so a client who had never written was reachable only
    // from their own record.
    var choosing by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { loadFailed = runCatching { threads = container.api.threads().threads }.isFailure }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.threads_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.weight(1f))
                TextButton({ choosing = true }) { Text(stringResource(R.string.new_message), color = TherapyColors.navy) }
            }
        }
        if (loadFailed) item { Row { Muted(stringResource(R.string.could_not_load)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } } }
        else if (threads.isEmpty()) item { Muted(stringResource(R.string.no_threads)) }
        items(threads, key = { it.otherUserId }) { t ->
            Card(modifier = Modifier.clickable { onThread(t.otherUserId, t.otherUserName ?: "") }) {
                Row(Modifier.fillMaxWidth()) {
                    Text(t.otherUserName ?: "#${t.otherUserId}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        fontWeight = if (t.unreadCount > 0) FontWeight.Bold else null)
                    if (t.unreadCount > 0) Pill(stringResource(R.string.unread_count, t.unreadCount), TherapyColors.navy)
                }
                Text(t.lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Muted(ApiTimestamp.parse(t.createdAt)?.shortDateTime() ?: t.createdAt)
            }
        }
    }

    if (choosing) NewConversationSheet(
        onDismiss = { choosing = false },
        onPick = { userId, name -> choosing = false; onThread(userId, name) })
}

/**
 * Pick a client to write to.
 *
 * Addressed by the client's *user* id, not the patients-table id: the caseload
 * carries it, and when an older server leaves it out the client's own record
 * still has it, so no client is silently missing from this list.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewConversationSheet(onDismiss: () -> Unit, onPick: (Int, String) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var patients by remember { mutableStateOf<List<ApiPatient>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var resolving by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        runCatching { patients = container.api.patients() }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = TherapyColors.pearl) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(stringResource(R.string.new_message), style = MaterialTheme.typography.headlineSmall,
                color = TherapyColors.navy, modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 12.dp))
            when {
                loading -> Muted(stringResource(R.string.loading), Modifier.padding(horizontal = 20.dp))
                patients.isEmpty() -> Muted(stringResource(R.string.no_clients), Modifier.padding(horizontal = 20.dp))
                else -> ListCard(Modifier.padding(horizontal = 16.dp)) {
                    patients.forEachIndexed { i, p ->
                        ListRow(p.name, leading = { Avatar(p.name, 36.dp) }, chevron = true,
                            divider = i < patients.lastIndex,
                            trailingContent = { if (resolving == p.id) Muted("…") },
                            onClick = {
                                if (resolving != null) return@ListRow
                                resolving = p.id
                                scope.launch {
                                    val userId = p.userId
                                        ?: runCatching { container.api.patient(p.id).userId }.getOrNull()
                                    resolving = null
                                    userId?.let { onPick(it, p.name) }
                                }
                            })
                    }
                }
            }
        }
    }
}

