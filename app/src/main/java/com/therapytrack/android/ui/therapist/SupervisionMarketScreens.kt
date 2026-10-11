package com.therapytrack.android.ui.therapist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiError
import com.therapytrack.android.core.ApiSupervisionOffer
import com.therapytrack.android.core.ApiSupervisionPlaceRequest
import com.therapytrack.android.core.ApiSupervisorListing
import com.therapytrack.android.core.ApiTimestamp
import com.therapytrack.android.core.SupervisionMarket
import com.therapytrack.android.ui.auth.errorText
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.Pill
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Finding supervision and offering it (REA-31), as on iOS.
//
// One rule runs through every screen here: what the app has checked about a
// supervisor is shown on every card, in the same words, and an unchecked
// supervisor says so rather than saying nothing. Silence would read as verified.

// Shared pieces --------------------------------------------------------------------

/** Back, a serif title, and the content, scrolling. */
@Composable
private fun MarketFrame(title: String, onBack: () -> Unit, backLabel: String = stringResource(R.string.back),
                        content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text("‹ $backLabel", style = TherapyType.bodyLarge, color = TherapyColors.navy)
        }
        Text(title, style = TherapyType.display, color = TherapyColors.navy)
        content()
    }
}

@Composable
private fun Header(text: String) = Overline(text, modifier = Modifier.padding(top = 10.dp))

/** The verification line, identical wherever a supervisor appears. */
@Composable
fun VerificationLine(status: String) {
    val checked = status == "licence_checked" || status == "supervisor_verified"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (checked) Icons.Outlined.Verified else Icons.Outlined.Info, null,
            tint = if (checked) TherapyColors.success else TherapyColors.neutral, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(when (status) {
            "licence_checked" -> R.string.sm_v_licence
            "supervisor_verified" -> R.string.sm_v_supervisor
            else -> R.string.sm_v_self
        }), style = TherapyType.caption, color = TherapyColors.muted)
    }
}

@Composable
private fun placeLabel(status: String) = stringResource(when (status) {
    "accepted" -> R.string.sm_p_accepted
    "declined" -> R.string.sm_p_declined
    "withdrawn" -> R.string.sm_p_withdrawn
    else -> R.string.sm_p_requested
})

private fun placeColor(status: String) = when (status) {
    "accepted" -> TherapyColors.success
    "declined" -> TherapyColors.critical
    else -> TherapyColors.warning
}

private val whenFormat get() = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.getDefault())

/** "qui, 16 out · 19:00 · 90 min" */
@Composable
private fun whenLine(o: ApiSupervisionOffer): String {
    val date = ApiTimestamp.parse(o.startsAt)?.atZone(ZoneId.systemDefault())?.let { whenFormat.format(it) } ?: o.startsAt
    return "$date · " + stringResource(R.string.sm_minutes, o.durationMinutes)
}

/** The server's refusals the person can act on, in their language; anything else as the app says errors. */
@Composable
private fun marketError(e: Throwable): String {
    val known = (e as? ApiError.Server)?.message?.let {
        when (it) {
            "Places cannot be fewer than those already confirmed." -> R.string.sm_err_fewer
            "Publish your supervisor listing before posting sessions." -> R.string.sm_err_publish_first
            "The kind of session cannot change. Cancel it and post a new one." -> R.string.sm_err_kind
            "The supervisor declined your request for this session" -> R.string.sm_err_declined
            "This is your own session" -> R.string.sm_err_own
            "This session has already started" -> R.string.sm_err_started
            "This session is full", "This session is already full" -> R.string.sm_err_full
            "This session was cancelled" -> R.string.sm_err_cancelled
            "You have already asked for a place" -> R.string.sm_err_already
            else -> null
        }
    }
    return known?.let { stringResource(it) } ?: errorText(e)
}

