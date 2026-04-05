package com.quizedguy.genghealth.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.quizedguy.genghealth.ui.navigation.Screen
import com.quizedguy.genghealth.ui.navigation.bottomNavItems
import com.quizedguy.genghealth.ui.screens.*
import com.quizedguy.genghealth.ui.viewmodel.AuthViewModel
import com.quizedguy.genghealth.util.CompatibilityUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import com.quizedguy.genghealth.util.RewardedAdManager
import android.app.Activity

@Composable
fun MainComposeApp() {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val context = LocalContext.current
    
    val isAdLoaded by RewardedAdManager.isAdLoaded.collectAsState()
    
    LaunchedEffect(currentUser) {
        if (currentUser != null && context is Activity) {
            RewardedAdManager.loadAd(context)
        }
    }
    
    var showCompatibilityAlert by remember { 
        mutableStateOf(!CompatibilityUtils.isGooglePlayServicesAvailable(context)) 
    }
    
    if (showCompatibilityAlert) {
        AlertDialog(
            onDismissRequest = { showCompatibilityAlert = false },
            title = { Text("Device Compatibility") },
            text = { 
                Text("GenGhealth noticed that Google Play Services is missing. Features like Real-time Sync and Ads may be limited on this device.") 
            },
            confirmButton = {
                Button(onClick = { showCompatibilityAlert = false }) {
                    Text("I Understand")
                }
            }
        )
    }
    
    // If logged out, show Login screen directly. This is the most reliable way to handle session changes.
    if (currentUser == null) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { 
                // AuthViewModel.currentUser will update and swap the UI
            }
        )
    } else {
        // Only initialize NavController when logged in to avoid cross-session state issues
        val navController = rememberNavController()
        
        Scaffold(
            bottomBar = {
                val currentBottomItems = remember(isAdmin) {
                    if (isAdmin) bottomNavItems + Screen.Admin else bottomNavItems
                }
                
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    currentBottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (isAdLoaded) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (context is Activity) {
                                RewardedAdManager.showAd(context) {
                                    // Reward claimed
                                }
                            }
                        },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        text = { Text("Watch") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { 
                    DashboardScreen(navController = navController) 
                }
                composable(Screen.ScreenTime.route) { ScreenTimeScreen() }
                composable(Screen.Rewards.route) { RewardsScreen(navController = navController) }
                composable(Screen.Points.route) { PointsScreen() }
                composable(Screen.Collection.route) { PointCollectionScreen(navController = navController) }
                composable(Screen.Profile.route) { 
                    ProfileScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    ) 
                }
                composable(Screen.Admin.route) { 
                    AdminDashboardScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    ) 
                }
            }
        }
    }
}
