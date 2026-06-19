package com.calculocorridas.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _interstitialReady = MutableStateFlow(false)
    val interstitialReady: StateFlow<Boolean> = _interstitialReady.asStateFlow()

    private var rideCountSinceLastInterstitial = 0
    private var interstitialIntervalRides = 5

    fun setInterstitialInterval(rides: Int) {
        interstitialIntervalRides = rides
    }

    fun loadInterstitial(unitId: String) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, unitId, request, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                _interstitialReady.value = true
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                _interstitialReady.value = false
            }
        })
    }

    fun loadRewarded(unitId: String) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, unitId, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }

    fun onRideDetected(unitId: String) {
        rideCountSinceLastInterstitial++
        if (rideCountSinceLastInterstitial >= interstitialIntervalRides) {
            rideCountSinceLastInterstitial = 0
        }
    }

    fun showInterstitialIfReady(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = interstitialAd ?: run { onDismiss(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _interstitialReady.value = false
                onDismiss()
            }
        }
        ad.show(activity)
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onDismiss: () -> Unit = {}) {
        val ad = rewardedAd ?: run { onDismiss(); return }
        ad.show(activity) {
            onRewarded()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismiss()
            }
        }
    }
}
