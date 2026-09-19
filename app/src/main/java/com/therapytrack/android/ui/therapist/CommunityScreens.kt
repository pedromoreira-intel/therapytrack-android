package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.therapytrack.android.core.ApiPost
import com.therapytrack.android.core.ApiPostDetail
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.shortDateTime
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

val postTypes = listOf("question" to R.string.pt_question, "case" to R.string.pt_case, "discussion" to R.string.pt_discussion, "resource" to R.string.pt_resource)

@Composable
fun CommunityScreen(onBack: () -> Unit, onPost: (Int) -> Unit) {
    val container = LocalContainer.current
    var sort by rememberSaveable { mutableStateOf("recent") }
    var posts by remember { mutableStateOf<List<ApiPost>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var composing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sort, reload) { failed = runCatching { posts = container.api.posts(sort) }.isFailure }
    if (composing) { NewPostForm { composing = false; reload++ }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.community_title), style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("recent" to R.string.sort_recent, "top" to R.string.sort_top, "discussed" to R.string.sort_discussed).forEach { (v, l) ->
                FilterChip(sort == v, { sort = v }, label = { Text(stringResource(l)) })
            }
        }
        PrimaryButton(stringResource(R.string.new_post)) { composing = true }
        if (failed) Row { Muted(stringResource(R.string.could_not_load)); TextButton({ reload++ }) { Text(stringResource(R.string.refresh)) } }
        else if (posts.isEmpty()) Muted(stringResource(R.string.no_posts))
        posts.forEach { p ->
            Card(modifier = Modifier.clickable { onPost(p.id) }) {
                Row(Modifier.fillMaxWidth()) {
                    Text(p.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill(stringResource(postTypes.firstOrNull { it.first == p.postType }?.second ?: R.string.pt_discussion), TherapyColors.navy)
                }
                Text(p.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                Muted(listOf(if (p.isAnonymous) stringResource(R.string.anonymous) else p.authorName ?: "", "▲ ${p.upvotes - p.downvotes}",
                    stringResource(R.string.replies_count, p.replyCount), ApiTimestamp.parse(p.createdAt)?.shortDateTime() ?: "").filter { it.isNotBlank() }.joinToString(" · "))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NewPostForm(onDone: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("question") }
    var anonymous by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    FormFrame(stringResource(R.string.new_post), onCancel = onDone, canSave = title.isNotBlank() && content.isNotBlank(), busy = busy, error = error, saveLabel = stringResource(R.string.publish), onSave = {
        busy = true; error = null
        scope.launch { try { container.api.createPost(title, content, type, anonymous); onDone() } catch (e: Exception) { error = e } finally { busy = false } }
    }) {
        Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Muted(stringResource(R.string.community_confidentiality)) }
        Field(title, { title = it }, R.string.post_title)
        Field(content, { content = it }, R.string.post_content, single = false)
        Muted(stringResource(R.string.post_type)); ChipRow(postTypes, type) { type = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.post_anonymous), Modifier.weight(1f)); Switch(anonymous, { anonymous = it }) }
    }
}

@Composable
fun PostScreen(postId: Int, onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val me = container.client.currentUserId
    var detail by remember { mutableStateOf<ApiPostDetail?>(null) }
    var reload by remember { mutableStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(postId, reload) { runCatching { detail = container.api.post(postId) } }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onBack) { Text(stringResource(R.string.back)) }
        val d = detail ?: run { Muted(stringResource(R.string.loading)); return@Column }
        val p = d.post
        Text(p.title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        Muted(listOf(if (p.isAnonymous) stringResource(R.string.anonymous) else p.authorName ?: "", ApiTimestamp.parse(p.createdAt)?.shortDateTime() ?: "").filter { it.isNotBlank() }.joinToString(" · "))
        Card { Text(p.content) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton({ scope.launch { runCatching { container.api.votePost(p.id, 1) }; reload++ } }) { Text(if (p.userVote == 1) "▲ ✓" else "▲") }
            Text("${p.upvotes - p.downvotes}", style = MaterialTheme.typography.titleMedium)
            TextButton({ scope.launch { runCatching { container.api.votePost(p.id, -1) }; reload++ } }) { Text(if (p.userVote == -1) "▼ ✓" else "▼") }
        }
        Text(stringResource(R.string.replies_count, d.replies.size), style = MaterialTheme.typography.titleMedium)
        d.replies.forEach { r ->
            Card(tint = if (r.isAcceptedAnswer) TherapyColors.success.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface) {
                if (r.isAcceptedAnswer) Pill(stringResource(R.string.accepted_answer), TherapyColors.success)
                Muted("${r.authorName ?: ""} · ${ApiTimestamp.parse(r.createdAt)?.shortDateTime() ?: ""}")
                Text(r.content)
                if (p.userId == me && !r.isAcceptedAnswer) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ scope.launch { runCatching { container.api.acceptAnswer(p.id, r.id) }; reload++ } }) { Text(stringResource(R.string.accept_answer)) }
                }
            }
        }
        OutlinedTextField(draft, { draft = it }, label = { Text(stringResource(R.string.reply)) }, modifier = Modifier.fillMaxWidth())
        PrimaryButton(stringResource(R.string.reply), enabled = draft.isNotBlank()) {
            val text = draft
            scope.launch { if (runCatching { container.api.replyToPost(p.id, text) }.isSuccess) { draft = ""; reload++ } }
        }
        Spacer(Modifier.height(24.dp))
    }
}
