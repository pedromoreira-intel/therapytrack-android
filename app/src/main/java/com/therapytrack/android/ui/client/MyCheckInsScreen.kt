package com.therapytrack.android.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiEmaResponse
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.ui.common.BackRow
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The client's own check-ins, newest first, in one list. */
@Composable
fun MyCheckInsScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    var entries by remember { mutableStateOf<List<ApiEmaResponse>?>(null) }
    LaunchedEffect(Unit) {
        entries = runCatching { container.api.checkIns().sortedByDescending { ApiTimestamp.parse(it.doneAt) ?: Instant.EPOCH } }.getOrDefault(emptyList())
    }
    val format = DateTimeFormatter.ofPattern("EEE, d MMM, HH:mm", appLocale())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(onBack, stringResource(R.string.my_check_ins))
        val list = entries
        if (list != null && list.isEmpty()) Muted(stringResource(R.string.no_check_ins_yet_plain))
        if (!list.isNullOrEmpty()) CompactCard {
            list.forEachIndexed { i, e ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(ApiTimestamp.parse(e.doneAt)?.atZone(ZoneId.systemDefault())?.format(format) ?: "", style = TherapyType.caption, color = TherapyColors.muted)
                    Text(scoresLine(e), style = TherapyType.body, color = TherapyColors.ink)
                    e.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
                }
                if (i < list.lastIndex) RowDivider()
            }
        }
    }
}
