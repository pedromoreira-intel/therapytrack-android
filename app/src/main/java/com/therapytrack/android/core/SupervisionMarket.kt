package com.therapytrack.android.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The arithmetic of posting a supervision session, kept apart so it can be tested. */
object SupervisionMarket {
    /**
     * Euros as typed, to cents. Empty is "price to agree" (null); a comma is a
     * decimal point, as Portuguese keyboards type it. Anything else that is not
     * a non-negative amount is [INVALID].
     */
    fun priceCents(typed: String): Int? {
        val cleaned = typed.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return INVALID
        if (value.signum() < 0) return INVALID
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toInt()
    }
    const val INVALID = -1

    /** Cents back to what the form shows: "40", "37.50". */
    fun priceText(cents: Int?): String = when {
        cents == null -> ""
        cents % 100 == 0 -> (cents / 100).toString()
        else -> String.format(java.util.Locale.ROOT, "%d.%02d", cents / 100, cents % 100)
    }

    /** "40 €" / "37,50 €" for a card, or null when the price is to be agreed. */
    fun priceLabel(cents: Int?, currency: String, locale: java.util.Locale = java.util.Locale.getDefault()): String? {
        cents ?: return null
        val format = java.text.NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = java.util.Currency.getInstance(currency) }
        format.minimumFractionDigits = if (cents % 100 == 0) 0 else 2
        format.maximumFractionDigits = format.minimumFractionDigits
        return format.format(cents / 100.0)
    }

    /** A date ("2026-10-18") and a time ("19:00") typed in the phone's zone, as an instant; null if either does not parse. */
    fun startsAt(date: String, time: String, zone: ZoneId = ZoneId.systemDefault()): Instant? {
        val d = runCatching { LocalDate.parse(date.trim()) }.getOrNull() ?: return null
        val t = runCatching { LocalTime.parse(time.trim()) }.getOrNull() ?: return null
        return d.atTime(t).atZone(zone).toInstant()
    }

    /** Topics as typed, "trauma, casal" → ["trauma", "casal"]. */
    fun topics(typed: String): List<String> = typed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