/** One session, as a card: in the list, in "your places", and in the supervisor's own list. */
@Composable
private fun OfferCard(o: ApiSupervisionOffer, asOwner: Boolean = false, onClick: (() -> Unit)? = null) {
    Card(Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        tint = if (o.isCancelled || o.isPast) TherapyColors.canvas else Color.White) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(stringResource(if (o.kind == "individual") R.string.sm_individual else R.string.sm_group), TherapyColors.navy)
            Pill(stringResource(if (o.format == "in_person") R.string.sm_in_person else R.string.sm_online), TherapyColors.muted)
            Spacer(Modifier.weight(1f))
            when {
                o.isCancelled -> Pill(stringResource(R.string.sm_cancelled), TherapyColors.critical)
                o.myPlace != null -> Pill(placeLabel(o.myPlace.status), placeColor(o.myPlace.status))
                asOwner && (o.requestsWaiting ?: 0) > 0 -> Pill(stringResource(R.string.sm_waiting, o.requestsWaiting ?: 0), TherapyColors.warning)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(o.title, style = TherapyType.emphasisLarge, color = TherapyColors.ink)
        Text(whenLine(o), style = TherapyType.body, color = TherapyColors.ink)
        if (!asOwner) {
            Spacer(Modifier.height(6.dp))
            Text(o.supervisor.name, style = TherapyType.emphasis, color = TherapyColors.ink)
            o.supervisor.headline?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
            VerificationLine(o.supervisor.verificationStatus)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(when {
                o.placesLeft == 0 -> stringResource(R.string.sm_full)
                o.places == 1 -> stringResource(R.string.sm_place_available)
                else -> stringResource(R.string.sm_places_left, o.placesLeft, o.places)
            }, style = TherapyType.caption, color = if (o.placesLeft == 0) TherapyColors.critical else TherapyColors.muted, modifier = Modifier.weight(1f))
            val price = SupervisionMarket.priceLabel(o.priceCents, o.currency)
            if (price != null) Text(price, style = TherapyType.emphasis, color = TherapyColors.ink)
            else Text(stringResource(R.string.sm_price_to_agree), style = TherapyType.caption, color = TherapyColors.muted)
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TherapyType.bodyLarge, color = TherapyColors.ink, modifier = Modifier.weight(1f))
        OutlinedButton({ onChange((value - step).coerceAtLeast(range.first)) }, enabled = value > range.first) { Text("−") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton({ onChange((value + step).coerceAtMost(range.last)) }, enabled = value < range.last) { Text("+") }
    }
}

// Finding ------------------------------------------------------------------------------

/**
 * Upcoming sessions other therapists have posted, and the places this one has
 * asked for. Empty is a real state, not a failure: until supervisors post
 * there is nothing to find, and the screen says so plainly.
 */
@Composable
fun FindSupervisionScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    var offers by remember { mutableStateOf<List<ApiSupervisionOffer>>(emptyList()) }
    var myPlaces by remember { mutableStateOf<List<ApiSupervisionOffer>>(emptyList()) }
    var kind by rememberSaveable { mutableStateOf("all") }
    var format by rememberSaveable { mutableStateOf("any") }
    var topic by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<ApiSupervisionOffer?>(null) }

    LaunchedEffect(reload, kind, format) {
        loading = true
        failed = runCatching {
            offers = container.api.supervisionOffers(kind.takeIf { it != "all" }, format.takeIf { it != "any" }, topic)
        }.isFailure
        myPlaces = runCatching { container.api.mySupervisionPlaces() }.getOrDefault(myPlaces)
        loading = false
    }
    LaunchedEffect(Unit) {
        listOf("supervision_response", "supervision_cancelled", "supervision_changed").forEach {
            runCatching { container.api.markNotificationsRead(kind = it) }
        }
        container.refreshInbox()
    }

    selected?.let { offer ->
        BackHandler { selected = null; reload++ }
        OfferDetail(offer) { selected = null; reload++ }
        return
    }

    val upcomingPlaces = myPlaces.filter { !it.isPast && !it.isCancelled && it.myPlace?.status != "declined" }

    MarketFrame(stringResource(R.string.sm_find), onBack) {
        if (upcomingPlaces.isNotEmpty()) {
            Header(stringResource(R.string.sm_your_places))
            upcomingPlaces.forEach { o -> OfferCard(o) { selected = o } }
        }
        Header(stringResource(R.string.sm_upcoming))
        Segmented(listOf("all" to stringResource(R.string.sm_all), "group" to stringResource(R.string.sm_group),
            "individual" to stringResource(R.string.sm_individual)), kind, onSelect = { kind = it })
        Segmented(listOf("any" to stringResource(R.string.sm_any_format), "online" to stringResource(R.string.sm_online),
            "in_person" to stringResource(R.string.sm_in_person)), format, onSelect = { format = it })
        OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(stringResource(R.string.sm_topic_search), color = TherapyColors.neutral) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TherapyColors.neutral) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { reload++ }))

        when {
            loading && offers.isEmpty() -> Muted(stringResource(R.string.loading))
            failed -> Card {
                Text(stringResource(R.string.sm_load_failed), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
                TextButton({ reload++ }) { Text(stringResource(R.string.sm_try_again), color = TherapyColors.navy) }
            }
            else -> {
                // A session I already hold or asked for is under Your places;
                // listing it twice reads as two sessions.
                val open = offers.filter { it.myPlace == null || it.myPlace.status == "declined" }
                // Empty means nothing posted at all, not "everything posted is already mine".
                if (offers.isEmpty()) Card(tint = TherapyColors.canvas) {
                    Text(stringResource(R.string.sm_none_title), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
                    Muted(stringResource(R.string.sm_none_body))
                }
                open.forEach { o -> OfferCard(o) { selected = o } }
            }
        }
    }
}

