package com.therapytrack.android.ui.therapist

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.clinical.CrisisResources
import com.therapytrack.android.core.ApiResource
import com.therapytrack.android.ui.common.BackRow
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.ListSearchField
import com.therapytrack.android.ui.common.PageTitle
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.SectionLabel
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType

/**
 * Recursos, one level deep: rows that open each collection, then the two
 * things worth reaching for mid-session. Same as iOS ResourcesView.
 */
@Composable
fun ResourcesPanel(onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CompactCard {
            CompactLinkRow(Icons.Outlined.LibraryBooks, stringResource(R.string.res_library), stringResource(R.string.res_library_sub)) { onOpen("library") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.TravelExplore, stringResource(R.string.res_research), stringResource(R.string.res_research_sub)) { onOpen("research") }
            RowDivider(50.dp)
            CompactLinkRow(Icons.Outlined.CalendarMonth, stringResource(R.string.res_conferences), stringResource(R.string.res_conferences_sub)) { onOpen("conferences") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.res_quick_reference))
            CompactCard {
                CompactLinkRow(Icons.Outlined.Assignment, stringResource(R.string.res_phq9_scoring)) { onOpen("phq9-scoring") }
                RowDivider(50.dp)
                // The same locale-aware list the client's crisis screen uses.
                CompactLinkRow(Icons.Outlined.Phone, stringResource(R.string.res_crisis_lines), tint = TherapyColors.critical) { onOpen("crisis-lines") }
            }
        }
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackRow(onBack)
        PageTitle(title)
        content()
    }
}

private fun openUrl(context: android.content.Context, url: String?) {
    val u = url?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
}

@Composable
private fun <T> Loading(state: Result<List<T>>?, empty: String, retry: () -> Unit, content: @Composable (List<T>) -> Unit) {
    when {
        state == null -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TherapyColors.navy) }
        state.isFailure -> Column {
            Text(stringResource(R.string.res_failed), style = TherapyType.body, color = TherapyColors.muted)
            TextButton(retry) { Text(stringResource(R.string.retry)) }
        }
        state.getOrThrow().isEmpty() -> Text(empty, style = TherapyType.body, color = TherapyColors.muted)
        else -> content(state.getOrThrow())
    }
}

