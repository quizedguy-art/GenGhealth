package com.quizedguy.genghealth.ui.components
 
import com.quizedguy.genghealth.BuildConfig

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import android.widget.Toast

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Always use Production Ad Unit ID as per user request
                adUnitId = context.getString(com.quizedguy.genghealth.R.string.ad_unit_banner)
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdMob", "Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdMob", "Ad failed to load: ${error.message} (Code: ${error.code})")
                        // Show diagnostic Toast to help user understand why banner ads are missing
                        Toast.makeText(context, "Banner Ad error: ${error.code}", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAdOpened() {
                        Log.d("AdMob", "Ad opened")
                    }
                }
                
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
