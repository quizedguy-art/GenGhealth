package com.quizedguy.genghealth.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.genghealth.shared.ui.viewmodel.AdminViewModel
import com.quizedguy.genghealth.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.genghealth.shared.ui.viewmodel.WithdrawalRequest
import com.quizedguy.genghealth.shared.ui.components.BannerAdView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsState()
    val withdrawalHistory by viewModel.withdrawalHistory.collectAsState()
    val usageRecords by viewModel.usageRecords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApproveDialog by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var giftCardCode by remember { mutableStateOf("") }
    
    var showCreditDialog by remember { mutableStateOf<com.quizedguy.genghealth.ui.viewmodel.DailyUsageRecord?>(null) }
    var pointsToCredit by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                val context = LocalContext.current
                IconButton(onClick = {
                    MobileAds.openAdInspector(context) { error ->
                        if (error != null) {
                            android.util.Log.e("AdminDashboard", "Ad Inspector failed to open: ${error.message}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Open Ad Inspector",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Requests") })
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Usage") })
            Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("History") })
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                when (selectedTabIndex) {
                    0 -> { // Requests
                        if (pendingWithdrawals.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No pending requests.") } }
                        } else {
                            items(pendingWithdrawals) { request ->
                                WithdrawalRequestCard(
                                    request = request,
                                    onApprove = { showApproveDialog = request },
                                    onReject = { viewModel.rejectRequest(request.id) }
                                )
                            }
                        }
                    }
                    1 -> { // Usage
                        if (usageRecords.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No usage records.") } }
                        } else {
                            items(usageRecords) { record ->
                                UsageReviewCard(
                                    record = record,
                                    onCredit = { 
                                        showCreditDialog = record
                                        pointsToCredit = record.pointsPotential.toString()
                                    }
                                )
                            }
                        }
                    }
                    2 -> { // History
                        if (withdrawalHistory.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No reward history.") } }
                        } else {
                            items(withdrawalHistory) { request ->
                                IssuedRewardCard(request) // Reusing from MyRewardsHistoryScreen
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Approval Dialog
    if (showApproveDialog != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = null },
            title = { Text("Issue Gift Card") },
            text = {
                Column {
                    Text("Enter the code for $${showApproveDialog?.amountRs} ${showApproveDialog?.rewardName ?: "reward"}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = giftCardCode,
                        onValueChange = { giftCardCode = it },
                        label = { Text("Gift Card Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveRequest(showApproveDialog!!.id, giftCardCode)
                        showApproveDialog = null
                        giftCardCode = ""
                    },
                    enabled = giftCardCode.isNotBlank()
                ) {
                    Text("Approve & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Credit Points Dialog
    if (showCreditDialog != null) {
        AlertDialog(
            onDismissRequest = { showCreditDialog = null },
            title = { Text("Credit Points") },
            text = {
                Column {
                    Text("Enter points to credit for usage on ${showCreditDialog?.date}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pointsToCredit,
                        onValueChange = { pointsToCredit = it },
                        label = { Text("Points") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val points = pointsToCredit.toIntOrNull()
                        if (points != null) {
                            viewModel.creditUsagePoints(showCreditDialog!!, points)
                            showCreditDialog = null
                            pointsToCredit = ""
                        }
                    },
                    enabled = pointsToCredit.isNotBlank() && pointsToCredit.toIntOrNull() != null
                ) {
                    Text("Credit User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to sign out of the Admin Dashboard?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WithdrawalRequestCard(
    request: WithdrawalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val rewardNameStr = if (request.rewardName.isNotEmpty()) request.rewardName else "Reward"
                    Text(text = "Amount: $${request.amountRs} ($rewardNameStr)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Points: ${request.pointsDeducted}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(request.createdAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "User UID: ${request.userId}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Text("Issue Code")
                }
            }
        }
    }
}

@Composable
fun UsageReviewCard(
    record: com.quizedguy.genghealth.ui.viewmodel.DailyUsageRecord,
    onCredit: () -> Unit
) {
    val todayStr = remember { java.time.LocalDate.now().toString() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isApproved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = record.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (record.isCollected) "Collected" else if (record.isApproved) "Approved (Uncollected)" else "Pending Review",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (record.isCollected) Color(0xFF4CAF50) else if (record.isApproved) Color(0xFF2196F3) else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(record.totalMillis)
            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(record.totalMillis) % 60
            Text(text = "Screen Time: ${hours}h ${minutes}m", style = MaterialTheme.typography.bodyMedium)
            Text(text = "User UID: ${record.userId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            if (!record.isApproved) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (record.date == todayStr) {
                        Text(
                            text = "Tracking in Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Button(onClick = onCredit) {
                            Text("Credit Points")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = if (record.isCollected) "Collected: ${record.pointsPotential} pts" else "Approved: ${record.pointsPotential} pts"
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
