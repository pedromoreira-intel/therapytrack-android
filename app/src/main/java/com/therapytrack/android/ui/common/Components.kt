package com.therapytrack.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyRadius
import com.therapytrack.android.ui.theme.TherapyType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Cards -----------------------------------------------------------------------

/**
 * A resting card as on iOS: white, 16dp radius, a soft ink shadow and no
 * hairline. `tint` swaps the surface for the tinted variants (alert, note).
 */
@Composable
fun Card(modifier: Modifier = Modifier, tint: Color = Color.White, radius: androidx.compose.ui.unit.Dp = TherapyRadius.large,
         padding: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = TherapyColors.shadowInk.copy(alpha = 0.10f), spotColor = TherapyColors.shadowInk.copy(alpha = 0.10f))
            .clip(shape).background(tint).padding(padding),
        content = content
    )
}

/** A card whose rows are separated by hairlines, like an iOS grouped list. */
@Composable
fun ListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(modifier, padding = 0.dp, content = content)

@Composable
fun ListRow(title: String, subtitle: String? = null, trailing: String? = null, icon: ImageVector? = null, tint: Color = TherapyColors.navy,
            chevron: Boolean = false, titleColor: Color = TherapyColors.ink, divider: Boolean = true, onClick: (() -> Unit)? = null,
            leading: (@Composable () -> Unit)? = null, trailingContent: (@Composable () -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.let { it(); Spacer(Modifier.width(12.dp)) }
            icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = TherapyType.bodyLarge, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            trailing?.let { Text(it, style = TherapyType.body, color = TherapyColors.muted, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.padding(start = 12.dp)) }
            trailingContent?.invoke()
            if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TherapyColors.neutral, modifier = Modifier.padding(start = 4.dp))
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(TherapyColors.hairline))
    }
}

// Text ---------------------------------------------------------------------------

/** Tracked uppercase label above a section or inside a card — champagne on canvas, grey on white. */
@Composable
fun Overline(text: String, color: Color = TherapyColors.muted, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = TherapyType.overline, color = color, modifier = modifier)
}

@Composable
fun SerifTitle(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = TherapyType.display) {
    Text(text, style = style, color = TherapyColors.navy, modifier = modifier)
}

/** Serif section heading with an optional pill link on the right ("Agenda ↗"). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, link: String? = null, onLink: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = TherapyType.serifTitle, color = TherapyColors.ink, modifier = Modifier.weight(1f))
        if (link != null && onLink != null) Text("$link ↗", style = TherapyType.captionBold, color = TherapyColors.navy,
            modifier = Modifier.clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.08f)).clickable(onClick = onLink).padding(10.dp, 6.dp))
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = TherapyType.body, color = TherapyColors.muted, modifier = modifier)
}

@Composable
fun ErrorText(text: String?) {
    if (text != null) Text(text, color = TherapyColors.critical, style = TherapyType.body, modifier = Modifier.padding(top = 8.dp))
}

/** A labelled value inside a brief card: small grey label, then the value. */
@Composable
fun LabelledValue(label: String, value: String, emphasis: Boolean = false) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = TherapyType.caption, color = TherapyColors.muted)
        Text(value, style = if (emphasis) TherapyType.emphasisLarge else TherapyType.bodyLarge, color = TherapyColors.ink)
    }
}

// Pills, avatars, icon circles ---------------------------------------------------------

/** Tinted uppercase pill: the risk and status chips. */
@Composable
fun Pill(text: String, color: Color, filled: Boolean = true) {
    Text(text.uppercase(), style = TherapyType.captionBold, color = color,
        modifier = Modifier.clip(CircleShape).background(if (filled) color.copy(alpha = 0.14f) else Color.Transparent).padding(10.dp, 4.dp))
}

