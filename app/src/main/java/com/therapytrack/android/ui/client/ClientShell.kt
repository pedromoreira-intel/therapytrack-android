package com.therapytrack.android.ui.client

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.therapytrack.android.ui.common.TabSpec
import com.therapytrack.android.ui.common.TherapyTabBar
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
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


private val tabs = listOf(
    TabSpec("home", "", Icons.Outlined.Home, Icons.Filled.Home),
    TabSpec("messages", "", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    TabSpec("journal", "", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    TabSpec("assessments", "", Icons.Outlined.Spa, Icons.Filled.Spa),
    TabSpec("profile", "", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
)
private val tabLabels = listOf(R.string.tab_home, R.string.tab_messages, R.string.tab_journal, R.string.tab_wellbeing, R.string.tab_profile)

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
    // A destination with optional arguments is "clients?new={new}"; the tab is "clients".
    val route = backStack?.destination?.route?.substringBefore('?')
    val showBar = route in tabs.map { it.route }

    Scaffold(
        containerColor = TherapyColors.canvas,
        bottomBar = {
            if (showBar) TherapyTabBar(tabs.mapIndexed { i, t -> t.copy(label = stringResource(tabLabels[i])) }, route) {
                nav.navigate(it) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
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

