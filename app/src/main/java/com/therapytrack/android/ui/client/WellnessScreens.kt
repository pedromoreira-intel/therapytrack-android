package com.therapytrack.android.ui.client

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therapytrack.android.R
import com.therapytrack.android.ui.common.CapsuleButton
import com.therapytrack.android.ui.common.CompactCard
import com.therapytrack.android.ui.common.CompactLinkRow
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PageTitle
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.RowDivider
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.theme.SerifDisplay
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.delay

// Bem-estar: the exercises, each saying what it asks of you. Same pages as iOS
// WellnessView, BreathingExercisesView and GroundingWalkthroughs.

@Composable
fun WellnessScreen(onBreathing: () -> Unit, onMeditation: () -> Unit, onGrounding: () -> Unit, onCrisis: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp, 0.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // No support button in the title here: "Precisa de apoio agora?" is
        // at the foot of the page, and the two said the same thing.
        PageTitle(stringResource(R.string.tab_wellbeing))
        Muted(stringResource(R.string.wellness_intro))
        CompactCard {
            CompactLinkRow(Icons.Outlined.Air, stringResource(R.string.breathing), stringResource(R.string.breathing_row), onClick = onBreathing)
            RowDivider(inset = 48.dp)
            CompactLinkRow(Icons.Outlined.Timer, stringResource(R.string.meditation), stringResource(R.string.meditation_row), onClick = onMeditation)
            RowDivider(inset = 48.dp)
            CompactLinkRow(Icons.Outlined.DirectionsWalk, stringResource(R.string.grounding), stringResource(R.string.grounding_row), onClick = onGrounding)
        }
        Spacer(Modifier.height(12.dp))
        CompactCard {
            CompactLinkRow(Icons.Outlined.Support, stringResource(R.string.need_support_now), stringResource(R.string.crisis_lines_and_what_to_do), onClick = onCrisis)
        }
    }
}

/** True when the person has turned animations off; the exercises then hold still. */
@Composable
private fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Keeps the screen awake while an exercise is open. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
}

/** The top of an exercise: a small ×, the title, an optional line under it. */
@Composable
private fun ExerciseHeader(title: String, line: String?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, stringResource(R.string.close), tint = TherapyColors.muted, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = TherapyType.emphasis, color = TherapyColors.ink)
            line?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
        }
        Spacer(Modifier.size(44.dp))
    }
}

// Breathing --------------------------------------------------------------------

private enum class BreathStep { INHALE, HOLD, EXHALE, REST }

/** The rhythms: 4–4–6, box and 4–7–8, each the same player with a different count. */
private enum class Rhythm(val count: String, val steps: List<Pair<BreathStep, Double>>, val cycles: Int, val summary: Int, val duration: Int) {
    CALM("4–4–6", listOf(BreathStep.INHALE to 4.0, BreathStep.HOLD to 4.0, BreathStep.EXHALE to 6.0), 8, R.string.rhythm_summary_calm, R.string.about_2_minutes_no_sound),
    BOX("4–4–4–4", listOf(BreathStep.INHALE to 4.0, BreathStep.HOLD to 4.0, BreathStep.EXHALE to 4.0, BreathStep.REST to 4.0), 7, R.string.rhythm_summary_box, R.string.about_2_minutes_no_sound),
    // Four breaths: the usual advice when starting 4–7–8.
    RELAX("4–7–8", listOf(BreathStep.INHALE to 4.0, BreathStep.HOLD to 7.0, BreathStep.EXHALE to 8.0), 4, R.string.rhythm_summary_relax, R.string.just_over_a_minute_no_sound);

    val cycleLength get() = steps.sumOf { it.second }
    val total get() = cycleLength * cycles

    /** Which step, how far into it, and how long it lasts, at [t] seconds. */
    fun at(t: Double): Triple<Int, Double, Double> {
        var r = t % cycleLength
        steps.forEachIndexed { i, (_, s) -> if (r < s) return Triple(i, r, s); r -= s }
        return Triple(steps.lastIndex, steps.last().second, steps.last().second)
    }
}

/**
 * Breathing: one calm object, one instruction, one rhythm. A navy core with
 * two soft halos expands on the in-breath, stays still on the hold and
 * contracts slowly on the out-breath; the instruction is written beneath it,
 * so the motion is never the only cue. The × is the one way out.
 *
 * The motion is iOS's restrained treatment (A). The other treatments being
 * compared on iOS are not ported until one is chosen.
 */
