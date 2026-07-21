package com.quizedguy.genghealth.shared.util

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: String?)
    }

    fun gatherConsent(onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener) {
        // Set tag for under age of consent. false means users are not under age of consent.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent has been gathered.
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError?.message)
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                Log.e("ConsentManager", "Consent failed to update: ${requestConsentError.message}")
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError.message)
            }
        )
    }

    // Check if you can initialize the Google Mobile Ads SDK in parallel
    // while checking for new consent information. Consent obtained in
    // the previous session can be used to request ads.
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()
}
