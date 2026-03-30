package com.example.geng_health.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.geng_health.R
import com.example.geng_health.data.UsageStatsHelper
import java.time.LocalDate
import java.time.ZoneId

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class MidnightRewardWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val totalMillis = UsageStatsHelper.getTodayTotalScreenTime(applicationContext)
        val points = calculatePoints(totalMillis)

        if (points > 0) {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid
            
            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .update("points", FieldValue.increment(points.toLong()))
                
                sendNotification(points)
            }
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
