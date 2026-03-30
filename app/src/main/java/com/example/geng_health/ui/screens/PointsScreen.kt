package com.example.geng_health.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geng_health.ui.viewmodel.PointsViewModel
import com.example.geng_health.ui.viewmodel.WithdrawalRequest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PointsScreen(viewModel: PointsViewModel = viewModel()) {
    val points by viewModel.userPoints.collectAsState()
    val history by viewModel.withdrawalHistory.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(text = "My Points", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$points Points", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Equivalent to ₹${points / 10}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Text(text = "Points History", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (history.isEmpty()) {
            item {
                Text(text = "No history yet. Reduce screen time to earn points!", color = Color.Gray)
            }
        } else {
            items(history) { request ->
                WithdrawalHistoryItem(request)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun WithdrawalHistoryItem(request: WithdrawalRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Withdrawal ₹${request.amountRs}", fontWeight = FontWeight.Bold)
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(request.createdAt))
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = request.status,
                color = when (request.status) {
                    "Pending" -> Color(0xFFFFA000)
                    "Approved" -> MaterialTheme.colorScheme.primary
                    else -> Color.Red
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}