/** One session in full, and asking for a place in it. */
@Composable
private fun OfferDetail(offer: ApiSupervisionOffer, onClose: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf(offer.myPlace) }
    var note by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val canAsk = place == null && !offer.isCancelled && offer.placesLeft > 0 && !offer.isPast

    MarketFrame(stringResource(R.string.sm_session), onClose, backLabel = stringResource(R.string.sm_close)) {
        OfferCard(offer)
        offer.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.bodyLarge, color = TherapyColors.ink) }
        if (offer.topics.isNotEmpty()) {
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                offer.topics.forEach { Pill(it, TherapyColors.muted) }
            }
        }
        offer.location?.takeIf { it.isNotBlank() }?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null, tint = TherapyColors.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Muted(it)
            }
        }
        Text(stringResource(R.string.sm_fee_note), style = TherapyType.caption, color = TherapyColors.muted)

        val p = place
        if (p != null) {
            Pill(placeLabel(p.status), placeColor(p.status))
            if (p.status == "accepted") Text(stringResource(R.string.sm_added_to_log), style = TherapyType.caption, color = TherapyColors.muted)
            if (p.status == "requested" || p.status == "accepted") {
                TextButton({ confirmWithdraw = true }, enabled = !working) {
                    Text(stringResource(if (p.status == "accepted") R.string.sm_give_up else R.string.sm_withdraw_request), color = TherapyColors.critical)
                }
            }
        } else if (canAsk) {
            Text(stringResource(R.string.sm_bring), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().height(120.dp))
            Text(stringResource(R.string.sm_bring_hint), style = TherapyType.caption, color = TherapyColors.muted)
            PrimaryButton(stringResource(R.string.sm_ask), loading = working) {
                working = true; error = null
                scope.launch {
                    try {
                        val r = container.api.requestSupervisionPlace(offer.id, note)
                        place = ApiSupervisionOffer.Place(r.id, r.status)
                    } catch (e: Exception) { error = e } finally { working = false }
                }
            }
        }
        error?.let { ErrorText(marketError(it)) }
    }

    if (confirmWithdraw) AlertDialog(onDismissRequest = { confirmWithdraw = false },
        title = { Text(stringResource(R.string.sm_withdraw_q)) },
        confirmButton = { TextButton({
            confirmWithdraw = false
            val p = place ?: return@TextButton
            working = true
            scope.launch {
                try { container.api.withdrawSupervisionPlace(p.id); onClose() }
                catch (e: Exception) { error = e } finally { working = false }
            }
        }) { Text(stringResource(R.string.sm_withdraw), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirmWithdraw = false }) { Text(stringResource(R.string.cancel)) } })
}

// Offering --------------------------------------------------------------------------------

/**
 * The supervisor's side: choosing to be listed, posting sessions, and
 * answering who asks for a place. Publishing is consent, so the text of what
 * becomes visible is on screen at the moment of agreeing, and withdrawing is
 * one button.
 */
