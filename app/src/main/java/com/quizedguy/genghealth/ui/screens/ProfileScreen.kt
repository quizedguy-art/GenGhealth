package com.quizedguy.genghealth.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.quizedguy.genghealth.ui.navigation.Screen
import com.quizedguy.genghealth.ui.viewmodel.AuthViewModel
import com.quizedguy.genghealth.ui.viewmodel.PointsViewModel
import com.quizedguy.genghealth.ui.components.BannerAdView
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    pointsViewModel: PointsViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val totalEarned by pointsViewModel.totalPointsEarned.collectAsState()
    val currentBalance by pointsViewModel.userPoints.collectAsState()

    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Creative Header
        item {
            HeaderSection(name = userName ?: "User", email = currentUser?.email ?: "", gradient = gradient)
        }

        // Stats Section
        item {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Lifetime Earned",
                    value = "$totalEarned",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                StatCard(
                    title = "Current Balance",
                    value = "$currentBalance",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        // Action Menu
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Account Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                ActionItem(
                    title = "Transaction History",
                    subtitle = "View all your past redemptions",
                    icon = Icons.Default.List,
                    onClick = { navController.navigate(Screen.Points.route) }
                )
                
                ActionItem(
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    icon = Icons.Default.Info,
                    onClick = { isPrivacyExpanded = !isPrivacyExpanded },
                    trailingIcon = if (isPrivacyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                )

                AnimatedVisibility(visible = isPrivacyExpanded) {
                    PrivacyPolicySection()
                }

                Spacer(modifier = Modifier.height(16.dp))

                val supportContext = LocalContext.current
                ActionItem(
                    title = "Contact Support",
                    subtitle = "Get help at quizedguy@gmail.com",
                    icon = Icons.Default.Email,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:quizedguy@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "GenGhealth Support Request")
                        }
                        supportContext.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Log Out", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
                BannerAdView(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to sign out of GenGhealth?") },
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
fun HeaderSection(name: String, email: String, gradient: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(text = title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Default.KeyboardArrowRight
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(trailingIcon, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun PrivacyPolicySection() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PrivacyItem(
                title = "Usage Tracking",
                content = "We track your screen time duration locally. Only points and redemptions are synced to our servers."
            )
            PrivacyItem(
                title = "Data Security",
                content = "We never share your data. Your name and email are used only for account identification and reward issuance."
            )
            PrivacyItem(
                title = "Permissions",
                content = "Usage Access is strictly for calculations. Notification Access is for threshold alerts."
            )
        }
    }
}

@Composable
fun PrivacyItem(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(text = content, style = MaterialTheme.typography.bodySmall)
    }
}