@Composable
fun BreathingScreen(onClose: () -> Unit) {
    KeepScreenOn()
    val still = reduceMotion()
    val view = LocalView.current
    var rhythm by rememberSaveable { mutableStateOf(Rhythm.CALM) }
    var haptics by rememberSaveable { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    // Time breathed, advanced by frames only while running.
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(started, paused, finished) {
        if (!started || paused || finished) return@LaunchedEffect
        var last = withFrameMillis { it }
        var lastStep = rhythm.at(elapsedMs / 1000.0).first
        while (true) {
            val now = withFrameMillis { it }
            elapsedMs += now - last; last = now
            val t = elapsedMs / 1000.0
            if (t >= rhythm.total) {
                finished = true
                if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                break
            }
            val step = rhythm.at(t).first
            if (step != lastStep) {
                if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                lastStep = step
            }
        }
    }

    val t = elapsedMs / 1000.0
    val (index, into, length) = rhythm.at(t)
    val step = if (!started || finished) BreathStep.REST else rhythm.steps[index].first

    Column(Modifier.fillMaxSize().background(TherapyColors.canvas).padding(horizontal = 20.dp)) {
        val left = (rhythm.total - t).coerceAtLeast(0.0).let { kotlin.math.ceil(it).toInt() }
        ExerciseHeader("${stringResource(R.string.breathing)} · ${rhythm.count}",
            if (started && !finished) stringResource(R.string.time_left, "%d:%02d".format(left / 60, left % 60)) else stringResource(rhythm.duration),
            onClose)
        Spacer(Modifier.weight(1f))
        BreathForm(step, if (!started || finished) 1.0 else into, length, still)
        Spacer(Modifier.height(36.dp))
        Column(Modifier.fillMaxWidth().height(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                finished -> {
                    Text(stringResource(R.string.exercise_complete), fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = TherapyColors.navy)
                    Text(stringResource(R.string.take_a_moment), style = TherapyType.body, color = TherapyColors.muted)
                }
                !started -> Text(stringResource(rhythm.summary), style = TherapyType.bodyLarge, color = TherapyColors.muted, textAlign = TextAlign.Center)
                else -> {
                    AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { s ->
                        Text(stringResource(when (s) { BreathStep.INHALE -> R.string.breathe_in; BreathStep.EXHALE -> R.string.breathe_out; else -> R.string.hold }),
                            fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, color = TherapyColors.navy)
                    }
                    Text("${kotlin.math.ceil(length - into).toInt()} s", style = TherapyType.body, color = TherapyColors.muted)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // The same height before, during and after, so the form does not jump.
        Column(Modifier.fillMaxWidth().height(220.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)) {
            if (!started || finished) {
                if (!finished) {
                    Segmented(Rhythm.entries.map { it.name to (if (it == Rhythm.BOX) stringResource(R.string.rhythm_box) else it.count) },
                        rhythm.name, { rhythm = Rhythm.valueOf(it) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.light_vibration), style = TherapyType.body, color = TherapyColors.muted, modifier = Modifier.weight(1f))
                        Switch(haptics, { haptics = it }, colors = SwitchDefaults.colors(checkedTrackColor = TherapyColors.navy))
                    }
                }
                PrimaryButton(stringResource(if (finished) R.string.again else R.string.begin)) {
                    elapsedMs = 0; finished = false; paused = false; started = true
                }
                if (finished) Text(stringResource(R.string.close), style = TherapyType.body, color = TherapyColors.muted,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onClose).padding(8.dp))
            } else {
                Box(Modifier.align(Alignment.CenterHorizontally).size(56.dp).clip(CircleShape).background(TherapyColors.navy.copy(alpha = 0.08f))
                    .clickable { paused = !paused }, contentAlignment = Alignment.Center) {
                    Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, stringResource(if (paused) R.string.resume else R.string.pause), tint = TherapyColors.navy)
                }
            }
        }
    }
}

/** The form: scale 0.68 at rest, 1 when full, eased in and out with no overshoot. */
@Composable
private fun BreathForm(step: BreathStep, into: Double, length: Double, still: Boolean) {
    val x = (into / length).coerceIn(0.0, 1.0)
    val eased = (x * x * (3 - 2 * x)).toFloat()
    val rest = 0.68f
    val scale = if (still) 0.84f else when (step) {
        BreathStep.INHALE -> rest + (1 - rest) * eased
        BreathStep.HOLD -> 1f
        BreathStep.EXHALE -> 1 - (1 - rest) * eased
        BreathStep.REST -> rest
    }
    // With animations off the form stays still and the step shows in the core's depth.
    val coreAlpha = if (still) when (step) { BreathStep.INHALE -> 0.75f; BreathStep.HOLD -> 0.95f; else -> 0.55f } else 0.85f
    val halo = Color(0xFF7A9FDB)
    val progress = (into / length).toFloat().coerceIn(0f, 1f)
    val arcAlpha = ((length - into) / 0.5).toFloat().coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxWidth().height(260.dp).clearAndSetSemantics { }) {
        val full = minOf(size.width * 0.46f, 220.dp.toPx())
        val c = Offset(size.width / 2, size.height / 2)
        val r = full / 2
        drawCircle(Brush.radialGradient(0f to halo.copy(alpha = 0.13f), 0.72f to halo.copy(alpha = 0.13f), 1f to halo.copy(alpha = 0f), center = c, radius = r * scale), r * scale, c)
        drawCircle(halo.copy(alpha = 0.22f), r * 0.8f * scale, c)
        drawCircle(TherapyColors.navy.copy(alpha = coreAlpha), r * 0.6f * scale, c)
        val ring = r + 14.dp.toPx()
        drawCircle(TherapyColors.hairline.copy(alpha = 0.7f), ring, c, style = Stroke(1.5.dp.toPx()))
        if (step != BreathStep.REST) drawArc(TherapyColors.navy.copy(alpha = 0.3f * arcAlpha), -90f, 360f * progress, false,
            Offset(c.x - ring, c.y - ring), Size(ring * 2, ring * 2), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

// Meditation -----------------------------------------------------------------------

/** A quiet timer: choose a length, start, and it counts down. It promised audio and played nothing. */
@Composable
fun MeditationScreen(onClose: () -> Unit) {
    KeepScreenOn()
    var minutes by rememberSaveable { mutableStateOf(5) }
    var endsAt by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) { while (endsAt != null) { now = System.currentTimeMillis(); delay(250) } }
    val left = ((endsAt ?: (now + minutes * 60_000L)) - now).coerceAtLeast(0) / 1000

    Column(Modifier.fillMaxSize().background(TherapyColors.canvas).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ExerciseHeader(stringResource(R.string.meditation), null, onClose)
        Spacer(Modifier.weight(1f))
        Text("%d:%02d".format(left / 60, left % 60), fontSize = 56.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Default, color = TherapyColors.navy)
        Text(stringResource(when { endsAt == null -> R.string.ready_when_you_are; left == 0L -> R.string.times_up; else -> R.string.breathe_naturally }),
            style = TherapyType.body, color = TherapyColors.muted)
        Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (endsAt == null) Segmented(listOf(5, 10, 15, 20).map { "$it" to "$it min" }, "$minutes", { minutes = it.toInt() })
            PrimaryButton(stringResource(if (endsAt == null) R.string.start else R.string.stop)) {
                endsAt = if (endsAt == null) System.currentTimeMillis() + minutes * 60_000L else null
            }
            Text(stringResource(R.string.meditation_note), style = TherapyType.caption, color = TherapyColors.neutral, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1.4f))
    }
}

// Grounding ---------------------------------------------------------------------------

/** The grounding exercises, each opening a guided walkthrough. Box and 4–7–8 breathing now live in Respiração. */
@Composable
fun GroundingScreen(onBack: () -> Unit, onSenses: () -> Unit, onBodyScan: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.therapytrack.android.ui.common.BackRow(onBack)
        Text(stringResource(R.string.grounding), style = TherapyType.serifTitle, color = TherapyColors.navy)
        Muted(stringResource(R.string.grounding_intro))
        CompactCard {
            CompactLinkRow(Icons.Outlined.Visibility, "5-4-3-2-1", stringResource(R.string.five_senses_row), onClick = onSenses)
            RowDivider(inset = 48.dp)
            CompactLinkRow(Icons.Outlined.Accessibility, stringResource(R.string.body_scan), stringResource(R.string.body_scan_row), onClick = onBodyScan)
        }
    }
}

