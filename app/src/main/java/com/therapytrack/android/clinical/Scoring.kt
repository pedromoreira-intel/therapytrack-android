package com.therapytrack.android.clinical

/**
 * Local scoring, for what the person sees the moment they finish. The server
 * scores independently and its result is the one on record; nothing here
 * decides whether an alert is raised.
 */
object Scoring {
    enum class Band { MINIMAL, MILD, MODERATE, MODERATELY_SEVERE, SEVERE }
    enum class Alliance { WEAK, FAIR, MODERATE, GOOD, STRONG }

    /** Item 9 is the suicidal-ideation item; any answer above 0 shows crisis resources. */
    const val PHQ9_ITEM9_INDEX = 8
    /** WAI-SR item 4 is reverse-scored. */
    const val WAISR_REVERSED_INDEX = 3

    fun phq9Band(score: Int): Band = when {
        score <= 4 -> Band.MINIMAL
        score <= 9 -> Band.MILD
        score <= 14 -> Band.MODERATE
        score <= 19 -> Band.MODERATELY_SEVERE
        else -> Band.SEVERE
    }

    fun gad7Band(score: Int): Band = when {
        score <= 4 -> Band.MINIMAL
        score <= 9 -> Band.MILD
        score <= 14 -> Band.MODERATE
        else -> Band.SEVERE
    }

    fun waiSrMean(responses: List<Int>): Double {
        if (responses.isEmpty()) return 0.0
        return responses.mapIndexed { i, r -> if (i == WAISR_REVERSED_INDEX) 6 - r else r }.sum().toDouble() / responses.size
    }

    fun waiSrAlliance(mean: Double): Alliance = when {
        mean < 3.0 -> Alliance.WEAK
        mean < 3.5 -> Alliance.FAIR
        mean < 4.0 -> Alliance.MODERATE
        mean < 4.5 -> Alliance.GOOD
        else -> Alliance.STRONG
    }

    /** The key the server uses for the same band, so both sides can be compared in tests. */
    fun serverKey(band: Band): String = band.name.lowercase()
    fun serverKey(alliance: Alliance): String = alliance.name.lowercase()
}
