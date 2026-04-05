package com.quizedguy.genghealth.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quizedguy.genghealth.R
import com.quizedguy.genghealth.ui.components.BannerAdView
import com.quizedguy.genghealth.ui.viewmodel.DashboardViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.quizedguy.genghealth.ui.navigation.Screen
import com.quizedguy.genghealth.ui.viewmodel.PointsViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    pointsViewModel: PointsViewModel = viewModel()
) {
    val screenTime by viewModel.screenTimeMillis.collectAsState()
    val hasUsagePermission by viewModel.hasPermission.collectAsState()
    val userPoints by pointsViewModel.userPoints.collectAsState()
    val context = LocalContext.current
    
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

    LaunchedEffect(Unit) {
        viewModel.checkPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!hasUsagePermission || !hasNotificationPermission) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Creative Header with Points
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "GenGhealth Home",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Points Card
            item {
                PointsHeader(points = userPoints, onRedeemClick = { 
                    navController.navigate(Screen.Rewards.route)
                })
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                UsageTimer(time = viewModel.formatTime(screenTime))
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                RewardGoalsCard(currentMillis = screenTime)
            }
            
            item {
                BannerAdView(modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }
}

@Composable
fun PointsHeader(points: Int, onRedeemClick: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Your Balance", color = Color.White.copy(alpha = 0.8f))
                Text(
                    text = "$points Pts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            
            Button(
                onClick = onRedeemClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Redeem", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UsageTimer(time: String) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.sweepGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    ),
                    shape = CircleShape
                )
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Usage Today", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RewardGoalsCard(currentMillis: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Today's Potential", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            RewardGoalItem("Healthy Choice", "Under 5h", "200 Pts", currentMillis < 5 * 3600000)
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
            RewardGoalItem("Balanced Life", "Under 6h", "100 Pts", currentMillis < 6 * 3600000)
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
            RewardGoalItem("Moderate User", "Under 7h", "50 Pts", currentMillis < 7 * 3600000)
        }
    }
}

@Composable
fun RewardGoalItem(title: String, desc: String, points: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = points,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun PermissionRequiredScreen(
    hasUsage: Boolean,
    hasNotification: Boolean,
    onRequestUsage: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GenGhealth needs usage and notification access to track your screen time and reward your healthy habits.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!hasUsage) {
            Button(onClick = onRequestUsage, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Grant Usage Access")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!hasNotification) {
            Button(onClick = onRequestNotification, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Grant Notification Access")
            }
        }
    }
}
