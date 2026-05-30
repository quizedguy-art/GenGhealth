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
import androidx.compose.ui.text.font.FontWeight
import com.quizedguy.genghealth.util.RewardedAdManager
import com.quizedguy.genghealth.util.RewardedInterstitialAdManager
import android.app.Activity
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.quizedguy.genghealth.ui.viewmodel.DashboardViewModel

@Composable
fun MainComposeApp() {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val context = LocalContext.current
    
    val isAdLoaded by RewardedAdManager.isAdLoaded.collectAsState()
    
    var showCompatibilityAlert by remember { 
        mutableStateOf(!CompatibilityUtils.isGooglePlayServicesAvailable(context)) 
    }

    val dashboardViewModel: DashboardViewModel = viewModel()
    val hasUsagePermission by dashboardViewModel.hasPermission.collectAsState()
    
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.checkPermission()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context, 
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            dashboardViewModel.checkPermission()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (context is Activity) {
                RewardedAdManager.loadAd(context)
                RewardedInterstitialAdManager.loadAd(context)
            }
        }
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

    if (currentUser == null) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { 
                // AuthViewModel.currentUser will update and swap the UI
            }
        )
    } else if (!hasUsagePermission || !hasNotificationPermission) {
        PermissionRequiredScreen(
            hasUsage = hasUsagePermission,
            hasNotification = hasNotificationPermission,
            onRequestUsage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRequestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
    } else {
        // Only initialize NavController when logged in to avoid cross-session state issues
        val navController = rememberNavController()
        

        // Show Rewarded Interstitial Ad when transitioning to the Rewards Screen
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        LaunchedEffect(currentRoute) {
            if (context is Activity && currentUser != null && currentRoute == Screen.Rewards.route) {
                RewardedInterstitialAdManager.showAd(context)
            }
        }

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
                    DashboardScreen(navController = navController, viewModel = dashboardViewModel) 
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
                composable(Screen.Referrals.route) {
                    ReferralScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
                composable(Screen.MyRewardsHistory.route) { MyRewardsHistoryScreen(navController = navController) }
            }
        }
    }
}
