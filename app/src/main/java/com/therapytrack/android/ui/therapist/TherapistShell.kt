package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.therapytrack.android.ui.common.TabSpec
import com.therapytrack.android.ui.common.TherapyTabBar
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.therapytrack.android.LocalContainer
import com.therapytrack.android.ui.common.PollInbox
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.therapytrack.android.R
import com.therapytrack.android.ui.client.ConversationScreen
import com.therapytrack.android.ui.theme.TherapyColors

private val tabs = listOf(
    TabSpec("today", "", Icons.Outlined.Home),
    TabSpec("clients", "", Icons.Outlined.Group),
    TabSpec("practice", "", Icons.Outlined.School),
    TabSpec("profile", "", Icons.Outlined.AccountCircle)
)
private val tabLabels = listOf(R.string.tab_today, R.string.tab_clients, R.string.tab_practice, R.string.tab_profile)

@Composable
fun TherapistShell(onSignedOut: () -> Unit) {
    val nav = rememberNavController()
    val container = LocalContainer.current
    val inbox by container.inbox.collectAsState()
    PollInbox(container)
    val backStack by nav.currentBackStackEntryAsState()
    // A destination with optional arguments is "clients?new={new}"; the tab is "clients".
    val route = backStack?.destination?.route?.substringBefore('?')
    val showBar = route in tabs.map { it.route }

    Scaffold(
        containerColor = TherapyColors.canvas,
        bottomBar = {
            // Badges only for what asks for a reply, as on iOS: messages on Hoje,
            // and requests on Prática. New client records show as a dot on the
            // client's row instead of a count on the tab.
            val badges = mapOf(
                "today" to inbox.count("message"),
                "practice" to inbox.count("referral", "join_request", "supervision_request")
            )
            if (showBar) TherapyTabBar(tabs.mapIndexed { i, t -> t.copy(label = stringResource(tabLabels[i]), badge = badges[t.route] ?: 0) }, route) {
                nav.navigate(it) { popUpTo("today") { saveState = true }; launchSingleTop = true; restoreState = true }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            NavHost(nav, startDestination = "today") {
                composable("activity") { com.therapytrack.android.ui.common.ActivityScreen(onBack = { nav.popBackStack() }) { open ->
                    when (open) {
                        is com.therapytrack.android.ui.common.Open.Thread -> nav.navigate("conversation/${open.userId}/${android.net.Uri.encode(open.name)}")
                        is com.therapytrack.android.ui.common.Open.Referral -> nav.navigate("referrals?direction=${open.direction}&focus=${open.id ?: ""}")
                        is com.therapytrack.android.ui.common.Open.Group -> nav.navigate("group/${open.id}")
                        is com.therapytrack.android.ui.common.Open.Patient -> nav.navigate("patient/${open.id}")
                        is com.therapytrack.android.ui.common.Open.Supervision -> nav.navigate(if (open.asSupervisor) "supervision/offer" else "supervision/find")
                    }
                } }
                composable("today") { TodayScreen(onActivity = { nav.navigate("activity") }, onPatient = { nav.navigate("patient/$it") }, onSession = { nav.navigate(sessionRoute(it, it.hasEnded())) }, onAgenda = { nav.navigate("agenda") }, onClients = { nav.navigate("clients") }, onNewClient = { nav.navigate("clients?new=1") }, onMessages = { nav.navigate("threads") }) }
                composable("clients?new={new}") { entry -> PatientsScreen(onPatient = { nav.navigate("patient/$it") }, startCreating = entry.arguments?.getString("new") == "1") }
                composable("patient/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    PatientDetailScreen(id,
                        onBack = { nav.popBackStack() },
                        onNewNote = { name -> nav.navigate("note/$id/${android.net.Uri.encode(name)}") },
                        onSchedule = { nav.navigate("schedule/$id") },
                        onMessage = { userId, name -> nav.navigate("conversation/$userId/${android.net.Uri.encode(name)}") },
                        onAiTools = { name -> nav.navigate("ai/$id/${android.net.Uri.encode(name)}") })
                }
                // A note written from the record, outside a scheduled session: today, now.
                composable("note/{id}/{name}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    val at = remember { java.time.Instant.now() }
                    NoteEditorScreen(id, entry.arguments?.getString("name") ?: "", at, null, onClose = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") })
                }
                composable("session/{id}?name={name}&at={at}&dur={dur}&sid={sid}&note={note}") { entry ->
                    val a = entry.arguments
                    val id = a?.getString("id")?.toIntOrNull() ?: return@composable
                    SessionScreen(id, a.getString("name") ?: "", a.getString("at")?.toLongOrNull()?.let { java.time.Instant.ofEpochMilli(it) },
                        a.getString("dur")?.toIntOrNull(), a.getString("sid")?.toIntOrNull(), startOnNote = a.getString("note") == "1",
                        onBack = { nav.popBackStack() }, onHistory = { nav.navigate("patient/$id") }, onSeePlan = { nav.navigate("practice") })
                }
                composable("agenda") {
                    AgendaScreen(onBack = { nav.popBackStack() }, onSession = { s, ended -> nav.navigate(sessionRoute(s, ended)) },
                        onNew = { day -> nav.navigate("schedule-new?day=$day") })
                }
                composable("schedule-new?day={day}") { entry ->
                    ScheduleSessionScreen(null, onDone = { nav.popBackStack() }, day = entry.arguments?.getString("day"))
                }
                composable("schedule/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    ScheduleSessionScreen(id, onDone = { nav.popBackStack() })
                }
                composable("threads") { ThreadsScreen(onThread = { userId, name -> nav.navigate("conversation/$userId/${android.net.Uri.encode(name)}") }) }
                composable("conversation/{userId}/{name}") { entry ->
                    ConversationScreen(entry.arguments?.getString("userId")?.toIntOrNull(), entry.arguments?.getString("name") ?: "", onBack = { nav.popBackStack() })
                }
                composable("profile") { TherapistProfileScreen(onSignedOut = onSignedOut) }
                composable("ai/{id}/{name}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    AiToolsScreen(id, entry.arguments?.getString("name") ?: "", onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") })
                }
                composable("community") { CommunityScreen(onBack = { nav.popBackStack() }, onPost = { nav.navigate("post/$it") }) }
                composable("post/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    PostScreen(id, onBack = { nav.popBackStack() })
                }

                // Practice: plan, records, and the network features.
                composable("practice") { PracticeScreen(onOpen = { nav.navigate(it) }) }
                composable("library") { LibraryScreen(onBack = { nav.popBackStack() }) }
                composable("research") { ResearchScreen(onBack = { nav.popBackStack() }) }
                composable("conferences") { ConferencesScreen(onBack = { nav.popBackStack() }) }
                composable("phq9-scoring") { Phq9ScoringScreen(onBack = { nav.popBackStack() }) }
                composable("crisis-lines") { CrisisLinesScreen(onBack = { nav.popBackStack() }) }
                composable("credentials") { CredentialsScreen(onBack = { nav.popBackStack() }) }
                composable("training") { TrainingScreen(onBack = { nav.popBackStack() }) }
                composable("supervision") { SupervisionScreen(onBack = { nav.popBackStack() },
                    onFind = { nav.navigate("supervision/find") }, onOffer = { nav.navigate("supervision/offer") }) }
                composable("supervision/find") { FindSupervisionScreen(onBack = { nav.popBackStack() }) }
                composable("supervision/offer") { OfferSupervisionScreen(onBack = { nav.popBackStack() }) }
                composable("myprofile") { MyProfileScreen(onBack = { nav.popBackStack() }) }
                composable("directory") { DirectoryScreen(onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") },
                    onRefer = { id, name -> nav.navigate("refer/$id/${android.net.Uri.encode(name)}") }) }
                composable("refer/{id}/{name}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    NewReferralScreen(id, entry.arguments?.getString("name") ?: "", onDone = { nav.popBackStack() })
                }
                composable("referrals?direction={direction}&focus={focus}") { entry ->
                    ReferralsScreen(onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") },
                        initialDirection = entry.arguments?.getString("direction") ?: "received",
                        focusId = entry.arguments?.getString("focus")?.toIntOrNull())
                }
                composable("intervision") { IntervisionScreen(onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") }, onGroup = { nav.navigate("group/$it") }) }
                composable("group/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    GroupScreen(id, onBack = { nav.popBackStack() }, onDiscussion = { did, title -> nav.navigate("discussion/$did/${android.net.Uri.encode(title)}") })
                }
                composable("discussion/{id}/{title}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    DiscussionScreen(id, entry.arguments?.getString("title") ?: "", onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

/** The session screen for a scheduled session: its client, time, length and id. */
fun sessionRoute(s: com.therapytrack.android.core.ApiSession, note: Boolean = false): String {
    val at = com.therapytrack.android.core.ApiTimestamp.parse(s.sessionDate)?.toEpochMilli() ?: ""
    return "session/${s.patientId}?name=${android.net.Uri.encode(s.patientName ?: "")}&at=$at&dur=${s.durationMinutes ?: ""}&sid=${s.id}&note=${if (note) "1" else ""}"
}

/** Ended sessions open on their note, as from Hoje on iOS. */
fun com.therapytrack.android.core.ApiSession.hasEnded(): Boolean =
    com.therapytrack.android.core.ApiTimestamp.parse(sessionDate)?.plusSeconds(60L * (durationMinutes ?: 50))?.isBefore(java.time.Instant.now()) == true
