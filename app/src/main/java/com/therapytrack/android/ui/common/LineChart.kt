package com.therapytrack.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.therapytrack.android.ui.theme.TherapyColors

/** One line on the chart: points as (x in 0..1, y in value units). */
data class Series(val label: String, val color: Color, val points: List<Pair<Float, Float>>)

/**
 * A small multi-series line chart drawn on a Canvas — three series and a
 * severity band are not worth a charting dependency. `bands` shade value
 * ranges (e.g. PHQ-9 moderate 10–14) behind the lines.
 */
@Composable
fun LineChart(series: List<Series>, minY: Float, maxY: Float, modifier: Modifier = Modifier,
              bands: List<Triple<Float, Float, Color>> = emptyList()) {
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width; val h = size.height
        fun y(v: Float) = h - ((v - minY) / (maxY - minY)).coerceIn(0f, 1f) * h
        bands.forEach { (lo, hi, color) -> drawRect(color, topLeft = Offset(0f, y(hi)), size = androidx.compose.ui.geometry.Size(w, y(lo) - y(hi))) }
        drawLine(TherapyColors.hairline, Offset(0f, h), Offset(w, h), 1.dp.toPx())
        series.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val path = Path()
            s.points.forEachIndexed { i, (x, v) -> val p = Offset(x * w, y(v)); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
            drawPath(path, s.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            s.points.forEach { (x, v) -> drawCircle(s.color, 3.dp.toPx(), Offset(x * w, y(v))) }
        }
    }
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        series.forEach { s ->
            androidx.compose.foundation.layout.Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
            Spacer(Modifier.size(4.dp)); Text(s.label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall); Spacer(Modifier.size(12.dp))
        }
    }
}
