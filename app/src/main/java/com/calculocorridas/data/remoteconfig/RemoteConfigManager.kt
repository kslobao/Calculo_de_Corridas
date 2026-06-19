package com.calculocorridas.data.remoteconfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val settings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(defaults())
    }

    suspend fun fetch() = runCatching {
        remoteConfig.fetchAndActivate().await()
    }

    val freeMaxRides: Long         get() = remoteConfig.getLong(Keys.FREE_MAX_RIDES)
    val freeHistoryDays: Long      get() = remoteConfig.getLong(Keys.FREE_HISTORY_DAYS)
    val interstitialInterval: Long get() = remoteConfig.getLong(Keys.INTERSTITIAL_INTERVAL)
    val rewardedEnabled: Boolean   get() = remoteConfig.getBoolean(Keys.REWARDED_ENABLED)
    val showPromoBanner: Boolean   get() = remoteConfig.getBoolean(Keys.SHOW_PROMO_BANNER)
    val promoMessage: String       get() = remoteConfig.getString(Keys.PROMO_MESSAGE)
    val selectorApiUrl: String     get() = remoteConfig.getString(Keys.SELECTOR_API_URL)
    val forceUpdateMinVersion: Long get() = remoteConfig.getLong(Keys.FORCE_UPDATE_MIN_VERSION)
    val betaOcrEnabled: Boolean    get() = remoteConfig.getBoolean(Keys.BETA_OCR_ENABLED)

    private fun defaults() = mapOf(
        Keys.FREE_MAX_RIDES           to 500L,
        Keys.FREE_HISTORY_DAYS        to 30L,
        Keys.INTERSTITIAL_INTERVAL    to 5L,
        Keys.REWARDED_ENABLED         to true,
        Keys.SHOW_PROMO_BANNER        to false,
        Keys.PROMO_MESSAGE            to "",
        Keys.SELECTOR_API_URL         to "https://api.calculocorridas.com/",
        Keys.FORCE_UPDATE_MIN_VERSION to 0L,
        Keys.BETA_OCR_ENABLED         to false
    )

    object Keys {
        const val FREE_MAX_RIDES           = "free_max_rides"
        const val FREE_HISTORY_DAYS        = "free_history_days"
        const val INTERSTITIAL_INTERVAL    = "interstitial_interval"
        const val REWARDED_ENABLED         = "rewarded_enabled"
        const val SHOW_PROMO_BANNER        = "show_promo_banner"
        const val PROMO_MESSAGE            = "promo_message"
        const val SELECTOR_API_URL         = "selector_api_url"
        const val FORCE_UPDATE_MIN_VERSION = "force_update_min_version"
        const val BETA_OCR_ENABLED         = "beta_ocr_enabled"
    }
}
