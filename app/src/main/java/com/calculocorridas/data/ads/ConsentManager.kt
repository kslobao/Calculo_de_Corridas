package com.calculocorridas.data.ads

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsentManager @Inject constructor() {

    private val _canShowAds = MutableStateFlow(false)
    val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    fun requestConsent(activity: Activity, isDebug: Boolean = false) {
        val params = ConsentRequestParameters.Builder().apply {
            setTagForUnderAgeOfConsent(false)
            if (isDebug) {
                val debugSettings = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
                setConsentDebugSettings(debugSettings)
            }
        }.build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(activity, params,
            {
                if (consentInfo.isConsentFormAvailable) {
                    loadAndShowConsentForm(activity, consentInfo)
                } else {
                    _canShowAds.value = consentInfo.canRequestAds()
                }
            },
            { _canShowAds.value = true }
        )
    }

    private fun loadAndShowConsentForm(activity: Activity, info: ConsentInformation) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
            _canShowAds.value = info.canRequestAds()
        }
    }

    private fun ConsentInformation.canRequestAds(): Boolean =
        consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
}
