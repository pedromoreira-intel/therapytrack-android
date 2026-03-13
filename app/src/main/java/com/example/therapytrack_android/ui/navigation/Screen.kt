package com.example.therapytrack_android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Schedule : Screen("schedule")
    object Networking : Screen("networking")
    object Supervision : Screen("supervision")
    object Intervision : Screen("intervision")
    object Training : Screen("training")
    object Profile : Screen("profile")
    object Patients : Screen("patients")
    object Messages : Screen("messages")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// Therapist tabs: Home, Networking (segmented), Training (segmented), Profile
val therapistBottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Filled.Home),
    BottomNavItem(Screen.Networking, "Networking", Icons.Filled.People, Icons.Filled.People),
    BottomNavItem(Screen.Training, "Training", Icons.Filled.School, Icons.Filled.School),
    BottomNavItem(Screen.Profile, "Profile", Icons.Filled.Person, Icons.Filled.Person)
)

// Legacy - keep for compatibility
val bottomNavItems = therapistBottomNavItems
