package com.therapytrack.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiNotification
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch

/** What a notification opens. The shells map this to a route. */
sealed class Open {
    data class Thread(val userId: Int, val name: String) : Open()
    /** [direction] is the tab it sits on: a referral you received, or your own that someone answered. */
    data class Referral(val id: Int?, val direction: String) : Open()
    data class Group(val id: Int) : Open()
    data class Patient(val id: Int) : Open()
    /** A supervision session: the supervisor's side for a request, the finder's side for the rest. */
    data class Supervision(val asSupervisor: Boolean) : Open()
}

fun ApiNotification.target(): Open? = when (kind) {
    "message" -> refId?.let { Open.Thread(it, title) }
    "referral" -> Open.Referral(refId, "received")
    "referral_response" -> Open.Referral(refId, "sent")
    "join_request", "join_response" -> refId?.let { Open.Group(it) }
    "alert", "client_record" -> refId?.let { Open.Patient(it) }
    "supervision_request" -> Open.Supervision(asSupervisor = true)
    "supervision_response", "supervision_cancelled", "supervision_changed" -> Open.Supervision(asSupervisor = false)
    else -> null
}

/** The feed behind the bell: newest first, unread marked, one tap opens the thing and marks it read. */
@Composable
fun ActivityScreen(onBack: () -> Unit, onOpen: (Open) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ApiNotification>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { failed = runCatching { items = container.api.notifications() }.isFailure; container.refreshInbox() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("‹ " + stringResource(R.string.back), style = TherapyType.bodyLarge, color = TherapyColors.navy) }
                Spacer(Modifier.weight(1f))
                if (items.any { it.readAt == null }) TextButton({ scope.launch { runCatching { container.api.markNotificationsRead() }; reload++ } }) { Text(stringResource(R.string.mark_all_read), color = TherapyColors.navy) }
            }
            Text(stringResource(R.string.activity_title), style = TherapyType.display, color = TherapyColors.navy, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (failed) item { Row { Muted(stringResource(R.string.could_not_load)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } } }
        else if (items.isEmpty()) item { Muted(stringResource(R.string.no_activity)) }
        items(items, key = { it.id }) { n ->
            val unread = n.readAt == null
            Card(modifier = Modifier.clickable {
                scope.launch { runCatching { container.api.markNotificationRead(n.id) }; container.refreshInbox() }
                n.target()?.let(onOpen)
            }, tint = if (unread) androidx.compose.ui.graphics.Color.White else TherapyColors.canvas) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(when (n.kind) { "message" -> Icons.Outlined.ChatBubbleOutline; "alert" -> Icons.Outlined.Warning; "join_request", "join_response" -> Icons.Outlined.Groups; "client_record" -> Icons.Outlined.EditNote
                        "supervision_request", "supervision_response" -> Icons.Outlined.PersonSearch
                        "supervision_cancelled" -> Icons.Outlined.EventBusy
                        "supervision_changed" -> Icons.Outlined.EditCalendar; else -> Icons.Outlined.SwapHoriz },
                        tint = when (n.kind) { "alert" -> TherapyColors.critical; "supervision_cancelled", "supervision_changed" -> TherapyColors.warning; else -> TherapyColors.navy })
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(kindLine(n), style = TherapyType.caption, color = TherapyColors.muted)
                        Text(n.title, style = if (unread) TherapyType.emphasisLarge else TherapyType.bodyLarge, color = TherapyColors.ink)
                        n.body?.let { Muted(bodyLine(n.kind, it)) }
                        Muted(ApiTimestamp.parse(n.createdAt)?.shortDateTime() ?: n.createdAt)
                    }
                    if (unread) Icon(Icons.Filled.Circle, null, tint = TherapyColors.navy, modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun kindLine(n: ApiNotification): String = stringResource(when (n.kind) {
    "message" -> R.string.n_message; "referral" -> R.string.n_referral; "referral_response" -> R.string.n_referral_response
    "join_request" -> R.string.n_join_request; "join_response" -> R.string.n_join_response; "alert" -> R.string.n_alert
    "client_record" -> R.string.n_client_record
    "supervision_request" -> R.string.n_supervision_request; "supervision_response" -> R.string.n_supervision_response
    "supervision_cancelled" -> R.string.n_supervision_cancelled; "supervision_changed" -> R.string.n_supervision_changed
    else -> R.string.activity_title
})

@Composable
private fun bodyLine(kind: String, body: String): String = when (kind) {
    "referral_response", "join_response", "supervision_response" -> stringResource(when (body) { "accepted", "approved" -> R.string.st_accepted; "declined", "rejected" -> R.string.st_declined; else -> R.string.st_pending })
    // The kind of record the client made, never what was in it.
    "client_record" -> when (body) {
        "check_in" -> stringResource(R.string.rec_check_in)
        "assessment" -> stringResource(R.string.rec_assessment)
        "journal" -> stringResource(R.string.rec_journal)
        else -> body
    }
    else -> body
}
