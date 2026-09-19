package com.therapytrack.android.ui.therapist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.therapytrack.android.R
import com.therapytrack.android.ui.client.ConversationScreen
import com.therapytrack.android.ui.theme.TherapyColors

private data class Tab(val route: String, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("today", R.string.tab_today, Icons.Outlined.Today),
    Tab("clients", R.string.tab_clients, Icons.Outlined.Group),
    Tab("practice", R.string.tab_practice, Icons.Outlined.School),
    Tab("threads", R.string.tab_messages, Icons.Outlined.ChatBubbleOutline),
    Tab("profile", R.string.tab_profile, Icons.Outlined.Person)
)

@Composable
fun TherapistShell(onSignedOut: () -> Unit) {
    val nav = rememberNavController()
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
                        onClick = { nav.navigate(tab.route) { popUpTo("today") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(stringResource(tab.label), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TherapyColors.navy, indicatorColor = TherapyColors.champagne.copy(alpha = 0.5f))
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "today") {
                composable("today") { TodayScreen(onPatient = { nav.navigate("patient/$it") }) }
                composable("clients") { PatientsScreen(onPatient = { nav.navigate("patient/$it") }) }
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
