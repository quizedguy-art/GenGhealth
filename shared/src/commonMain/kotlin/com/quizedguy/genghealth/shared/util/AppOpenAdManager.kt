package com.quizedguy.genghealth.shared.util
 
import com.quizedguy.genghealth.BuildConfig

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quizedguy.genghealth.GengHealthApplication
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Prefetches and shows App Open ads when the app is brought to the foreground.
 */
class AppOpenAdManager(private val myApplication: GengHealthApplication) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var retryAttempt = 0

    /** Keep track of the time an ad was loaded to ensures it's still valid. */
    private var loadTime: Long = 0

    private var currentActivity: Activity? = null

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Request an ad. */
    fun fetchAd() {
        if (!com.quizedguy.genghealth.GengHealthApplication.isAdSdkInitialized) {
            Log.d(TAG, "Ad SDK is not initialized yet. Skipping fetch.")
            return
        }
        if (AgeSignalsHelper.isMinor.value) {
            Log.d(TAG, "Blocking App Open ad fetch: User is a minor.")
            appOpenAd = null
            return
        }
        // Have unused ad, no need to fetch another.
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        val adUnitId = if (BuildConfig.DEBUG) {
            myApplication.getString(com.quizedguy.genghealth.R.string.test_ad_unit_app_open)
        } else {
            myApplication.getString(com.quizedguy.genghealth.R.string.ad_unit_app_open)
        }

        AppOpenAd.load(
            myApplication,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    retryAttempt = 0
                    Log.d(TAG, "Ad loaded successfully.")
                }
 
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    appOpenAd = null
                    Log.e(TAG, "Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    
                    if (retryAttempt < 3) {
                        retryAttempt++
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d(TAG, "Retrying App Open ad load (Attempt $retryAttempt)...")
                            fetchAd()
                        }, 15000)
                    }
                }
            }
        )
    }

    /** Utility method that checks if ad exists and can be shown. */
    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Show the ad if one is available or has been loaded. */
    fun showAdIfAvailable(activity: Activity, onAdClosed: () -> Unit = {}) {
        if (AgeSignalsHelper.isMinor.value) {
            Log.d(TAG, "Blocking App Open ad show: User is a minor.")
            appOpenAd = null
            onAdClosed()
            return
        }
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.")
            onAdClosed()
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.")
            fetchAd()
            onAdClosed()
            return
        }

        Log.d(TAG, "Will show ad.")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Set the ad reference to null so that it is not shown again.
                appOpenAd = null
                isShowingAd = false
                Log.d(TAG, "Ad dismissed full screen content.")
                fetchAd()
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                fetchAd()
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Ad showed full screen content.")
            }
        }

        appOpenAd?.show(activity)
    }

    /** LifecycleObserver method that shows an app open ad when the app moves to foreground. */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let {
            showAdIfAvailable(it)
        }
    }

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // An ad should only be shown if there is a relevant context.
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    companion object {
        private const val TAG = "AppOpenAdManager"
    }
}
