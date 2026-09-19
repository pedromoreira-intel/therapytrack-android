package com.therapytrack.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember

data class TabSpec(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

/** iOS-style tab bar: pearl surface, hairline on top, navy selected, grey otherwise, no indicator pill. */
@Composable
fun TherapyTabBar(tabs: List<TabSpec>, current: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(TherapyColors.pearl)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(TherapyColors.hairline))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 6.dp)) {
            tabs.forEach { tab ->
                val on = tab.route == current
                val color = if (on) TherapyColors.navy else TherapyColors.neutral
                Column(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSelect(tab.route) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(if (on) tab.selectedIcon else tab.icon, null, tint = color, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(tab.label, style = TherapyType.label, color = color, maxLines = 1)
                }
            }
        }
    }
}
