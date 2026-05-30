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
import androidx.compose.material.icons.filled.Star

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
    
    val withdrawalHistory by pointsViewModel.withdrawalHistory.collectAsState()
    
    // Check if there are any uncollected points
    val hasUncollectedPoints = dailyUsage.any { !it.isCollected && it.pointsPotential > 0 }
    
    val rewards = listOf(
        RewardItem("Amazon Gift Card", "$3 Voucher", 800, 3),
        RewardItem("Amazon Gift Card", "$5 Voucher", 1200, 5),
        RewardItem("PayPal Gift Card", "$3 Transfer", 800, 3),
        RewardItem("PayPal Gift Card", "$5 Transfer", 1200, 5),
        RewardItem("Google Play Gift Card", "$3 credits", 800, 3),
        RewardItem("Google Play Gift Card", "$5 credits", 1200, 5)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
            }

            item {
                BalanceCard(points = userPoints)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CollectionGatewayCard(
                            hasPoints = hasUncollectedPoints,
                            onClick = { navController.navigate(Screen.Collection.route) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RewardHistoryGatewayCard(
                            onClick = { navController.navigate(Screen.MyRewardsHistory.route) }
                        )
                    }
                }
            }
                
            item {
                if (userPoints < 5000) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Earn at least 5,000 points to unlock rewards. You need ${5000 - userPoints} more points. (Note: We will remove this threshold soon!)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
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
                    canAfford = userPoints >= 5000 && userPoints >= reward.points,
                    onClaim = { pointsViewModel.requestWithdrawal(reward.amountRs, reward.points, reward.title) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
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

@Composable
fun RewardHistoryGatewayCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "My Rewards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "View your history",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