@Composable
fun OfferSupervisionScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var listing by remember { mutableStateOf<ApiSupervisorListing?>(null) }
    var offers by remember { mutableStateOf<List<ApiSupervisionOffer>>(emptyList()) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var working by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var posting by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<ApiSupervisionOffer?>(null) }
    var confirmPublish by remember { mutableStateOf(false) }
    var confirmWithdraw by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        try {
            listing = container.api.mySupervisorListing()
            offers = runCatching { container.api.mySupervisionOffers() }.getOrDefault(offers)
            error = null
        } catch (e: Exception) { error = e }
    }
    LaunchedEffect(Unit) { runCatching { container.api.markNotificationsRead(kind = "supervision_request") }; container.refreshInbox() }

    if (posting) {
        BackHandler { posting = false }
        OfferForm(editing = null) { posting = false; reload++ }
        return
    }
    open?.let { offer ->
        BackHandler { open = null; reload++ }
        OfferRequests(offer) { open = null; reload++ }
        return
    }

    fun act(block: suspend () -> ApiSupervisorListing) {
        working = true
        scope.launch {
            try { listing = block(); error = null } catch (e: Exception) { error = e } finally { working = false }
        }
    }

    val upcoming = offers.filter { !it.isPast && !it.isCancelled }
    val earlier = offers.filter { it.isPast || it.isCancelled }

    MarketFrame(stringResource(R.string.sm_offer), onBack) {
        val l = listing
        if (l == null) { if (error == null) Muted(stringResource(R.string.loading)) }
        else {
            Card {
                Pill(stringResource(if (l.isPublished) R.string.sm_listed else R.string.sm_not_listed),
                    if (l.isPublished) TherapyColors.success else TherapyColors.muted)
                Spacer(Modifier.height(8.dp))
                if (!l.headline.isNullOrBlank()) Text(l.headline, style = TherapyType.emphasis, color = TherapyColors.ink)
                else Text(stringResource(R.string.sm_add_headline), style = TherapyType.caption, color = TherapyColors.warning)
                l.city?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
                VerificationLine(l.verificationStatus)
                Spacer(Modifier.height(8.dp))
                if (l.isPublished) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sm_accepting), style = TherapyType.bodyLarge, color = TherapyColors.ink, modifier = Modifier.weight(1f))
                        Switch(l.acceptingSupervisees, { v -> act { container.api.setAcceptingSupervisees(v) } }, enabled = !working)
                    }
                    TextButton({ confirmWithdraw = true }, enabled = !working, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(stringResource(R.string.sm_withdraw_listing), color = TherapyColors.critical)
                    }
                } else {
                    Muted(stringResource(R.string.sm_publish_intro))
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(stringResource(R.string.sm_publish_listing), loading = working) { confirmPublish = true }
                }
            }

            if (l.isPublished) PrimaryButton(stringResource(R.string.sm_post), icon = Icons.Outlined.Add) { posting = true }

            if (upcoming.isNotEmpty()) {
                Header(stringResource(R.string.sm_your_upcoming))
                upcoming.forEach { o -> OfferCard(o, asOwner = true) { open = o } }
            } else if (l.isPublished) Text(stringResource(R.string.sm_no_upcoming), style = TherapyType.caption, color = TherapyColors.muted)

            if (earlier.isNotEmpty()) {
                Header(stringResource(R.string.sm_earlier))
                earlier.forEach { o -> OfferCard(o, asOwner = true) { open = o } }
            }
        }
        error?.let { ErrorText(marketError(it)) }
    }

    if (confirmPublish) AlertDialog(onDismissRequest = { confirmPublish = false },
        title = { Text(stringResource(R.string.sm_publish_q)) },
        text = { Text(stringResource(R.string.sm_publish_consent)) },
        confirmButton = { TextButton({ confirmPublish = false; act { container.api.publishSupervisorListing() } }) { Text(stringResource(R.string.sm_publish)) } },
        dismissButton = { TextButton({ confirmPublish = false }) { Text(stringResource(R.string.cancel)) } })
    if (confirmWithdraw) AlertDialog(onDismissRequest = { confirmWithdraw = false },
        title = { Text(stringResource(R.string.sm_withdraw_listing_q)) },
        text = { Text(stringResource(R.string.sm_withdraw_listing_body)) },
        confirmButton = { TextButton({ confirmWithdraw = false; act { container.api.withdrawSupervisorListing() } }) { Text(stringResource(R.string.sm_withdraw), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirmWithdraw = false }) { Text(stringResource(R.string.cancel)) } })
}

