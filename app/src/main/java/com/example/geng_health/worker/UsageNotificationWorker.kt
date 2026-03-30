package com.example.geng_health.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.geng_health.data.UsageStatsHelper

class UsageNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val totalMillis = UsageStatsHelper.getTodayTotalScreenTime(applicationContext)
        val hours = totalMillis / 3600000.0

        // 2-Hourly Reminders
        checkAndNotify(hours, 2.0, "You've been on your phone for 2 hours! Take a break to protect your rewards.")
        checkAndNotify(hours, 4.0, "4 hours of usage today. Careful! You're halfway to losing your top rewards.")
        checkAndNotify(hours, 6.0, "6 hours passed! Your rewards are decreasing. Put the phone down now!")

        // Proximity Thresholds (Proactive alerts)
        checkAndNotify(hours, 5.0, "Approaching 5h limit! Stay below to earn 200 points.", isHighPriority = true)
        checkAndNotify(hours, 7.0, "Approaching 7h limit! Last chance for any points today.", isHighPriority = true)

        return Result.success()
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