/** Initials in a coloured circle. The colour follows the name, so one person keeps theirs. */
@Composable
fun Avatar(name: String, size: androidx.compose.ui.unit.Dp = 40.dp, color: Color? = null) {
    val palette = listOf(Color(0xFF3B82F6), Color(0xFFF43F7B), Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF59E0B), TherapyColors.navy)
    val c = color ?: palette[(name.sumOf { it.code } and 0x7fffffff) % palette.size]
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
        Text(initials, style = if (size >= 56.dp) TherapyType.h3 else TherapyType.emphasis, color = Color.White)
    }
}

/** An icon on a soft tinted disc: the leading element of the iOS quick-action cards. */
@Composable
fun IconCircle(icon: ImageVector, tint: Color = TherapyColors.navy, size: androidx.compose.ui.unit.Dp = 44.dp, square: Boolean = false) {
    Box(Modifier.size(size).clip(if (square) RoundedCornerShape(12.dp) else CircleShape).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size / 2))
    }
}

/** A stat tile: rounded-bold number, overline label. Three side by side on Today. */
@Composable
fun RowScope.MetricTile(value: String, label: String, valueColor: Color = TherapyColors.navy, tint: Color = Color.White) {
    Card(Modifier.weight(1f), tint = tint, padding = 14.dp) {
        Text(value, style = TherapyType.metricLarge, color = valueColor)
        Spacer(Modifier.height(4.dp))
        Overline(label)
    }
}

// Controls ---------------------------------------------------------------------------

/** iOS-style segmented control on a canvas track. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TherapyColors.canvas).border(1.dp, TherapyColors.hairline, RoundedCornerShape(12.dp)).padding(4.dp)) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (on) TherapyColors.navy else Color.Transparent).clickable { onSelect(key) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(label, style = TherapyType.emphasis, color = if (on) Color.White else TherapyColors.muted, maxLines = 1)
            }
        }
    }
}

enum class ButtonStyle { Navy, Champagne, Canvas }

/** The CTA. Navy by default; champagne for the consent "Continue"; canvas (navy text) for "Entrar" on the sign-in sheet. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false,
                  style: ButtonStyle = ButtonStyle.Navy, icon: ImageVector? = null, onClick: () -> Unit) {
    val (bg, fg) = when (style) {
        ButtonStyle.Navy -> TherapyColors.navy to Color.White
        ButtonStyle.Champagne -> TherapyColors.champagne to TherapyColors.navy
        ButtonStyle.Canvas -> TherapyColors.canvas to TherapyColors.navy
    }
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(TherapyRadius.large),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg, disabledContainerColor = TherapyColors.neutral, disabledContentColor = Color.White),
        border = if (style == ButtonStyle.Canvas) androidx.compose.foundation.BorderStroke(1.dp, TherapyColors.hairline) else null,
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
        else {
            icon?.let { Icon(it, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = TherapyType.emphasisLarge)
        }
    }
}

// Dates -------------------------------------------------------------------------------

private val dayTime = DateTimeFormatter.ofPattern("d MMM, HH:mm")
private val dayOnly = DateTimeFormatter.ofPattern("d MMM yyyy")
private val longDay = DateTimeFormatter.ofPattern("EEEE, d MMMM")

fun Instant.shortDateTime(): String = dayTime.format(atZone(ZoneId.systemDefault()))
fun Instant.shortDate(): String = dayOnly.format(atZone(ZoneId.systemDefault()))
fun Instant.longDay(): String = longDay.format(atZone(ZoneId.systemDefault()))

/** The bell: opens the activity feed, with the unread total on it. */
@Composable
fun BellButton(onClick: () -> Unit) {
    val container = com.therapytrack.android.LocalContainer.current
    val inbox by container.inbox.collectAsState()
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(androidx.compose.material.icons.Icons.Outlined.Notifications, null, tint = TherapyColors.navy, modifier = Modifier.size(22.dp))
        if (inbox.unreadTotal > 0) Text(if (inbox.unreadTotal > 99) "99+" else "${inbox.unreadTotal}", style = TherapyType.label, color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).background(TherapyColors.critical, CircleShape).padding(horizontal = 5.dp, vertical = 1.dp))
    }
}
