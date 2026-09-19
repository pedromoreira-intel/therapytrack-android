package com.therapytrack.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.therapytrack.android.AppContainer
import kotlinx.coroutines.delay

/**
 * Keep the badge counts fresh while the app is on screen: at once on
 * resume, then every [everySeconds]. Stops in the background — polling is
 * a stand-in for push, not a replacement for it, and must not drain the
 * battery of a phone in a pocket.
 */
@Composable
fun PollInbox(container: AppContainer, everySeconds: Long = 45) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { container.refreshInbox(); delay(everySeconds * 1000) }
        }
    }
}
