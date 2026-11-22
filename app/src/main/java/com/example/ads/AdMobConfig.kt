package com.example.ads

/**
 * Central Google AdMob Configuration.
 *
 * Out of the box, this uses Google's official Test Ad Unit IDs so ads load immediately
 * during development without risking invalid-traffic policy violations.
 *
 * TO START EARNING REAL REVENUE:
 * 1. Replace `admob_app_id` in `app/src/main/res/values/strings.xml` with your AdMob App ID.
 * 2. Replace the 3 Ad Unit IDs below with your real Banner, Interstitial, and Rewarded IDs
 *    from https://admob.google.com.
 */
object AdMobConfig {
    // Official Google Test IDs (Safe for emulator, testing & screenshots)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Active IDs used by the app — swap these with your live AdMob Ad Unit IDs before release
    const val BANNER_AD_UNIT_ID = TEST_BANNER_AD_UNIT_ID
    const val INTERSTITIAL_AD_UNIT_ID = TEST_INTERSTITIAL_AD_UNIT_ID
    const val REWARDED_AD_UNIT_ID = TEST_REWARDED_AD_UNIT_ID

    // Policy-friendly frequency cap: minimum 60 seconds between automatic interstitials
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    // Rewarded Ad reward duration: 2 hours of Ad-Free Pro mode
    const val REWARD_AD_FREE_DURATION_MS = 2 * 60 * 60 * 1000L

    val isUsingTestAdUnits: Boolean
        get() = BANNER_AD_UNIT_ID == TEST_BANNER_AD_UNIT_ID ||
            INTERSTITIAL_AD_UNIT_ID == TEST_INTERSTITIAL_AD_UNIT_ID ||
            REWARDED_AD_UNIT_ID == TEST_REWARDED_AD_UNIT_ID
}
