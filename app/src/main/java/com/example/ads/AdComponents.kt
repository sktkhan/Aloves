package com.example.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseAccent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AnchoredAdaptiveBannerAd(
    modifier: Modifier = Modifier
) {
    val adState by AdManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AnimatedVisibility(visible = !adState.isAdFreeActive) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val adWidthDp = maxWidth.value.toInt().coerceAtLeast(320)
                    val adView = remember(adWidthDp) {
                        AdView(context).apply {
                            adUnitId = AdMobConfig.BANNER_AD_UNIT_ID
                            setAdSize(
                                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                    context,
                                    adWidthDp
                                )
                            )
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    AdManager.updateBannerStatus(
                                        AdLoadStatus.READY,
                                        "Adaptive banner live"
                                    )
                                }

                                override fun onAdImpression() {
                                    AdManager.recordImpression(context)
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    AdManager.updateBannerStatus(
                                        AdLoadStatus.FAILED,
                                        "Banner unavailable (${error.code})"
                                    )
                                }
                            }
                            AdManager.updateBannerStatus(
                                AdLoadStatus.LOADING,
                                "Loading adaptive banner..."
                            )
                            loadAd(AdRequest.Builder().build())
                        }
                    }

                    DisposableEffect(lifecycleOwner, adView) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> adView.resume()
                                Lifecycle.Event.ON_PAUSE -> adView.pause()
                                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                                else -> Unit
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            adView.destroy()
                        }
                    }

                    if (adState.bannerStatus != AdLoadStatus.READY) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdsClick,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = adState.bannerMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    AndroidView(
                        factory = { adView },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdMobConsoleSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adState by AdManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CobaltAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AdsClick,
                                contentDescription = "AdMob Console",
                                tint = CobaltAccent
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AdMob Monetization Hub",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (AdMobConfig.isUsingTestAdUnits) {
                                "Google Test Ads Mode (Safe for testing)"
                            } else {
                                "Live Production Ad Units Active"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (AdMobConfig.isUsingTestAdUnits) AmberAccent else EmeraldAccent
                        )
                    }
                }
            }

            // Summary metrics card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = adState.sdkInitSummary,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (adState.sdkInitialized) EmeraldAccent else AmberAccent
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${adState.totalImpressions} Impr.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    AdFormatStatusRow("Adaptive Banner", adState.bannerStatus, adState.bannerMessage)
                    AdFormatStatusRow("Interstitial Ad", adState.interstitialStatus, adState.interstitialMessage)
                    AdFormatStatusRow("Rewarded Video", adState.rewardedStatus, adState.rewardedMessage)
                }
            }

            // Rewarded Ad / Ad-Free Pass card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (adState.isAdFreeActive) {
                        EmeraldAccent.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (adState.isAdFreeActive) EmeraldAccent.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (adState.isAdFreeActive) {
                                Icons.Rounded.Verified
                            } else {
                                Icons.Rounded.WorkspacePremium
                            },
                            contentDescription = null,
                            tint = if (adState.isAdFreeActive) EmeraldAccent else AmberAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (adState.isAdFreeActive) {
                                "Ad-Free Pass Active (${adState.adFreeRemainingMinutes}m left)"
                            } else {
                                "Unlock 2-Hour Ad-Free Experience"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (adState.isAdFreeActive) {
                            "Banners and automatic interstitials are paused. You can still test ads anytime below."
                        } else {
                            "Users can watch 1 Rewarded Video Ad to pause banners and interstitials for 2 hours."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                AdManager.showRewarded(
                                    activity = activity,
                                    onRewardEarned = {
                                        feedbackMessage = "Reward granted! 2-hour Ad-Free Pass activated."
                                    },
                                    onUnavailable = { reason ->
                                        feedbackMessage = reason
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_rewarded_ad_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Watch Rewarded")
                        }
                        if (adState.isAdFreeActive) {
                            OutlinedButton(
                                onClick = {
                                    AdManager.clearAdFreePass(context)
                                    feedbackMessage = "Ad-Free Pass cleared. Banner resumed."
                                },
                                modifier = Modifier.testTag("resume_ads_button")
                            ) {
                                Text("Resume Ads")
                            }
                        }
                    }
                }
            }

            // Live Ad Testing buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        AdManager.showInterstitial(
                            activity = activity,
                            forceBypassCooldown = true
                        ) {
                            feedbackMessage = "Interstitial cycle completed."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_interstitial_button")
                ) {
                    Icon(Icons.Rounded.AdsClick, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Interstitial")
                }
                OutlinedButton(
                    onClick = {
                        AdManager.loadInterstitial(context)
                        AdManager.loadRewarded(context)
                        feedbackMessage = "Refreshing ad inventory..."
                    },
                    modifier = Modifier.testTag("reload_ads_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reload")
                }
            }

            if (feedbackMessage != null) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Developer quick-swap guide
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "How to add your own AdMob IDs",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "1. Put your AdMob App ID in strings.xml (admob_app_id).\n" +
                            "2. Put your Banner, Interstitial & Rewarded Unit IDs in AdMobConfig.kt.\n" +
                            "3. Build release APK/AAB & publish on Google Play.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdFormatStatusRow(
    label: String,
    status: AdLoadStatus,
    message: String
) {
    val (icon, tint) = when (status) {
        AdLoadStatus.READY -> Icons.Rounded.CheckCircle to EmeraldAccent
        AdLoadStatus.LOADING -> Icons.Rounded.HourglassTop to AmberAccent
        AdLoadStatus.FAILED -> Icons.Rounded.ErrorOutline to RoseAccent
        AdLoadStatus.IDLE -> Icons.Rounded.HourglassTop to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = tint
        )
    }
}
