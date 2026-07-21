package com.quizedguy.genghealth.shared.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.genghealth.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.genghealth.shared.ui.viewmodel.ReferralViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.quizedguy.genghealth.shared.ui.components.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    referralViewModel: ReferralViewModel = viewModel()
) {
    val referralCode by authViewModel.userReferralCode.collectAsState()
    val referrals by referralViewModel.referrals.collectAsState()
    val isLoading by referralViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Referrals", fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-6).dp)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.offset(y = (-6).dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Header Card with Code (Made smaller)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Refer Friends & Earn",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Both get 500 points when they sign up using your code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = referralCode ?: "......",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        referralCode?.let {
                                            clipboardManager.setText(AnnotatedString(it))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Copy Code")
                                }
                                
                                FilledTonalIconButton(
                                    onClick = {
                                        referralCode?.let {
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                val shareMessage = """
                                                    Join me on GenGhealth!
                                                    Use code to get 500 points: $it
                                                    Download: https://quizedguy-art.github.io/GenGhealth/
                                                """.trimIndent()
                                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Code"))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                    }
                }

                // Stats row (Made smaller)
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Total Refers", style = MaterialTheme.typography.labelSmall)
                                Text("${referrals.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Points Earned", style = MaterialTheme.typography.labelSmall)
                                Text("${referrals.sumOf { it.pointsAwarded }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Referral History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (referrals.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No referrals yet. Start sharing!", color = Color.Gray)
                        }
                    }
                } else {
                    items(referrals) { record ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ReferralItem(record)
                        }
                    }
                }
            }
            
            BannerAdView(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun ReferralItem(record: com.quizedguy.genghealth.ui.viewmodel.ReferralRecord) {
    val date = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }
    
    val maskedEmail = remember(record.referredEmail) {
        val parts = record.referredEmail.split("@")
        if (parts.size == 2) {
            val name = parts[0]
            val domain = parts[1]
            if (name.length > 2) {
                name.take(2) + "***@" + domain
            } else {
                "***@" + domain
            }
        } else {
            record.referredEmail
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.referredName, fontWeight = FontWeight.Bold)
                Text(text = maskedEmail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "+${record.pointsAwarded}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
