package com.quizedguy.genghealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.genghealth.ui.viewmodel.PointsViewModel
import com.quizedguy.genghealth.ui.components.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRewardsHistoryScreen(
    navController: NavController,
    pointsViewModel: PointsViewModel = viewModel()
) {
    val withdrawalHistory by pointsViewModel.withdrawalHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Rewards") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History & Codes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All your claimed gift cards in one place",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (withdrawalHistory.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No rewards requested yet.", 
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(withdrawalHistory) { request ->
                        IssuedRewardCard(request)
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
            
            BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
fun IssuedRewardCard(request: com.quizedguy.genghealth.ui.viewmodel.WithdrawalRequest) {
    val isApproved = request.status == "Approved"
    
    val cardGradient = if (isApproved) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
        )
    }
    
    val iconColor = if (isApproved) Color(0xFF2E7D32) else Color(0xFFEF6C00)
    val rewardDisplayName = if (request.rewardName.isNotEmpty()) request.rewardName else "Reward"
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(request.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.background(cardGradient)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "$${request.amountRs} $rewardDisplayName", 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 18.sp,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateStr, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Color(0xFF49454F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                if (isApproved && !request.giftCardCode.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "YOUR GIFT CARD CODE", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.Gray,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = request.giftCardCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else if (request.status == "Pending") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "We are reviewing your request. Your code will appear here shortly.", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color(0xFF5D4037),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