/** Who has asked for a place in one of my sessions, and my answer. Editing and cancelling start here. */
@Composable
private fun OfferRequests(initial: ApiSupervisionOffer, onClose: () -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf(initial) }
    var requests by remember { mutableStateOf<List<ApiSupervisionPlaceRequest>?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var working by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    val live = !offer.isCancelled && !offer.isPast

    LaunchedEffect(reload) {
        try { requests = container.api.supervisionPlaceRequests(offer.id); error = null } catch (e: Exception) { error = e }
        // The card's counts ("1 waiting", places left) move with every answer.
        runCatching { container.api.mySupervisionOffers().firstOrNull { it.id == offer.id }?.let { offer = it } }
    }

    if (editing) {
        BackHandler { editing = false }
        OfferForm(editing = offer) { saved -> saved?.let { offer = it.copy(requestsWaiting = offer.requestsWaiting) }; editing = false; reload++ }
        return
    }

    MarketFrame(stringResource(R.string.sm_session), onClose, backLabel = stringResource(R.string.sm_close)) {
        OfferCard(offer, asOwner = true)
        if (live) TextButton({ editing = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text(stringResource(R.string.sm_edit_session), color = TherapyColors.navy, style = TherapyType.emphasisLarge)
        }

        Header(stringResource(R.string.sm_requests))
        val list = requests
        when {
            list == null -> if (error == null) Muted(stringResource(R.string.loading))
            list.isEmpty() -> Text(stringResource(R.string.sm_no_requests), style = TherapyType.caption, color = TherapyColors.muted)
            else -> list.forEach { r ->
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.therapist.name, style = TherapyType.emphasisLarge, color = TherapyColors.ink, modifier = Modifier.weight(1f))
                        // "Waiting for the supervisor" is the asker's words; here the buttons say it.
                        if (r.status != "requested") Pill(placeLabel(r.status), placeColor(r.status))
                    }
                    r.therapist.headline?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.caption, color = TherapyColors.muted) }
                    r.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = TherapyType.bodyLarge, color = TherapyColors.ink, modifier = Modifier.padding(top = 6.dp)) }
                    if (r.status == "requested" && !offer.isCancelled) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(true, false).forEach { accept ->
                                val label = stringResource(if (accept) R.string.sm_accept else R.string.sm_decline)
                                val go = {
                                    working = true
                                    scope.launch {
                                        try { container.api.answerSupervisionPlace(r.id, accept); reload++ } catch (e: Exception) { error = e } finally { working = false }
                                    }
                                    Unit
                                }
                                if (accept) PrimaryButton(label, Modifier.weight(1f), enabled = !working, onClick = go)
                                else OutlinedButton(go, Modifier.weight(1f).height(54.dp), enabled = !working) { Text(label, color = TherapyColors.navy) }
                            }
                        }
                    }
                }
            }
        }

        if (live) TextButton({ confirmCancel = true }, enabled = !working, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text(stringResource(R.string.sm_cancel_session), color = TherapyColors.critical)
        }
        error?.let { ErrorText(marketError(it)) }
    }

    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false },
        title = { Text(stringResource(R.string.sm_cancel_q)) },
        text = { Text(stringResource(R.string.sm_cancel_body)) },
        confirmButton = { TextButton({
            confirmCancel = false; working = true
            scope.launch {
                try { container.api.cancelSupervisionOffer(offer.id); onClose() } catch (e: Exception) { error = e } finally { working = false }
            }
        }) { Text(stringResource(R.string.sm_cancel_confirm), color = TherapyColors.critical) } },
        dismissButton = { TextButton({ confirmCancel = false }) { Text(stringResource(R.string.cancel)) } })
}

