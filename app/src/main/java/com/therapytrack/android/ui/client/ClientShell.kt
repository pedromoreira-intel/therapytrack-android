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
import androidx.compose.runtime.collectAsState
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.ui.common.PollInbox
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


// Outlines selected or not, as on iOS: the colour says which tab you are on.
// Bem-estar holds the exercises, as on iOS; the questionnaires moved to a
// link on Início.
private val tabs = listOf(
    TabSpec("home", "", Icons.Outlined.Home),
    TabSpec("messages", "", Icons.Outlined.ChatBubbleOutline),
    TabSpec("journal", "", Icons.Outlined.MenuBook),
    TabSpec("wellness", "", Icons.Outlined.Spa),
    TabSpec("profile", "", Icons.Outlined.AccountCircle)
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

    val inbox by container.inbox.collectAsState()
    PollInbox(container)
    val backStack by nav.currentBackStackEntryAsState()
    // A destination with optional arguments is "clients?new={new}"; the tab is "clients".
    val route = backStack?.destination?.route?.substringBefore('?')
    val showBar = route in tabs.map { it.route }

    Scaffold(
        containerColor = TherapyColors.canvas,
        bottomBar = {
            val badges = mapOf("messages" to inbox.count("message"))
            if (showBar) TherapyTabBar(tabs.mapIndexed { i, t -> t.copy(label = stringResource(tabLabels[i]), badge = badges[t.route] ?: 0) }, route) {
                nav.navigate(it) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            NavHost(nav, startDestination = "home") {
                composable("activity") { com.therapytrack.android.ui.common.ActivityScreen(onBack = { nav.popBackStack() }) { open ->
                    if (open is com.therapytrack.android.ui.common.Open.Thread) nav.navigate("messages")
                } }
                composable("home") {
                    HomeScreen(state, onActivity = { nav.navigate("activity") }, onCheckIn = { nav.navigate("checkin") }, onCrisis = { nav.navigate("crisis") },
                        onReload = { reloadTick++ }, onTopics = { nav.navigate("topics") }, onBreathing = { nav.navigate("breathing") },
                        onMyCheckIns = { nav.navigate("mycheckins") }, onQuestionnaires = { nav.navigate("assessments") })
                }
                composable("mycheckins") { MyCheckInsScreen(onBack = { nav.popBackStack() }) }
                composable("wellness") {
                    WellnessScreen(onBreathing = { nav.navigate("breathing") }, onMeditation = { nav.navigate("meditation") },
                        onGrounding = { nav.navigate("grounding") }, onCrisis = { nav.navigate("crisis") })
                }
                composable("breathing") { BreathingScreen(onClose = { nav.popBackStack() }) }
                composable("meditation") { MeditationScreen(onClose = { nav.popBackStack() }) }
                composable("grounding") { GroundingScreen(onBack = { nav.popBackStack() }, onSenses = { nav.navigate("senses") }, onBodyScan = { nav.navigate("bodyscan") }) }
                composable("senses") { SensesScreen(onClose = { nav.popBackStack() }) }
                composable("bodyscan") { BodyScanScreen(onClose = { nav.popBackStack() }) }
                composable("topics") { SessionTopicsScreen(onBack = { nav.popBackStack() }) }
                composable("checkin") { CheckInScreen(state, onDone = { nav.popBackStack() }) }
                composable("journal") { JournalScreen(onCrisis = { nav.navigate("crisis") }) }
                composable("assessments") { AssessmentsScreen(state, onBack = { nav.popBackStack() }, onStart = { nav.navigate("assessment/${it.name}") }) }
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

