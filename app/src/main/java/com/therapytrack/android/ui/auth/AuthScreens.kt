package com.therapytrack.android.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.clinical.CrisisResources
import com.therapytrack.android.core.ApiError
import com.therapytrack.android.ui.common.ButtonStyle
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.Overline
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.common.Segmented
import com.therapytrack.android.ui.theme.TherapyColors
import com.therapytrack.android.ui.theme.TherapyRadius
import com.therapytrack.android.ui.theme.TherapyType
import kotlinx.coroutines.launch

/** Something the server said in a form a person can act on. */
@Composable
fun errorText(e: Throwable): String = when (e) {
    is ApiError.Network -> stringResource(R.string.offline_login)
    is ApiError.Unauthorized -> stringResource(R.string.login_failed)
    is ApiError.Server -> e.message ?: stringResource(R.string.error_generic)
    else -> stringResource(R.string.error_generic)
}

/** The champagne head-and-brain mark, optionally inside the ring the sign-in hero draws around it. */
@Composable
fun BrandMark(size: androidx.compose.ui.unit.Dp = 72.dp, ring: Boolean = false) {
    val mark = @Composable { Image(painterResource(R.drawable.ic_brand_mark), null, Modifier.size(size)) }
    if (!ring) mark() else Box(
        Modifier.size(size + 36.dp).clip(CircleShape).background(TherapyColors.champagne.copy(alpha = 0.12f)).border(1.dp, TherapyColors.champagne.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) { mark() }
}

/** A label + filled field, as the iOS sign-in sheet lays them out. */
@Composable
fun LabelledField(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", password: Boolean = false,
                  keyboard: KeyboardType = KeyboardType.Text) {
    Column(Modifier.fillMaxWidth()) {
        Overline(label, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(value, onChange, placeholder = { Text(placeholder, color = TherapyColors.neutral) }, singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(TherapyRadius.medium),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = TherapyColors.canvas, unfocusedContainerColor = TherapyColors.canvas,
                focusedBorderColor = TherapyColors.navy, unfocusedBorderColor = TherapyColors.hairline),
            modifier = Modifier.fillMaxWidth())
    }
}

/** Navy-to-violet hero on top, a white sheet with 24dp top corners below: the sign-in family of screens. */
@Composable
private fun HeroSheet(subtitle: String, sheet: @Composable ColumnScopeAlias.() -> Unit) {
    Column(Modifier.fillMaxSize().background(TherapyColors.heroGradient).verticalScroll(rememberScrollState()).imePadding()) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(ring = true)
            Spacer(Modifier.height(20.dp))
            Text("TherapyTrack", style = TherapyType.display, color = TherapyColors.pearl)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = TherapyType.bodyLarge, color = TherapyColors.rose.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = TherapyRadius.xlarge, topEnd = TherapyRadius.xlarge)).background(Color.White).padding(24.dp, 28.dp, 24.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { sheet() }
    }
}
private typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope

@Composable
fun ConsentScreen(onConsentGiven: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var terms by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var how by rememberSaveable { mutableStateOf(false) }
    val or = stringResource(R.string.or_word)
    val numbers = CrisisResources.resources().joinToString(" $or ") { "${it.number} (${it.name})" }

    Column(Modifier.fillMaxSize().background(TherapyColors.canvas).statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        BrandMark(64.dp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.consent_welcome), style = TherapyType.display, color = TherapyColors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        // How it works — a collapsible navy-tinted row
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(TherapyRadius.large)).background(TherapyColors.navy.copy(alpha = 0.10f)).clickable { how = !how }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HelpOutline, null, tint = TherapyColors.navy)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.how_it_works), style = TherapyType.h3, color = TherapyColors.navy, modifier = Modifier.weight(1f))
                Icon(if (how) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = TherapyColors.navy)
            }
            AnimatedVisibility(how) { Text(stringResource(R.string.consent_body), style = TherapyType.body, color = TherapyColors.ink, modifier = Modifier.padding(top = 12.dp)) }
        }
        Spacer(Modifier.height(20.dp))

        Card(radius = TherapyRadius.xlarge, padding = 20.dp) {
            Text(stringResource(R.string.consent_title), style = TherapyType.h2, color = TherapyColors.ink)
            Spacer(Modifier.height(12.dp))
            ConsentRow(terms, stringResource(R.string.terms_title), stringResource(R.string.consent_terms)) { terms = !terms }
            ConsentRow(privacy, stringResource(R.string.privacy_title), stringResource(R.string.consent_privacy)) { privacy = !privacy }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(TherapyColors.hairline))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(TherapyRadius.medium)).background(TherapyColors.warning.copy(alpha = 0.10f)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = TherapyColors.warning); Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.important_notice), style = TherapyType.emphasisLarge, color = TherapyColors.ink)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.consent_disclaimer, numbers), style = TherapyType.body, color = TherapyColors.muted)
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(R.string.continue_), enabled = terms && privacy, style = ButtonStyle.Champagne, icon = Icons.AutoMirrored.Filled.ArrowForward) {
            // Best effort and not blocking: a network failure must not trap
            // someone here. The state is visible again under Profile → Your data.
            scope.launch { runCatching { api.recordConsent("clinical_data", true) } }
            onConsentGiven()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConsentRow(on: Boolean, title: String, subtitle: String, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) TherapyColors.navy else Color.Transparent).border(2.dp, if (on) TherapyColors.navy else TherapyColors.neutral, CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
        Spacer(Modifier.width(16.dp))
        Column { Text(title, style = TherapyType.emphasisLarge, color = TherapyColors.ink); Text(subtitle, style = TherapyType.body, color = TherapyColors.muted) }
    }
}

