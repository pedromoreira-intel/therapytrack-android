package com.therapytrack.android.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.ui.auth.AcceptInviteScreen
import com.therapytrack.android.ui.auth.ConsentScreen
import com.therapytrack.android.ui.auth.LoginScreen
import com.therapytrack.android.ui.auth.RegisterScreen
import com.therapytrack.android.ui.client.ClientShell

private enum class Gate { CONSENT, LOGIN, ACCEPT_INVITE, REGISTER, CLIENT, THERAPIST_PLACEHOLDER }

/**
 * The outermost routing: consent once per install, then sign-in, then the
 * shell for the signed-in role. The consent flag is only about whether the
 * screen shows again; the record that matters is the server's.
 */
@Composable
fun AppNav() {
    val container = LocalContainer.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ui", Context.MODE_PRIVATE) }

    var consented by remember { mutableStateOf(prefs.getBoolean("hasConsented", false)) }
    // Follows the client's session, so a 401 that refresh cannot fix — or a
    // sign-out from anywhere — lands on the login screen without a restart.
    val session by container.client.session.collectAsState()
    val signedIn = session.isSignedIn
    var gate by remember { mutableStateOf(Gate.LOGIN) }

    val role = session.role
    val current = when {
        !consented && !signedIn -> Gate.CONSENT
        signedIn && role == "patient" -> Gate.CLIENT   // the server calls the client role "patient"
        signedIn -> Gate.THERAPIST_PLACEHOLDER
        else -> gate
    }

    when (current) {
        Gate.CONSENT -> ConsentScreen { prefs.edit().putBoolean("hasConsented", true).apply(); consented = true }
        Gate.LOGIN -> LoginScreen(
            onSignedIn = { },
            onAcceptInvite = { gate = Gate.ACCEPT_INVITE },
            onRegister = { gate = Gate.REGISTER })
        Gate.ACCEPT_INVITE -> AcceptInviteScreen(onSignedIn = { }, onBack = { gate = Gate.LOGIN })
        Gate.REGISTER -> RegisterScreen(onSignedIn = { }, onBack = { gate = Gate.LOGIN })
        Gate.CLIENT -> ClientShell(onSignedOut = { gate = Gate.LOGIN })
        Gate.THERAPIST_PLACEHOLDER -> com.therapytrack.android.ui.client.TherapistPlaceholder(onSignedOut = { gate = Gate.LOGIN })
    }
}
