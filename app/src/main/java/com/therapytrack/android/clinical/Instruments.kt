package com.therapytrack.android.clinical

import com.therapytrack.android.R
import com.therapytrack.android.core.Api

/**
 * The three instruments, as string resources so the wording lives with the
 * other translations. The Portuguese item text is a working translation
 * pending clinical review against the published PT versions (PHQ-9/GAD-7:
 * Pfizer's official translations; WAI-SR: licence required).
 */
data class Instrument(
    val api: Api.Instrument,
    val title: Int,
    val intro: Int,
    val items: List<Int>,
    /** Value → label. PHQ-9/GAD-7 score 0–3; WAI-SR 1–5. */
    val options: List<Pair<Int, Int>>,
    val maxScore: Double
)

object Instruments {
    private val frequency = listOf(
        0 to R.string.opt_not_at_all, 1 to R.string.opt_several_days, 2 to R.string.opt_more_than_half, 3 to R.string.opt_nearly_every_day)
    private val alliance = listOf(
        1 to R.string.opt_seldom, 2 to R.string.opt_sometimes, 3 to R.string.opt_fairly_often, 4 to R.string.opt_very_often, 5 to R.string.opt_always)

    val phq9 = Instrument(Api.Instrument.PHQ9, R.string.phq9_title, R.string.two_weeks_intro, listOf(
        R.string.phq9_1, R.string.phq9_2, R.string.phq9_3, R.string.phq9_4, R.string.phq9_5,
        R.string.phq9_6, R.string.phq9_7, R.string.phq9_8, R.string.phq9_9), frequency, 27.0)

    val gad7 = Instrument(Api.Instrument.GAD7, R.string.gad7_title, R.string.two_weeks_intro, listOf(
        R.string.gad7_1, R.string.gad7_2, R.string.gad7_3, R.string.gad7_4, R.string.gad7_5, R.string.gad7_6, R.string.gad7_7), frequency, 21.0)

    val waisr = Instrument(Api.Instrument.WAISR, R.string.waisr_title, R.string.waisr_intro, listOf(
        R.string.waisr_1, R.string.waisr_2, R.string.waisr_3, R.string.waisr_4, R.string.waisr_5, R.string.waisr_6,
        R.string.waisr_7, R.string.waisr_8, R.string.waisr_9, R.string.waisr_10, R.string.waisr_11, R.string.waisr_12), alliance, 5.0)

    val all = listOf(phq9, gad7, waisr)
    fun byApi(api: Api.Instrument) = all.first { it.api == api }
}
