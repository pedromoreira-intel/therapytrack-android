package com.therapytrack.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyRadius
import com.therapytrack.android.ui.theme.TherapyType

/** The navy header card at the top of both profile tabs: avatar, name, role, email; a champagne role pill. */
@Composable
fun ProfileHeader(name: String, roleLine: String, email: String, rolePill: String) {
    val shape = RoundedCornerShape(TherapyRadius.xlarge)
    Row(
        Modifier.fillMaxWidth().shadow(8.dp, shape, ambientColor = TherapyColors.shadowInk.copy(alpha = 0.12f), spotColor = TherapyColors.shadowInk.copy(alpha = 0.12f))
            .clip(shape).background(TherapyColors.navyGradient).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name, 64.dp, color = Color.White.copy(alpha = 0.22f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = TherapyType.h2, color = Color.White)
            Text(roleLine, style = TherapyType.body, color = Color.White.copy(alpha = 0.85f))
            Text(email, style = TherapyType.caption, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(rolePill.uppercase(), style = TherapyType.captionBold, color = TherapyColors.champagne,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.12f)).padding(8.dp, 4.dp))
    }
}
