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

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Switch between Test and Production Ad Unit IDs
                adUnitId = if (BuildConfig.DEBUG) {
                    context.getString(com.quizedguy.genghealth.R.string.test_ad_unit_banner)
                } else {
                    context.getString(com.quizedguy.genghealth.R.string.ad_unit_banner)
                }
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdMob", "Ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdMob", "Ad failed to load: ${error.message} (Code: ${error.code})")
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
