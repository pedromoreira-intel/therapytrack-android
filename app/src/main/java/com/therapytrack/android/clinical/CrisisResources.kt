package com.therapytrack.android.clinical

import java.util.Locale

data class CrisisResource(val name: String, val number: String, val detail: String, val isEmergency: Boolean) {
    /** Digits and a leading plus only, for a dial intent. */
    val dialable: String get() = number.filter { it.isDigit() || it == '+' }
}

/**
 * Who to call, by country. Portugal is the pilot and the fallback for an
 * unknown region: 112 is the EU-wide emergency number.
 *
 * Detail text is in Portuguese for PT and English elsewhere, deliberately:
 * a person in a crisis in Portugal reads Portuguese.
 */
object CrisisResources {
    fun resources(regionCode: String? = null): List<CrisisResource> = when ((regionCode ?: deviceRegion).uppercase()) {
        "PT" -> listOf(
            CrisisResource("Serviços de emergência", "112", "Perigo imediato de vida", true),
            CrisisResource("SNS 24", "808 24 24 24", "Linha nacional de saúde — apoio em saúde mental, 24 horas por dia", false),
            CrisisResource("Linha de Prevenção do Suicídio", "21 354 45 45", "SOS Voz Amiga — prevenção do suicídio", false)
        )
        "US" -> listOf(
            CrisisResource("Emergency services", "911", "Immediate danger to life", true),
            CrisisResource("988 Suicide & Crisis Lifeline", "988", "Call or text, 24/7", false)
        )
        "GB", "UK" -> listOf(
            CrisisResource("Emergency services", "999", "Immediate danger to life", true),
            CrisisResource("Samaritans", "116 123", "Free, 24/7", false)
        )
        "BR" -> listOf(
            CrisisResource("Emergency services", "192", "Immediate danger to life", true),
            CrisisResource("CVV — Centro de Valorização da Vida", "188", "Emotional support and suicide prevention, 24/7", false)
        )
        else -> listOf(CrisisResource("Emergency services", "112", "Immediate danger to life", true))
    }

    fun needsInternationalDirectory(regionCode: String? = null): Boolean =
        (regionCode ?: deviceRegion).uppercase() !in setOf("PT", "US", "GB", "UK", "BR")

    const val INTERNATIONAL_DIRECTORY = "https://findahelpline.com"

    private val deviceRegion: String get() = Locale.getDefault().country.ifEmpty { "PT" }
}
