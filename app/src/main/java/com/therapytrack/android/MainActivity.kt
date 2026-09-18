package com.therapytrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.therapytrack.android.core.Session
import com.therapytrack.android.ui.AppNav
import com.therapytrack.android.ui.theme.TherapyTrackTheme
import kotlinx.coroutines.runBlocking

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("no container") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TherapyTrackApp).container

        // Debug builds accept a session by intent extra, so a smoke run needs no
        // typed credentials. Compiled out of release entirely.
        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("debug_token")?.let { token ->
                runBlocking {
                    container.client.storeSession(Session(
                        accessToken = token,
                        refreshToken = intent.getStringExtra("debug_refresh"),
                        userId = intent.getIntExtra("debug_user_id", 0).takeIf { it > 0 },
                        role = intent.getStringExtra("debug_role") ?: "patient"))
                }
            }
        }

        setContent {
            CompositionLocalProvider(LocalContainer provides container) {
                TherapyTrackTheme { AppNav() }
            }
        }
    }
}
