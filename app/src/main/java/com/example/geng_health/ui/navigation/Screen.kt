package com.example.geng_health.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object ScreenTime : Screen("screen_time", "Usage", Icons.Default.List)
    object Rewards : Screen("rewards", "Rewards", Icons.Default.Star)
    object Points : Screen("points", "Points", Icons.Default.CheckCircle)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Admin : Screen("admin", "Admin", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Rewards,
    Screen.Profile
)
