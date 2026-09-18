package com.therapytrack.android.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.offline.StuckItem
import com.therapytrack.android.offline.StuckWork
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.common.shortDateTime
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Work that has stopped retrying, with the two things a person can do about
 * it. Shown at the top of Home. Nothing here is automatic: discard is behind a
 * confirmation whose wording says whether the thing exists anywhere else.
 */
@Composable
fun StuckWorkBanner(refreshKey: Any? = Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<StuckItem>>(emptyList()) }
    var confirmDiscard by remember { mutableStateOf<StuckItem?>(null) }

    suspend fun reload() { items = StuckWork.all(container.stuckSources) }
    LaunchedEffect(refreshKey) { reload() }

    if (items.isEmpty()) return

    Card(tint = TherapyColors.rose.copy(alpha = 0.25f), modifier = Modifier.padding(bottom = 12.dp)) {
        Text(stringResource(R.string.stuck_headline_client), style = MaterialTheme.typography.titleMedium, color = TherapyColors.navy)
        Muted(stringResource(R.string.stuck_body), Modifier.padding(top = 4.dp, bottom = 8.dp))
        items.forEach { item ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("${stringResource(kindLabel(item.kind))} · ${Instant.ofEpochMilli(item.writtenAtMillis).shortDateTime()}", style = MaterialTheme.typography.labelSmall)
                Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                item.lastError?.let { Muted(it) }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton({ confirmDiscard = item }) { Text(stringResource(R.string.discard), color = TherapyColors.critical) }
                    TextButton({ scope.launch { StuckWork.retry(item.id, container.stuckSources); reload() } }) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }

    confirmDiscard?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDiscard = null },
            title = { Text(stringResource(if (item.kind.isIrreplaceable) R.string.discard_irreplaceable_title else R.string.discard_replaceable_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = { TextButton({ scope.launch { StuckWork.discard(item.id, container.stuckSources); reload() }; confirmDiscard = null }) { Text(stringResource(R.string.discard), color = TherapyColors.critical) } },
            dismissButton = { TextButton({ confirmDiscard = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

fun kindLabel(kind: StuckItem.Kind): Int = when (kind) {
    StuckItem.Kind.CHECK_IN -> R.string.kind_check_in
    StuckItem.Kind.ASSESSMENT -> R.string.kind_assessment
    StuckItem.Kind.JOURNAL_ENTRY -> R.string.kind_journal
    StuckItem.Kind.MESSAGE -> R.string.kind_message
    StuckItem.Kind.SESSION_NOTE -> R.string.kind_session_note
}
