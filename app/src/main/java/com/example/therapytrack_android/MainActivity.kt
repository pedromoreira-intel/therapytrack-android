package com.example.therapytrack_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.therapytrack_android.ui.navigation.Screen
import com.example.therapytrack_android.ui.navigation.therapistBottomNavItems
import com.example.therapytrack_android.ui.screens.*
import com.example.therapytrack_android.ui.theme.Therapytrack_androidTheme
import com.example.therapytrack_android.ui.viewmodel.TherapyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val viewModel = TherapyViewModel()
        
        setContent {
            Therapytrack_androidTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: TherapyViewModel) {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                therapistBottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (currentDestination?.hierarchy?.any { it.route == item.screen.route } == true) 
                                    item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                    onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                    onNavigateToNetworking = { navController.navigate(Screen.Networking.route) },
                    onNavigateToTraining = { navController.navigate(Screen.Training.route) }
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(viewModel)
            }
            composable(Screen.Networking.route) {
                NetworkingScreen(viewModel)
            }
            composable(Screen.Training.route) {
                TrainingScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.Patients.route) {
                PatientsScreen(viewModel)
            }
            composable(Screen.Messages.route) {
                MessagesScreen(viewModel)
            }
        }
    }
}
