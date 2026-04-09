package com.quizedguy.genghealth

import android.app.Application
import android.util.Log
import com.quizedguy.genghealth.util.AppOpenAdManager
import com.quizedguy.genghealth.util.RewardedAdManager
import com.google.android.gms.ads.MobileAds
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quizedguy.genghealth.worker.MidnightRewardWorker
import com.quizedguy.genghealth.worker.UsageNotificationWorker
import java.util.concurrent.TimeUnit

class GengHealthApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()

        // Initialize the App Open Ad Manager.
        appOpenAdManager = AppOpenAdManager(this)

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("GengHealthApp", "Mobile Ads SDK initialized status: $initializationStatus")
            
            // Initial ad fetches only after SDK is ready
            appOpenAdManager.fetchAd()
            RewardedAdManager.loadAd(this)
        }

        // Schedule Workers
        scheduleBackgroundWorkers()
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        // 1. Usage Notification Worker (Every 15 minutes - minimum allowed)
        val usageRequest = PeriodicWorkRequestBuilder<UsageNotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "UsageNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            usageRequest
        )

        // 2. Midnight Reward Worker (Every 24 hours)
        val rewardRequest = PeriodicWorkRequestBuilder<MidnightRewardWorker>(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "MidnightRewardWork",
            ExistingPeriodicWorkPolicy.KEEP,
            rewardRequest
        )
        
        Log.d("GengHealthApp", "Background workers scheduled successfully.")
    }
}
