package com.quizedguy.genghealth.shared.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayTotalScreenTime(context: Context): Long {
        return getUsageForDate(context, LocalDate.now())
    }

    private fun getLauncherPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val launcherPackages = mutableSetOf<String>()
        try {
            val list = pm.queryIntentActivities(launcherIntent, 0)
            for (resolveInfo in list) {
                resolveInfo.activityInfo?.packageName?.let {
                    launcherPackages.add(it)
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        
        // Also add the default home launcher
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val defaultLauncherInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            defaultLauncherInfo?.activityInfo?.packageName?.let {
                launcherPackages.add(it)
            }
        } catch (e: Exception) {
            // Fallback
        }
        
        return launcherPackages
    }

    fun getUsageForDate(context: Context, date: LocalDate): Long {
        if (!hasUsageStatsPermission(context)) return 0L

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Start and end of the specified date
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val launcherPackages = getLauncherPackages(context)
        
        // Initial state carryover check
        // Query events from 1 hour before midnight to midnight to determine what was in foreground
        val preEvents = usageStatsManager.queryEvents(start - 3600000, start)
        val preEvent = UsageEvents.Event()
        var initialApp: String? = null
        var initialAppStartTime = 0L
        
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(preEvent)
            val eventType = preEvent.eventType
            val pkg = preEvent.packageName
            val time = preEvent.timeStamp
            
            when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != null && (pkg == context.packageName || launcherPackages.contains(pkg))) {
                        initialApp = pkg
                        initialAppStartTime = time
                    } else {
                        initialApp = null
                        initialAppStartTime = 0L
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (initialApp != null && initialApp == pkg) {
                        initialApp = null
                        initialAppStartTime = 0L
                    }
                }
                16, 17 -> { // SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN
                    initialApp = null
                    initialAppStartTime = 0L
                }
            }
        }

        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        
        var totalTime = 0L
        var currentApp: String? = null
        var currentAppStartTime = 0L

        // If there was an app carryover from the previous day, initialize it
        if (initialApp != null) {
            currentApp = initialApp
            currentAppStartTime = start
        }
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val eventType = event.eventType
            val pkg = event.packageName
            val time = event.timeStamp
            
            when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != null && (pkg == context.packageName || launcherPackages.contains(pkg))) {
                        if (currentApp != null) {
                            if (currentApp != pkg) {
                                totalTime += (time - currentAppStartTime)
                                currentApp = pkg
                                currentAppStartTime = time
                            }
                        } else {
                            currentApp = pkg
                            currentAppStartTime = time
                        }
                    } else {
                        if (currentApp != null) {
                            totalTime += (time - currentAppStartTime)
                            currentApp = null
                            currentAppStartTime = 0L
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentApp != null && currentApp == pkg) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
                16 -> { // UsageEvents.Event.SCREEN_NON_INTERACTIVE
                    if (currentApp != null) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
                17 -> { // UsageEvents.Event.KEYGUARD_SHOWN
                    if (currentApp != null) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
            }
        }
        
        // If an app is still in the foreground at the end of the query range
        if (currentApp != null && currentAppStartTime > 0L) {
            val queryEnd = Math.min(System.currentTimeMillis(), end)
            if (queryEnd > currentAppStartTime) {
                totalTime += (queryEnd - currentAppStartTime)
            }
        }
        
        return totalTime
    }
}
