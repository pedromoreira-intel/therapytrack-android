package com.therapytrack.android.ui.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.R
import com.therapytrack.android.core.Api
import com.therapytrack.android.core.ApiPatient
import com.therapytrack.android.ui.theme.TherapyColors

private data class Tab(val route: String, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", R.string.tab_home, Icons.Outlined.Home),
    Tab("journal", R.string.tab_journal, Icons.Outlined.MenuBook),
    Tab("assessments", R.string.tab_assessments, Icons.Outlined.Assignment),
    Tab("messages", R.string.tab_messages, Icons.Outlined.ChatBubbleOutline),
    Tab("profile", R.string.tab_profile, Icons.Outlined.Person)
)

/** What every client screen needs about the person: their patient record, loaded once and shared. */
class ClientState {
    var patient by mutableStateOf<ApiPatient?>(null)
    var loadError by mutableStateOf<Throwable?>(null)
}

@Composable
fun ClientShell(onSignedOut: () -> Unit) {
    val container = LocalContainer.current
    val nav = rememberNavController()
    val state = remember { ClientState() }
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(reloadTick) {
        try {
            state.patient = container.api.myPatientRecord()
            state.loadError = null
        } catch (e: Exception) {
            state.loadError = e
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.route }

    Scaffold(
        containerColor = TherapyColors.canvas,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = TherapyColors.pearl) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { nav.navigate(tab.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(stringResource(tab.label), maxLines = 1, softWrap = false, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TherapyColors.navy, indicatorColor = TherapyColors.champagne.copy(alpha = 0.5f))
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(state, onCheckIn = { nav.navigate("checkin") }, onCrisis = { nav.navigate("crisis") }, onReload = { reloadTick++ }) }
                composable("checkin") { CheckInScreen(state, onDone = { nav.popBackStack() }) }
                composable("journal") { JournalScreen() }
                composable("assessments") { AssessmentsScreen(state, onStart = { nav.navigate("assessment/${it.name}") }) }
                composable("assessment/{instrument}") { entry ->
                    val instrument = Api.Instrument.valueOf(entry.arguments?.getString("instrument") ?: "PHQ9")
                    QuestionnaireScreen(state, instrument, onDone = { nav.popBackStack() }, onCrisis = { nav.navigate("crisis") })
                }
                composable("crisis") { CrisisScreen(onBack = { nav.popBackStack() }) }
                composable("messages") { MessagesScreen(state) }
                composable("profile") { ProfileScreen(state, onPrivacy = { nav.navigate("privacy") }, onCrisis = { nav.navigate("crisis") }, onSignedOut = onSignedOut) }
                composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }, onErased = onSignedOut) }
            }
        }
    }
}

