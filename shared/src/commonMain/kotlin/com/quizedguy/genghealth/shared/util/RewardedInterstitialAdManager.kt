package com.quizedguy.genghealth.shared.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.quizedguy.genghealth.BuildConfig
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages the loading and showing of rewarded interstitial ads globally.
 */
object RewardedInterstitialAdManager {
    private const val TAG = "RewardedInterstitial"
    private fun getAdUnitId(context: Context): String {
        return if (BuildConfig.DEBUG) {
            "ca-app-pub-3940256099942544/5354046379"
        } else {
            context.getString(com.quizedguy.genghealth.R.string.ad_unit_rewarded_interstitial)
        }
    }
    
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false
    private var loadTime: Long = 0
    private var lastShowTime: Long = 0
    private var retryAttempt = 0
    
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded
    
    private var onAdLoadedCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Checks if a loaded ad is still valid.
     */
    fun isAdAvailable(): Boolean {
        val now = System.currentTimeMillis()
        val fourHoursInMillis = 4 * 60 * 60 * 1000
        val isExpired = now - loadTime > fourHoursInMillis
        
        return rewardedInterstitialAd != null && !isExpired
    }

    /**
     * Loads a rewarded interstitial ad.
     */
    fun loadAd(context: Context) {
        if (!com.quizedguy.genghealth.GengHealthApplication.isAdSdkInitialized) {
            Log.d(TAG, "Ad SDK is not initialized yet. Skipping load.")
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        if (AgeSignalsHelper.isMinor.value) {
            Log.d(TAG, "Blocking ad load: User is a minor according to Play Age Signals API.")
            rewardedInterstitialAd = null
            _isAdLoaded.value = false
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        if (isAdAvailable()) {
            onAdLoadedCallback?.invoke(true)
            onAdLoadedCallback = null
            return
        }
        if (isLoading) {
            return
        }
        
        isLoading = true
        Log.d(TAG, "Loading rewarded interstitial ad...")
        
        val adRequest = AdRequest.Builder().build()
        val currentAdUnitId = getAdUnitId(context)
        
        RewardedInterstitialAd.load(context, currentAdUnitId, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message}")
                rewardedInterstitialAd = null
                isLoading = false
                _isAdLoaded.value = false
                
                onAdLoadedCallback?.invoke(false)
                onAdLoadedCallback = null

                if (retryAttempt < 3) {
                    retryAttempt++
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Retrying to load rewarded interstitial ad (Attempt $retryAttempt)...")
                        loadAd(context)
                    }, 15000)
                }
            }
 
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                Log.d(TAG, "Ad was loaded successfully.")
                rewardedInterstitialAd = ad
                isLoading = false
                loadTime = System.currentTimeMillis()
                retryAttempt = 0
                _isAdLoaded.value = true
                
                rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed.")
                        rewardedInterstitialAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
 
                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e(TAG, "Ad failed to show: ${adError.message}")
                        rewardedInterstitialAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
                }

                onAdLoadedCallback?.invoke(true)
                onAdLoadedCallback = null
            }
        })
    }

    /**
     * Shows the ad if it's available and cooldown has passed.
     */
    fun showAd(activity: Activity, force: Boolean = false, onRewardEarned: (() -> Unit)? = null) {
        if (AgeSignalsHelper.isMinor.value) {
            Log.w(TAG, "Blocking ad show: User is a minor.")
            return
        }
        val now = System.currentTimeMillis()
        // Minimum 60 seconds between ads to avoid spamming and AdMob policy violations
        if (!force && now - lastShowTime < 60000) {
            Log.d(TAG, "Cooldown active. Skipping ad show.")
            return
        }

        if (isAdAvailable()) {
            lastShowTime = now
            rewardedInterstitialAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                creditPoints(2) // Fixed 2 points per ad
                onRewardEarned?.invoke()
            }
        } else {
            Log.d(TAG, "The ad wasn't ready.")
            loadAd(activity)
        }
    }

    /**
     * Loads a rewarded interstitial ad on demand and calls the callback when done.
     */
    fun loadAdOnDemand(context: Context, callback: (Boolean) -> Unit) {
        if (isAdAvailable()) {
            callback(true)
            return
        }
        onAdLoadedCallback = callback
        loadAd(context)
    }

    private fun creditPoints(points: Int) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .set(hashMapOf("points" to FieldValue.increment(points.toLong())), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error crediting points: ${e.message}")
            }
        }
    }
}