/**
 * The frame the grounding exercises share with breathing: the whole page, a
 * small ×, quiet progress marks, one step at a time with Continuar, and a
 * quiet ending. No countdown, no score, nothing to type.
 */
@Composable
private fun Walkthrough(title: String, steps: Int, onClose: () -> Unit, step: @Composable (Int) -> Unit) {
    KeepScreenOn()
    var index by rememberSaveable { mutableStateOf(0) }
    val finished = index >= steps
    Column(Modifier.fillMaxSize().background(TherapyColors.canvas).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ExerciseHeader(title, null, onClose)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(steps) { i ->
                Box(Modifier.width(18.dp).height(4.dp).clip(CircleShape).background(if (i <= index) TherapyColors.navy.copy(alpha = 0.6f) else TherapyColors.hairline))
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedContent(index, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "walkthrough") { i ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (i >= steps) {
                    Text(stringResource(R.string.exercise_complete), fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = TherapyColors.navy)
                    Text(stringResource(R.string.take_a_moment), style = TherapyType.body, color = TherapyColors.muted)
                } else step(i)
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (finished) {
                PrimaryButton(stringResource(R.string.again)) { index = 0 }
                Text(stringResource(R.string.close), style = TherapyType.body, color = TherapyColors.muted,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onClose).padding(8.dp))
            } else PrimaryButton(stringResource(if (index == steps - 1) R.string.finish_exercise else R.string.continue_)) { index++ }
        }
    }
}

