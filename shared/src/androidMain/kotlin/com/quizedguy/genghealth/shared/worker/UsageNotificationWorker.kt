package com.quizedguy.genghealth.shared.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quizedguy.genghealth.shared.data.UsageStatsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.time.LocalDate

class UsageNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val totalMillis = UsageStatsHelper.getTodayTotalScreenTime(applicationContext)
        val hours = totalMillis / 3600000.0

        // 2-Hourly Reminders
        checkAndNotify(hours, 2.0, "You've been on your phone for 2 hours! Keep usage low to earn rewards from Admin.")
        checkAndNotify(hours, 4.0, "4 hours of usage today. Careful! Keep usage low to maximize your points.")
        checkAndNotify(hours, 6.0, "6 hours passed! Your potential rewards are decreasing. Put the phone down!")

        // Proximity Thresholds (Proactive alerts)
        checkAndNotify(hours, 5.0, "Approaching 5h limit! Stay below for maximum potential points.", isHighPriority = true)
        checkAndNotify(hours, 7.0, "Approaching 7h limit! Last chance for points today.", isHighPriority = true)

        // Point Credit Check
        checkPointCredits()

        // Sync usage to Firestore every 15 minutes
        syncUsageToFirestore(totalMillis)

        return Result.success()
    }

    private suspend fun checkPointCredits() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        try {
            // Check for approved but uncollected daily usage records
            val usageList = db.collection("daily_usage")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isApproved", true)
                .whereEqualTo("isCollected", false)
                .get().await()
            
            if (!usageList.isEmpty) {
                val prefs = applicationContext.getSharedPreferences("points_prefs", Context.MODE_PRIVATE)
                val notifiedIds = prefs.getStringSet("notified_approved_ids", emptySet()) ?: emptySet()
                val currentIds = usageList.documents.map { it.id }.toSet()
                
                val newApprovedIds = currentIds.subtract(notifiedIds)
                if (newApprovedIds.isNotEmpty()) {
                    val totalPoints = usageList.documents
                        .filter { newApprovedIds.contains(it.id) }
                        .sumOf { it.getLong("pointsPotential") ?: 0L }
                    
                    if (totalPoints > 0) {
                        sendApprovedNotification(totalPoints.toInt())
                        prefs.edit().putStringSet("notified_approved_ids", notifiedIds + newApprovedIds).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error checking points: ${e.message}")
        }
    }

    private suspend fun syncUsageToFirestore(totalMillis: Long) {
        val today = LocalDate.now().toString()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        try {
            val hours = totalMillis / 3600000.0
            val pointsPotential = when {
                hours < 5 -> 200
                hours < 6 -> 100
                hours < 7 -> 50
                else -> 0
            }

            val docId = "${userId}_$today"
            val usageRef = db.collection("daily_usage").document(docId)
            val usageSnapshot = usageRef.get().await()

            if (usageSnapshot.exists()) {
                val isCollected = usageSnapshot.getBoolean("isCollected") ?: false
                if (!isCollected) {
                    usageRef.update(
                        "totalMillis", totalMillis,
                        "pointsPotential", pointsPotential
                    ).await()
                } else {
                    usageRef.update("totalMillis", totalMillis).await()
                }
            } else {
                val record = hashMapOf(
                    "userId" to userId,
                    "date" to today,
                    "totalMillis" to totalMillis,
                    "pointsPotential" to pointsPotential,
                    "isCollected" to false,
                    "isApproved" to false
                )
                usageRef.set(record).await()
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error syncing usage: ${e.message}")
        }
    }

    private fun sendApprovedNotification(amount: Int) {
        val channelId = "reward_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Rewards", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, com.quizedguy.genghealth.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Points Ready to Collect! 🎉")
            .setContentText("Admin has approved $amount points for you. Open the app now to watch an ad and collect them!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(99, notification)
    }

    private fun checkAndNotify(currentHours: Double, threshold: Double, message: String, isHighPriority: Boolean = false) {
        // Notify if within 10 minutes of the threshold or interval
        if (currentHours >= (threshold - 0.16) && currentHours < threshold) {
            val channelId = "usage_channel"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val importance = if (isHighPriority) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, "Usage Alerts", importance)
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(applicationContext, com.quizedguy.genghealth.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("GenGhealth Alert")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(threshold.toInt(), notification)
        }
    }
}
