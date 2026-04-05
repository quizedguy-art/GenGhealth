package com.quizedguy.genghealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizedguy.genghealth.ui.viewmodel.PointsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quizedguy.genghealth.ui.components.BannerAdView
import androidx.navigation.NavController
import com.quizedguy.genghealth.ui.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List

data class RewardItem(
    val title: String,
    val description: String,
    val points: Int,
    val amountRs: Int
)

@Composable
fun RewardsScreen(
    navController: NavController,
    pointsViewModel: PointsViewModel = viewModel()
) {
    val userPoints by pointsViewModel.userPoints.collectAsState()
    val dailyUsage by pointsViewModel.dailyUsageHistory.collectAsState()
    val context = LocalContext.current
    
    // Check if there are any uncollected points
    val hasUncollectedPoints = dailyUsage.any { !it.isCollected && it.pointsPotential > 0 }
    
    val rewards = listOf(
        RewardItem("Amazon Gift Card", "₹100 Voucher", 1000, 100),
        RewardItem("Google Play Credits", "₹100 credits", 1000, 100),
        RewardItem("Amazon Gift Card", "₹500 Voucher", 5000, 500),
        RewardItem("Google Play Credits", "₹500 credits", 5000, 500)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Redeem Rewards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Turn your screen time into real value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            BalanceCard(points = userPoints)
            Spacer(modifier = Modifier.height(16.dp))
            
            // New Collection Card
            CollectionGatewayCard(
                hasPoints = hasUncollectedPoints,
                onClick = { navController.navigate(Screen.Collection.route) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Available Rewards",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(rewards) { reward ->
            RewardCard(
                reward = reward, 
                canAfford = userPoints >= reward.points,
                onClaim = { pointsViewModel.requestWithdrawal(reward.amountRs) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            BannerAdView(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
fun BalanceCard(points: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "Your Balance", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$points Points",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RewardCard(
    reward: RewardItem,
    canAfford: Boolean,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reward.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = reward.description, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reward.points} Points",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onClaim,
                enabled = canAfford,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Claim")
            }
        }
    }
}

@Composable
fun CollectionGatewayCard(hasPoints: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPoints) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPoints) "✨ Points Waiting!" else "Daily Usage History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasPoints) "Collect your screen time rewards now" else "View your daily progress",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = if (hasPoints) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
