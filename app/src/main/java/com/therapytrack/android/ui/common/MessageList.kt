package com.therapytrack.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors

/** One line in a thread, from either party. */
data class Bubble(val key: String, val mine: Boolean, val text: String, val caption: String, val author: String? = null)

@Composable
fun MessageList(bubbles: List<Bubble>, state: LazyListState, modifier: Modifier = Modifier, empty: @Composable () -> Unit = {}) {
    LazyColumn(modifier.padding(horizontal = 16.dp), state = state, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        if (bubbles.isEmpty()) item { empty() }
        items(bubbles, key = { it.key }) { b ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (b.mine) Alignment.End else Alignment.Start) {
                b.author?.takeIf { !b.mine }?.let { Muted(it) }
                Box(
                    Modifier.widthIn(max = 300.dp).clip(MaterialTheme.shapes.medium)
                        .background(if (b.mine) TherapyColors.navy else TherapyColors.pearl).padding(12.dp)
                ) { Text(b.text, color = if (b.mine) Color.White else TherapyColors.ink) }
                Muted(b.caption)
            }
        }
    }
}

@Composable
fun Composer(draft: String, onDraft: (String) -> Unit, placeholder: String, sendLabel: String, enabled: Boolean, onSend: () -> Unit) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(draft, onDraft, placeholder = { Text(placeholder) }, modifier = Modifier.weight(1f), maxLines = 4)
        TextButton(enabled = enabled, onClick = onSend) { Text(sendLabel) }
    }
}