/** Posting a session ([editing] null) or changing one. `onDone` gets what was saved, or null when cancelled. */
@Composable
private fun OfferForm(editing: ApiSupervisionOffer?, onDone: (ApiSupervisionOffer?) -> Unit) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val zone = ZoneId.systemDefault()
    val start = editing?.let { ApiTimestamp.parse(it.startsAt)?.atZone(zone) }
    var kind by rememberSaveable { mutableStateOf(editing?.kind ?: "group") }
    var title by rememberSaveable { mutableStateOf(editing?.title ?: "") }
    var details by rememberSaveable { mutableStateOf(editing?.description ?: "") }
    var topics by rememberSaveable { mutableStateOf(editing?.topics?.joinToString(", ") ?: "") }
    var date by rememberSaveable { mutableStateOf(start?.toLocalDate()?.toString() ?: LocalDate.now().plusDays(7).toString()) }
    var time by rememberSaveable { mutableStateOf(start?.toLocalTime()?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "19:00") }
    var duration by rememberSaveable { mutableIntStateOf(editing?.durationMinutes ?: 90) }
    var format by rememberSaveable { mutableStateOf(editing?.format ?: "online") }
    var location by rememberSaveable { mutableStateOf(editing?.location ?: "") }
    var places by rememberSaveable { mutableIntStateOf(editing?.places ?: 4) }
    var price by rememberSaveable { mutableStateOf(SupervisionMarket.priceText(editing?.priceCents)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    val startsAt = SupervisionMarket.startsAt(date, time, zone)
    val inFuture = startsAt != null && startsAt.isAfter(Instant.now())
    val cents = SupervisionMarket.priceCents(price)
    val canSave = title.isNotBlank() && inFuture && (format == "online" || location.isNotBlank()) && cents != SupervisionMarket.INVALID

    FormFrame(stringResource(if (editing == null) R.string.sm_post else R.string.sm_edit_session), onCancel = { onDone(null) },
        canSave = canSave, busy = busy, error = null,
        saveLabel = stringResource(if (editing == null) R.string.sm_post_button else R.string.save), onSave = {
            busy = true; error = null
            scope.launch {
                try {
                    val saved = container.api.saveSupervisionOffer(editing?.id, kind, title.trim(), details.trim(), SupervisionMarket.topics(topics),
                        startsAt!!, duration, format, if (format == "in_person") location.trim() else null,
                        if (kind == "individual") 1 else places, cents)
                    onDone(saved)
                } catch (e: Exception) { error = e } finally { busy = false }
            }
        }) {
        Muted(stringResource(R.string.sm_kind))
        // A group with confirmed places cannot become a one-to-one; the server
        // refuses it, so the form does not offer it.
        if (editing == null) ChipRow(listOf("group" to R.string.sm_group, "individual" to R.string.sm_individual), kind) { kind = it }
        else Pill(stringResource(if (kind == "individual") R.string.sm_individual else R.string.sm_group), TherapyColors.navy)
        Field(title, { title = it }, R.string.sm_title_field)
        Field(details, { details = it }, R.string.sm_description, single = false)
        Field(topics, { topics = it }, R.string.sm_topics)
        Text(stringResource(R.string.sm_shown_note), style = TherapyType.caption, color = TherapyColors.muted)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(3f)) { Field(date, { date = it }, R.string.sm_date, isError = startsAt == null && date.isNotBlank()) }
            Column(Modifier.weight(2f)) { Field(time, { time = it }, R.string.sm_time, isError = startsAt == null && time.isNotBlank()) }
        }
        if (startsAt != null && !inFuture) Text(stringResource(R.string.sm_in_future), style = TherapyType.caption, color = TherapyColors.critical)
        Stepper(stringResource(R.string.sm_duration, duration), duration, 30..240, step = 15) { duration = it }
        Muted(stringResource(R.string.sm_format))
        ChipRow(listOf("online" to R.string.sm_online, "in_person" to R.string.sm_in_person), format) { format = it }
        if (format == "in_person") Field(location, { location = it }, R.string.sm_city)
        if (kind == "group") Stepper(stringResource(R.string.sm_places, places), places, 1..20) { places = it }
        Field(price, { price = it }, R.string.sm_price, isError = cents == SupervisionMarket.INVALID, keyboard = KeyboardType.Decimal)
        Text(stringResource(R.string.sm_price_note), style = TherapyType.caption, color = TherapyColors.muted)
        if (editing != null) Text(stringResource(R.string.sm_change_note), style = TherapyType.caption, color = TherapyColors.muted)
        error?.let { ErrorText(marketError(it)) }
    }
}
