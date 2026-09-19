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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.therapytrack.android.BuildConfig
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiThread
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.ApiUser
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

@Composable
fun ThreadsScreen(onThread: (Int, String) -> Unit) {
    val container = LocalContainer.current
    var threads by remember { mutableStateOf<List<ApiThread>>(emptyList()) }
    var loadFailed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { loadFailed = runCatching { threads = container.api.threads().threads }.isFailure }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.threads_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
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
}

