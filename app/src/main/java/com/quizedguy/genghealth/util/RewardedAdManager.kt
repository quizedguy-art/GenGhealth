package com.quizedguy.genghealth.util

import com.quizedguy.genghealth.BuildConfig

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
 * Manages the loading and showing of rewarded ads globally.
 */
object RewardedAdManager {
    private const val TAG = "RewardedAdManager"
    private fun getAdUnitId(context: Context): String {
        // Always use Production Ad Unit ID as per user request
        return context.getString(com.quizedguy.genghealth.R.string.ad_unit_rewarded)
    }
    
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var loadTime: Long = 0
    private var retryAttempt = 0
    
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded
    
    private var onAdLoadedCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Checks if a loaded ad is still valid (Ads typically expire after 4 hours).
     */
    fun isAdAvailable(): Boolean {
        val now = System.currentTimeMillis()
        val fourHoursInMillis = 4 * 60 * 60 * 1000
        val isExpired = now - loadTime > fourHoursInMillis
        
        return rewardedAd != null && !isExpired
    }

    /**
     * Loads a rewarded ad.
     */
    fun loadAd(context: Context) {
        if (AgeSignalsHelper.isMinor.value) {
            Log.d(TAG, "Blocking ad load: User is a minor according to Play Age Signals API.")
            rewardedAd = null
            _isAdLoaded.value = false
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        if (isLoading || isAdAvailable()) {
            onAdLoadedCallback?.invoke(isAdAvailable())
            onAdLoadedCallback = null
            return
        }
        
        isLoading = true
        Log.d(TAG, "Loading rewarded ad (Attempt: ${retryAttempt + 1})...")
        
        val adRequest = AdRequest.Builder().build()
        val currentAdUnitId = getAdUnitId(context)
        Log.d(TAG, "Starting RewardedAd.load with ID: $currentAdUnitId")
        
        RewardedAd.load(context, currentAdUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message} (Code: ${adError.code})")
                
                rewardedAd = null
                isLoading = false
                _isAdLoaded.value = false
                
                onAdLoadedCallback?.invoke(false)
                onAdLoadedCallback = null
            }
 
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded successfully.")
                rewardedAd = ad
                isLoading = false
                loadTime = System.currentTimeMillis()
                retryAttempt = 0
                _isAdLoaded.value = true
                
                onAdLoadedCallback?.invoke(true)
                onAdLoadedCallback = null
                
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed.")
                        rewardedAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
 
                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e(TAG, "Ad failed to show: ${adError.message}")
                        rewardedAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
                }
            }
        })
    }

    /**
     * Shows the rewarded ad if it's available.
     */
    fun showAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (AgeSignalsHelper.isMinor.value) {
            Log.w(TAG, "Blocking ad show: User is a minor.")
            return
        }
        if (isAdAvailable()) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                creditPoints(3) // Fixed 3 points per ad
                onRewardEarned()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready or was stale.")
            rewardedAd = null
            loadAd(activity)
        }
    }

    /**
     * Loads a rewarded ad on demand and calls the callback when done.
     */
    fun loadAdOnDemand(context: Context, callback: (Boolean) -> Unit) {
        if (isAdAvailable()) {
            callback(true)
            return
        }
        onAdLoadedCallback = callback
        loadAd(context)
    }

    /**
     * Credits points to the user's Firestore document.
     */
    private fun creditPoints(points: Int) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(userId)
                    .set(hashMapOf("points" to FieldValue.increment(points.toLong())), SetOptions.merge())
                    .await()
                Log.d(TAG, "Successfully credited $points points.")
            } catch (e: Exception) {
                Log.e(TAG, "Error crediting points: ${e.message}")
            }
        }
    }
}