/** The five senses, one at a time: a large numeral, what to notice, and a line of help for when nothing comes. */
@Composable
fun SensesScreen(onClose: () -> Unit) {
    val senses = listOf(
        Triple(5, R.string.sense_see, R.string.sense_see_help), Triple(4, R.string.sense_hear, R.string.sense_hear_help),
        Triple(3, R.string.sense_feel, R.string.sense_feel_help), Triple(2, R.string.sense_smell, R.string.sense_smell_help),
        Triple(1, R.string.sense_taste, R.string.sense_taste_help)
    )
    Walkthrough("5-4-3-2-1", senses.size, onClose) { i ->
        val (n, prompt, help) = senses[i]
        Text("$n", fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 96.sp, color = TherapyColors.navy, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.height(18.dp))
        Text(stringResource(prompt), fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = TherapyColors.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(help), style = TherapyType.body, color = TherapyColors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

private enum class Region(val title: Int, val prompt: Int) {
    HEAD(R.string.body_head, R.string.body_head_help), SHOULDERS(R.string.body_shoulders, R.string.body_shoulders_help),
    ARMS(R.string.body_arms, R.string.body_arms_help), TORSO(R.string.body_torso, R.string.body_torso_help),
    LEGS(R.string.body_legs, R.string.body_legs_help), FEET(R.string.body_feet, R.string.body_feet_help)
}

/** From the head to the feet, one region lit at a time on a plain figure, at your own pace. */
@Composable
fun BodyScanScreen(onClose: () -> Unit) {
    Walkthrough(stringResource(R.string.body_scan), Region.entries.size, onClose) { i ->
        val region = Region.entries[i]
        BodyFigure(region)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(region.title), fontFamily = SerifDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = TherapyColors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(region.prompt), style = TherapyType.body, color = TherapyColors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

/** A plain standing figure in pale blue, with one region in navy. Solid tones, so overlaps show no seam. */
@Composable
private fun BodyFigure(lit: Region) {
    val on = Color(0xFF364570); val off = Color(0xFFD9E3F2)
    fun c(r: Region) = if (r == lit) on else off
    Canvas(Modifier.size(160.dp, 334.dp).clearAndSetSemantics { }) {
        val u = size.width / 160f
        fun rect(r: Region, cx: Float, cy: Float, w: Float, h: Float, radius: Float = 0f) =
            drawRoundRect(c(r), Offset((cx - w / 2) * u, (cy - h / 2) * u), Size(w * u, h * u), androidx.compose.ui.geometry.CornerRadius(radius * u))
        rect(Region.SHOULDERS, 80f, 52f, 16f, 12f)
        rect(Region.SHOULDERS, 80f, 68f, 104f, 24f, 11f)
        drawCircle(c(Region.HEAD), 23f * u, Offset(80f * u, 25f * u))
        rect(Region.TORSO, 80f, 136f, 74f, 108f, 18f)
        rect(Region.ARMS, 23f, 134f, 20f, 108f, 10f)
        rect(Region.ARMS, 137f, 134f, 20f, 108f, 10f)
        drawCircle(c(Region.ARMS), 10f * u, Offset(18f * u, 198f * u))
        drawCircle(c(Region.ARMS), 10f * u, Offset(142f * u, 198f * u))
        rect(Region.LEGS, 63f, 256f, 30f, 124f, 15f)
        rect(Region.LEGS, 97f, 256f, 30f, 124f, 15f)
        rect(Region.FEET, 57f, 326f, 36f, 14f, 7f)
        rect(Region.FEET, 103f, 326f, 36f, 14f, 7f)
    }
}
