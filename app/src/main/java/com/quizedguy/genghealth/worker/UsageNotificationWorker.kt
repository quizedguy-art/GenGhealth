package com.quizedguy.genghealth.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quizedguy.genghealth.data.UsageStatsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

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

        return Result.success()
    }

    private suspend fun checkPointCredits() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        try {
            val userSnap = db.collection("users").document(userId).get().await()
            if (userSnap.exists()) {
                val points = userSnap.getLong("points")?.toInt() ?: 0
                val prefs = applicationContext.getSharedPreferences("points_prefs", Context.MODE_PRIVATE)
                val lastPoints = prefs.getInt("last_points", -1)
                
                if (lastPoints != -1 && points > lastPoints) {
                    val diff = points - lastPoints
                    sendCreditNotification(diff)
                }
                
                prefs.edit().putInt("last_points", points).apply()
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error checking points: ${e.message}")
        }
    }

    private fun sendCreditNotification(amount: Int) {
        val channelId = "reward_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Rewards", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Points Credited! 🎉")
            .setContentText("Admin has credited $amount points to your account. Great job!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("GenGhealth Alert")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(if (isHighPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(threshold.toInt(), notification)
        }
    }
}
