package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.adDataStore by preferencesDataStore(name = "nexakit_ad_prefs")

enum class AdLoadStatus {
    IDLE,
    LOADING,
    READY,
    FAILED
}

data class AdRuntimeState(
    val sdkInitialized: Boolean = false,
    val sdkInitSummary: String = "Initializing Google Mobile Ads SDK...",
    val bannerStatus: AdLoadStatus = AdLoadStatus.IDLE,
    val bannerMessage: String = "Preparing adaptive banner",
    val interstitialStatus: AdLoadStatus = AdLoadStatus.IDLE,
    val interstitialMessage: String = "Not loaded yet",
    val rewardedStatus: AdLoadStatus = AdLoadStatus.IDLE,
    val rewardedMessage: String = "Not loaded yet",
    val totalImpressions: Int = 0,
    val adFreeUntilMs: Long = 0L,
    val lastInterstitialShownMs: Long = 0L
) {
    val isAdFreeActive: Boolean
        get() = System.currentTimeMillis() < adFreeUntilMs

    val adFreeRemainingMinutes: Long
        get() = ((adFreeUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)) / 60_000L
}

object AdManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val KEY_AD_FREE_UNTIL = longPreferencesKey("ad_free_until_ms")
    private val KEY_IMPRESSIONS = intPreferencesKey("total_impressions")

    private val _state = MutableStateFlow(AdRuntimeState())
    val state: StateFlow<AdRuntimeState> = _state.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitializing = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (isInitializing || _state.value.sdkInitialized) return
        isInitializing = true

        scope.launch(Dispatchers.IO) {
            runCatching {
                val prefs = appContext.adDataStore.data.first()
                _state.update {
                    it.copy(
                        adFreeUntilMs = prefs[KEY_AD_FREE_UNTIL] ?: 0L,
                        totalImpressions = prefs[KEY_IMPRESSIONS] ?: 0
                    )
                }
            }

            runCatching {
                MobileAds.initialize(appContext) { initStatus ->
                    val adapterCount = initStatus.adapterStatusMap.size
                    _state.update {
                        it.copy(
                            sdkInitialized = true,
                            sdkInitSummary = "AdMob SDK Ready ($adapterCount adapter${if (adapterCount == 1) "" else "s"} active)"
                        )
                    }
                    scope.launch(Dispatchers.Main) {
                        loadInterstitial(appContext)
                        loadRewarded(appContext)
                    }
                }
            }.onFailure { error ->
                isInitializing = false
                _state.update {
                    it.copy(
                        sdkInitialized = false,
                        sdkInitSummary = "SDK init error: ${error.localizedMessage ?: "Unknown"}"
                    )
                }
            }
        }
    }

    fun updateBannerStatus(status: AdLoadStatus, message: String) {
        _state.update { it.copy(bannerStatus = status, bannerMessage = message) }
        if (status == AdLoadStatus.READY) {
            // Count banner impression once loaded
        }
    }

    fun recordImpression(context: Context) {
        val appContext = context.applicationContext
        _state.update { it.copy(totalImpressions = it.totalImpressions + 1) }
        scope.launch(Dispatchers.IO) {
            runCatching {
                appContext.adDataStore.edit { prefs ->
                    prefs[KEY_IMPRESSIONS] = _state.value.totalImpressions
                }
            }
        }
    }

    fun loadInterstitial(context: Context) {
        val appContext = context.applicationContext
        if (interstitialAd != null || _state.value.interstitialStatus == AdLoadStatus.LOADING) return

        _state.update {
            it.copy(
                interstitialStatus = AdLoadStatus.LOADING,
                interstitialMessage = "Loading interstitial ad..."
            )
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            AdMobConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _state.update {
                        it.copy(
                            interstitialStatus = AdLoadStatus.READY,
                            interstitialMessage = "Preloaded & ready"
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    _state.update {
                        it.copy(
                            interstitialStatus = AdLoadStatus.FAILED,
                            interstitialMessage = "Load failed (${loadAdError.code}): ${loadAdError.message}"
                        )
                    }
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity?,
        forceBypassCooldown: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        if (activity == null) {
            onComplete()
            return
        }
        val current = _state.value
        if (!forceBypassCooldown && current.isAdFreeActive) {
            onComplete()
            return
        }
        val elapsed = System.currentTimeMillis() - current.lastInterstitialShownMs
        if (!forceBypassCooldown && elapsed < AdMobConfig.INTERSTITIAL_COOLDOWN_MS) {
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity)
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                _state.update {
                    it.copy(lastInterstitialShownMs = System.currentTimeMillis())
                }
                recordImpression(activity)
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _state.update {
                    it.copy(
                        interstitialStatus = AdLoadStatus.IDLE,
                        interstitialMessage = "Reloading next ad..."
                    )
                }
                loadInterstitial(activity)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                _state.update {
                    it.copy(
                        interstitialStatus = AdLoadStatus.FAILED,
                        interstitialMessage = "Show failed: ${adError.message}"
                    )
                }
                loadInterstitial(activity)
                onComplete()
            }
        }
        ad.show(activity)
    }

    fun loadRewarded(context: Context) {
        val appContext = context.applicationContext
        if (rewardedAd != null || _state.value.rewardedStatus == AdLoadStatus.LOADING) return

        _state.update {
            it.copy(
                rewardedStatus = AdLoadStatus.LOADING,
                rewardedMessage = "Loading rewarded video..."
            )
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            appContext,
            AdMobConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _state.update {
                        it.copy(
                            rewardedStatus = AdLoadStatus.READY,
                            rewardedMessage = "Rewarded video ready"
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    _state.update {
                        it.copy(
                            rewardedStatus = AdLoadStatus.FAILED,
                            rewardedMessage = "Load failed (${loadAdError.code}): ${loadAdError.message}"
                        )
                    }
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity?,
        onRewardEarned: () -> Unit = {},
        onUnavailable: (String) -> Unit = {}
    ) {
        if (activity == null) {
            onUnavailable("Activity not available")
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onUnavailable("Rewarded ad is still loading. Try again in a moment.")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                recordImpression(activity)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _state.update {
                    it.copy(
                        rewardedStatus = AdLoadStatus.IDLE,
                        rewardedMessage = "Reloading rewarded ad..."
                    )
                }
                loadRewarded(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _state.update {
                    it.copy(
                        rewardedStatus = AdLoadStatus.FAILED,
                        rewardedMessage = "Show failed: ${adError.message}"
                    )
                }
                loadRewarded(activity)
                onUnavailable(adError.message)
            }
        }

        ad.show(activity) { _ ->
            val newExpiry = System.currentTimeMillis() + AdMobConfig.REWARD_AD_FREE_DURATION_MS
            _state.update { it.copy(adFreeUntilMs = newExpiry) }
            scope.launch(Dispatchers.IO) {
                runCatching {
                    activity.applicationContext.adDataStore.edit { prefs ->
                        prefs[KEY_AD_FREE_UNTIL] = newExpiry
                    }
                }
            }
            onRewardEarned()
        }
    }

    fun clearAdFreePass(context: Context) {
        val appContext = context.applicationContext
        _state.update { it.copy(adFreeUntilMs = 0L) }
        scope.launch(Dispatchers.IO) {
            runCatching {
                appContext.adDataStore.edit { prefs ->
                    prefs[KEY_AD_FREE_UNTIL] = 0L
                }
            }
        }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
