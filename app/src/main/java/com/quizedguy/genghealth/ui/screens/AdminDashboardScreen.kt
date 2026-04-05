package com.quizedguy.genghealth.ui.screens

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
import com.quizedguy.genghealth.ui.viewmodel.AdminViewModel
import com.quizedguy.genghealth.ui.viewmodel.AuthViewModel
import com.quizedguy.genghealth.ui.viewmodel.WithdrawalRequest
import com.quizedguy.genghealth.ui.components.BannerAdView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApproveDialog by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var giftCardCode by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BannerAdView(modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column {
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
            }
        }
        Text(
            text = "Manage Pending Redemptions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pendingWithdrawals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No pending requests!", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
                }
                items(pendingWithdrawals) { request ->
                    WithdrawalRequestCard(
                        request = request,
                        onApprove = { showApproveDialog = request },
                        onReject = { viewModel.rejectRequest(request.id) }
                    )
                }
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
                    Text("Enter the code for ₹${showApproveDialog?.amountRs} reward.")
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
                    Text(text = "Amount: ₹${request.amountRs}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
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
