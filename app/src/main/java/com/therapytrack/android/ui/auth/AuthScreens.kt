package com.therapytrack.android.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.ApiError
import com.therapytrack.android.ui.common.Card
import com.therapytrack.android.ui.common.ErrorText
import com.therapytrack.android.ui.common.Muted
import com.therapytrack.android.ui.common.PrimaryButton
import com.therapytrack.android.ui.theme.TherapyColors
import kotlinx.coroutines.launch

@Composable
private fun AuthFrame(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("TherapyTrack", style = MaterialTheme.typography.labelLarge, color = TherapyColors.muted)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TherapyColors.navy)
        content()
    }
}

/** Something the server said in a form a person can act on. */
@Composable
fun errorText(e: Throwable): String = when (e) {
    is ApiError.Network -> stringResource(R.string.offline_login)
    is ApiError.Unauthorized -> stringResource(R.string.login_failed)
    is ApiError.Server -> e.message ?: stringResource(R.string.error_generic)
    else -> stringResource(R.string.error_generic)
}

@Composable
fun ConsentScreen(onConsentGiven: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var terms by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }

    AuthFrame(stringResource(R.string.consent_title)) {
        Card { Text(stringResource(R.string.consent_body), style = MaterialTheme.typography.bodyLarge) }
        // The whole row toggles, not only the 20dp box: the text is what people tap.
        Row(Modifier.fillMaxWidth().clickable { terms = !terms }, verticalAlignment = Alignment.CenterVertically) { Checkbox(terms, { terms = it }); Text(stringResource(R.string.consent_terms)) }
        Row(Modifier.fillMaxWidth().clickable { privacy = !privacy }, verticalAlignment = Alignment.CenterVertically) { Checkbox(privacy, { privacy = it }); Text(stringResource(R.string.consent_privacy)) }
        PrimaryButton(stringResource(R.string.continue_), enabled = terms && privacy) {
            // Best effort and not blocking: a network failure must not trap
            // someone here. The state is visible again under Profile → Your data.
            scope.launch { runCatching { api.recordConsent("clinical_data", true) } }
            onConsentGiven()
        }
    }
}

@Composable
fun LoginScreen(onSignedIn: () -> Unit, onAcceptInvite: () -> Unit, onRegister: () -> Unit) {
    val api = LocalContainer.current.api
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var busy by remember { mutableStateOf(false) }

    AuthFrame(stringResource(R.string.sign_in)) {
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        error?.let { ErrorText(errorText(it)) }
        PrimaryButton(stringResource(R.string.sign_in), enabled = email.isNotBlank() && password.isNotBlank(), loading = busy) {
            busy = true; error = null
            scope.launch {
                try { api.login(email.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false }
            }
        }
        TextButton(onAcceptInvite) { Text(stringResource(R.string.have_invite)) }
        TextButton(onRegister) { Text(stringResource(R.string.im_a_therapist)) }
    }
}

@Composable
fun AcceptInviteScreen(onSignedIn: () -> Unit, onBack: () -> Unit) {
    val api = LocalContainer.current.api
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var inviteeName by rememberSaveable { mutableStateOf<String?>(null) }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var busy by remember { mutableStateOf(false) }

    AuthFrame(stringResource(R.string.accept_invite_title)) {
        OutlinedTextField(code, { code = it; inviteeName = null }, label = { Text(stringResource(R.string.invite_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
            Card(tint = TherapyColors.champagne.copy(alpha = 0.3f)) { Text(stringResource(R.string.invite_for, inviteeName!!), style = MaterialTheme.typography.titleMedium) }
            OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.choose_password)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            error?.let { ErrorText(errorText(it)) }
            PrimaryButton(stringResource(R.string.activate), enabled = password.length >= 8, loading = busy) {
                busy = true; error = null
                scope.launch {
                    try { api.acceptInvite(code.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false }
                }
            }
        }
        TextButton(onBack) { Text(stringResource(R.string.back)) }
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

    AuthFrame(stringResource(R.string.register_title)) {
        Muted(stringResource(R.string.im_a_therapist))
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        error?.let { ErrorText(errorText(it)) }
        PrimaryButton(stringResource(R.string.create_account), enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 8, loading = busy) {
            busy = true; error = null
            scope.launch {
                try { api.register(name.trim(), email.trim(), password); onSignedIn() } catch (e: Exception) { error = e } finally { busy = false }
            }
        }
        TextButton(onBack) { Text(stringResource(R.string.back)) }
    }
}
