package com.quizedguy.genghealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.quizedguy.genghealth.ui.MainComposeApp
import com.quizedguy.genghealth.ui.theme.Geng_healthTheme
import com.google.android.gms.ads.MobileAds
import com.quizedguy.genghealth.util.AgeSignalsHelper
import com.quizedguy.genghealth.util.RewardedAdManager
import com.quizedguy.genghealth.util.RewardedInterstitialAdManager
import com.quizedguy.genghealth.util.ConsentManager
import java.util.concurrent.atomic.AtomicBoolean

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var consentManager: ConsentManager
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            android.util.Log.e("MainActivity", "Update flow failed! Result code: ${result.resultCode}")
            // Re-check to prompt them again if they cancelled an immediate update
            promptForUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Query age signals for regional compliance (e.g. Texas SB 2420)
        AgeSignalsHelper.checkAge(this)
        
        // Initialize AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        
        // Initialize ConsentManager
        consentManager = ConsentManager(this)
        consentManager.gatherConsent(object : ConsentManager.OnConsentGatheringCompleteListener {
            override fun consentGatheringComplete(error: String?) {
                if (error != null) {
                    android.util.Log.e("MainActivity", "Consent error: $error")
                }
                
                if (consentManager.canRequestAds) {
                    initializeMobileAdsSdk()
                }
            }
        })
        
        // Check if we can initialize ads immediately (e.g. from a previous session)
        if (consentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }
        
        enableEdgeToEdge()
        setContent {
            Geng_healthTheme {
                MainComposeApp()
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        (application as GengHealthApplication).initializeMobileAds()
    }

    override fun onResume() {
        super.onResume()
        resumeUpdateIfInProgress()
        promptForUpdate()
    }

    private fun promptForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to start in-app update: ${e.message}")
                }
            }
        }
    }

    private fun resumeUpdateIfInProgress() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to resume in-app update: ${e.message}")
                }
            }
        }
    }
}