@Composable
fun LoginScreen(onSignedIn: () -> Unit, onAcceptInvite: () -> Unit, onRegister: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var role by rememberSaveable { mutableStateOf("therapist") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var busy by remember { mutableStateOf(false) }

    HeroSheet(stringResource(R.string.login_tagline)) {
        Overline(stringResource(R.string.signing_in_as))
        Segmented(listOf("therapist" to stringResource(R.string.role_therapist), "patient" to stringResource(R.string.role_patient)), role, onSelect = { role = it })
        LabelledField(stringResource(R.string.email), email, { email = it }, placeholder = "your@email.com", keyboard = KeyboardType.Email)
        LabelledField(stringResource(R.string.password), password, { password = it }, placeholder = "••••••••", password = true, keyboard = KeyboardType.Password)
        error?.let { ErrorText(errorText(it)) }
        PrimaryButton(stringResource(R.string.sign_in), enabled = email.isNotBlank() && password.isNotBlank(), loading = busy, style = ButtonStyle.Canvas, icon = Icons.AutoMirrored.Filled.ArrowForward) {
            busy = true; error = null
            scope.launch { try { api.login(email.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false } }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onAcceptInvite) { Text(stringResource(R.string.have_invite), style = TherapyType.emphasis, color = TherapyColors.navy) }
            TextButton(onRegister) { Text(stringResource(R.string.im_a_therapist), style = TherapyType.emphasis, color = TherapyColors.navy) }
        }
    }
}

@Composable
fun AcceptInviteScreen(onSignedIn: () -> Unit, onBack: () -> Unit) {
    val api = LocalContainer.current.api
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var inviteeName by rememberSaveable { mutableStateOf<String?>(null) }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var busy by remember { mutableStateOf(false) }

    HeroSheet(stringResource(R.string.accept_invite_title)) {
        LabelledField(stringResource(R.string.invite_code), code, { code = it; inviteeName = null }, placeholder = "XXXX-XXXX-XXXX")
        if (inviteeName == null) {
            error?.let { ErrorText(errorText(it)) }
            PrimaryButton(stringResource(R.string.check_code), enabled = code.isNotBlank(), loading = busy) {
                busy = true; error = null
                scope.launch {
                    try {
                        // Looked up first so the client sees their own name before typing a password.
                        val invite = api.lookUpInvite(code.trim())
                        if (invite.valid) inviteeName = invite.name else error = ApiError.Server(400, context.getString(R.string.error_generic))
                    } catch (e: Exception) { error = e } finally { busy = false }
                }
            }
        } else {
            Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Text(stringResource(R.string.invite_for, inviteeName!!), style = TherapyType.emphasisLarge) }
            LabelledField(stringResource(R.string.choose_password), password, { password = it }, password = true, keyboard = KeyboardType.Password)
            error?.let { ErrorText(errorText(it)) }
            PrimaryButton(stringResource(R.string.activate), enabled = password.length >= 12, loading = busy, icon = Icons.AutoMirrored.Filled.ArrowForward) {
                busy = true; error = null
                scope.launch { try { api.acceptInvite(code.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false } }
            }
        }
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.back), style = TherapyType.emphasis, color = TherapyColors.navy) }
    }
}

@Composable
fun RegisterScreen(onSignedIn: () -> Unit, onBack: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var busy by remember { mutableStateOf(false) }

    HeroSheet(stringResource(R.string.register_title)) {
        Muted(stringResource(R.string.im_a_therapist))
        LabelledField(stringResource(R.string.name), name, { name = it })
        LabelledField(stringResource(R.string.email), email, { email = it }, placeholder = "your@email.com", keyboard = KeyboardType.Email)
        LabelledField(stringResource(R.string.password), password, { password = it }, password = true, keyboard = KeyboardType.Password)
        error?.let { ErrorText(errorText(it)) }
        PrimaryButton(stringResource(R.string.create_account), enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 12, loading = busy) {
            busy = true; error = null
            scope.launch { try { api.register(name.trim(), email.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false } }
        }
        TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.back), style = TherapyType.emphasis, color = TherapyColors.navy) }
    }
}
