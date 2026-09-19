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
    TabSpec("today", "", Icons.Outlined.Home, Icons.Filled.Home),
    TabSpec("clients", "", Icons.Outlined.Group, Icons.Filled.Group),
    TabSpec("practice", "", Icons.Outlined.School, Icons.Filled.School),
    TabSpec("profile", "", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
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
            val badges = mapOf(
                "today" to inbox.count("alert"),
                "clients" to inbox.count("message"),
                "practice" to inbox.count("referral", "referral_response", "join_request", "join_response")
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
                        is com.therapytrack.android.ui.common.Open.Referrals -> nav.navigate("referrals")
                        is com.therapytrack.android.ui.common.Open.Group -> nav.navigate("group/${open.id}")
                        is com.therapytrack.android.ui.common.Open.Patient -> nav.navigate("patient/${open.id}")
                    }
                } }
                composable("today") { TodayScreen(onActivity = { nav.navigate("activity") }, onPatient = { nav.navigate("patient/$it") }, onClients = { nav.navigate("clients") }, onNewClient = { nav.navigate("clients?new=1") }) }
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
                composable("note/{id}/{name}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    SessionNoteScreen(id, entry.arguments?.getString("name") ?: "", onDone = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") })
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
                composable("credentials") { CredentialsScreen(onBack = { nav.popBackStack() }) }
                composable("training") { TrainingScreen(onBack = { nav.popBackStack() }) }
                composable("supervision") { SupervisionScreen(onBack = { nav.popBackStack() }) }
                composable("myprofile") { MyProfileScreen(onBack = { nav.popBackStack() }) }
                composable("directory") { DirectoryScreen(onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") },
                    onRefer = { id, name -> nav.navigate("refer/$id/${android.net.Uri.encode(name)}") }) }
                composable("refer/{id}/{name}") { entry ->
                    val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    NewReferralScreen(id, entry.arguments?.getString("name") ?: "", onDone = { nav.popBackStack() })
                }
                composable("referrals") { ReferralsScreen(onBack = { nav.popBackStack() }, onSeePlan = { nav.navigate("practice") }) }
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
