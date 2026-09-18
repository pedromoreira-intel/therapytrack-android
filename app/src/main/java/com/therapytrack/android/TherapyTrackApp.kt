package com.therapytrack.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.therapytrack.android.core.Reachability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TherapyTrackApp : Application() {
    lateinit var container: AppContainer
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Drain on foreground and on the network coming back — the two moments
        // work queued offline is most likely to get through.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { scope.launch { container.drainQueues() } }
        })
        scope.launch { Reachability.online(this@TherapyTrackApp).filter { it }.collect { container.drainQueues() } }
    }
}
