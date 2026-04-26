package com.quizedguy.genghealth.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quizedguy.genghealth.R
import com.quizedguy.genghealth.data.UsageStatsHelper
import java.time.LocalDate
import java.time.ZoneId

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import android.util.Log

class MidnightRewardWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString() // e.g., "2026-03-31"
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        try {
            // 1. Check if already rewarded today
            val userDoc = db.collection("users").document(userId).get().await()
            val lastRewardDate = userDoc.getString("lastRewardDate")

            if (lastRewardDate == today) {
                Log.d("MidnightReward", "Already rewarded today ($today). Skipping.")
                return Result.success()
            }

            // 2. Calculate today's usage and points
            val totalMillis = UsageStatsHelper.getTodayTotalScreenTime(applicationContext)
            val pointsPotential = calculatePoints(totalMillis)

            val docId = "${userId}_$today"
            val usageRef = db.collection("daily_usage").document(docId)
            val usageSnapshot = usageRef.get().await()
            
            if (usageSnapshot.exists()) {
                // If document already exists, only update usage data
                val isCollected = usageSnapshot.getBoolean("isCollected") ?: false
                
                if (!isCollected) {
                    usageRef.update(
                        "totalMillis", totalMillis,
                        "pointsPotential", pointsPotential
                    ).await()
                } else {
                    // If already collected, only update millis
                    usageRef.update("totalMillis", totalMillis).await()
                }
            } else {
                // Create new record
                val record = hashMapOf(
                    "userId" to userId,
                    "date" to today,
                    "totalMillis" to totalMillis,
                    "pointsPotential" to pointsPotential,
                    "isCollected" to false
                )
                usageRef.set(record).await()
            }
            
            // Mark lastRewardDate in user doc to avoid daily worker duplication
            db.collection("users").document(userId)
                .update("lastRewardDate", today)
                .await()

            if (pointsPotential > 0) {
                Log.d("MidnightReward", "Successfully created record for $pointsPotential points for $today.")
                sendNotification(pointsPotential)
            } else {
                Log.d("MidnightReward", "No points earned today ($today).")
            }

        } catch (e: Exception) {
            Log.e("MidnightReward", "Error awarding points: ${e.message}")
            return Result.retry() // Retry if transient error
        }

        return Result.success()
    }

    private fun calculatePoints(millis: Long): Int {
        val hours = millis / 3600000.0
        return when {
            hours < 5 -> 200
            hours < 6 -> 100
            hours < 7 -> 50
            else -> 0
        }
    }

    private fun sendNotification(points: Int) {
        val channelId = "reward_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Rewards", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Reward Earned!")
            .setContentText("You earned $points points for today's healthy phone usage!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}