/** Ordered by how a clinician reaches for them: what to follow, what protocol, what to administer. */
private val librarySections = listOf(
    Triple("guidelines", R.string.lib_guidelines, R.string.lib_guidelines_note),
    Triple("manuals", R.string.lib_manuals, R.string.lib_manuals_note),
    Triple("instruments", R.string.lib_instruments, R.string.lib_instruments_note)
)

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    var state by remember { mutableStateOf<Result<List<ApiResource>>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { state = null; state = runCatching { container.api.library() } }

    SubPage(stringResource(R.string.res_library), onBack) {
        ListSearchField(query, { query = it }, stringResource(R.string.lib_search))
        Loading(state, stringResource(R.string.lib_empty), { reload++ }) { all ->
            val q = query.trim()
            val shown = if (q.isEmpty()) all else all.filter { r -> listOf(r.title, r.author ?: "", r.description ?: "").any { it.contains(q, ignoreCase = true) } }
            if (shown.isEmpty()) Text(stringResource(R.string.lib_no_match, q), style = TherapyType.body, color = TherapyColors.muted)
            librarySections.forEach { (key, title, note) ->
                val items = shown.filter { it.category == key }
                if (items.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(title))
                    Text(stringResource(note), style = TherapyType.caption, color = TherapyColors.neutral)
                    CompactCard {
                        items.forEachIndexed { i, r ->
                            ReferenceRow(r) { openUrl(context, r.url) }
                            if (i < items.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

/** One reference: title, who it is by, what it is for. Only the measures this app administers are marked. */
@Composable
private fun ReferenceRow(r: ApiResource, onOpen: () -> Unit) {
    val link = !r.url.isNullOrBlank()
    Row(Modifier.fillMaxWidth().clickable(enabled = link, onClick = onOpen).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, style = TherapyType.emphasis, color = TherapyColors.ink, modifier = Modifier.weight(1f, fill = false))
                if (r.title in listOf("PHQ-9", "GAD-7", "WAI-SR")) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lib_in_use), style = TherapyType.caption, color = TherapyColors.navy,
                        modifier = Modifier.clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            r.author?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.navy) }
            r.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
        }
        if (link) Icon(Icons.AutoMirrored.Filled.CallMade, null, tint = TherapyColors.neutral, modifier = Modifier.padding(start = 8.dp).size(14.dp))
    }
}

@Composable
fun ResearchScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    var state by remember { mutableStateOf<Result<List<com.therapytrack.android.core.ApiResearchArticle>>?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { state = null; state = runCatching { container.api.research() } }
    SubPage(stringResource(R.string.res_research), onBack) {
        Text(stringResource(R.string.res_research_sub), style = TherapyType.caption, color = TherapyColors.muted)
        Loading(state, stringResource(R.string.res_nothing), { reload++ }) { items ->
            CompactCard {
                items.forEachIndexed { i, a ->
                    Column(Modifier.fillMaxWidth().clickable(enabled = !a.url.isNullOrBlank()) { openUrl(context, a.url) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(a.title ?: "—", style = TherapyType.emphasis, color = TherapyColors.ink)
                        Text(listOfNotNull(a.source, a.date).filter { it.isNotBlank() }.joinToString(" · "), style = TherapyType.caption, color = TherapyColors.muted)
                    }
                    if (i < items.lastIndex) RowDivider()
                }
            }
        }
    }
}

@Composable
fun ConferencesScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val context = LocalContext.current
    var state by remember { mutableStateOf<Result<List<com.therapytrack.android.core.ApiConference>>?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) { state = null; state = runCatching { container.api.conferences() } }
    SubPage(stringResource(R.string.res_conferences), onBack) {
        Loading(state, stringResource(R.string.res_nothing), { reload++ }) { items ->
            CompactCard {
                items.forEachIndexed { i, c ->
                    Column(Modifier.fillMaxWidth().clickable(enabled = !c.url.isNullOrBlank()) { openUrl(context, c.url) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(c.name, style = TherapyType.emphasis, color = TherapyColors.ink)
                        Text("${c.date} · ${c.location}", style = TherapyType.caption, color = TherapyColors.muted)
                        c.deadline?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.res_deadline, it), style = TherapyType.caption, color = TherapyColors.warning) }
                    }
                    if (i < items.lastIndex) RowDivider()
                }
            }
        }
    }
}

/** Cut-offs as published (Kroenke et al., 2001). */
@Composable
fun Phq9ScoringScreen(onBack: () -> Unit) {
    val rows = listOf(
        Triple("0–4", R.string.band_minimal, R.string.phq_act_none),
        Triple("5–9", R.string.band_mild, R.string.phq_act_mild),
        Triple("10–14", R.string.band_moderate, R.string.phq_act_moderate),
        Triple("15–19", R.string.band_moderately_severe, R.string.phq_act_mod_severe),
        Triple("20–27", R.string.band_severe, R.string.phq_act_severe)
    )
    SubPage(stringResource(R.string.res_phq9_scoring), onBack) {
        CompactCard {
            rows.forEachIndexed { i, (score, band, action) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(score, style = TherapyType.emphasis, color = TherapyColors.navy, modifier = Modifier.width(64.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(band), style = TherapyType.emphasis, color = TherapyColors.ink)
                        Text(stringResource(action), style = TherapyType.caption, color = TherapyColors.muted)
                    }
                }
                if (i < rows.lastIndex) RowDivider()
            }
        }
        Text(stringResource(R.string.phq_item9_note), style = TherapyType.caption, color = TherapyColors.muted)
    }
}

@Composable
fun CrisisLinesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SubPage(stringResource(R.string.res_crisis_lines), onBack) {
        CompactCard {
            val lines = CrisisResources.resources()
            lines.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${r.dialable}"))) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = TherapyType.emphasis, color = if (r.isEmergency) TherapyColors.critical else TherapyColors.ink)
                        Text(r.detail, style = TherapyType.caption, color = TherapyColors.muted)
                    }
                    Text(r.number, style = TherapyType.emphasis, color = TherapyColors.navy)
                }
                if (i < lines.lastIndex) RowDivider()
            }
        }
    }
}
