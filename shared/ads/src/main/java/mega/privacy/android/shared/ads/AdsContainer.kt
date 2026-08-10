package mega.privacy.android.shared.ads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.delay
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.mobile.analytics.event.AdsBannerCloseAdsButtonPressedEvent
import timber.log.Timber

private const val MOBILE_ADS_INIT_POLL_INTERVAL_MS = 300L

/**
 * Container for the Ads.
 */
@Composable
fun AdsContainer(
    request: BannerAdRequest?,
    modifier: Modifier = Modifier,
    isLoggedInUser: Boolean = true,
    viewModel: AdsContainerViewModel = hiltViewModel(),
    onCloseAds: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    var handledState by remember { mutableStateOf(Lifecycle.State.INITIALIZED) }
    var handledRequest by remember { mutableStateOf<BannerAdRequest?>(null) }
    var adLoaded by remember { mutableStateOf(false) }
    var isMobileAdsInitialized by remember { mutableStateOf(MobileAds.isInitialized) }

    LaunchedEffect(request) {
        if (request != null && !isMobileAdsInitialized) {
            while (!MobileAds.isInitialized) {
                delay(MOBILE_ADS_INIT_POLL_INTERVAL_MS)
            }
            isMobileAdsInitialized = true
        }
    }

    if (request != null && isMobileAdsInitialized) {
        Box(modifier = modifier) {
            AndroidView(modifier = Modifier.align(Alignment.Center), factory = { context ->
                AdView(context)
            }, update = { adView ->
                // update called many times when recomposition, so we need to check if the request and state are changed
                if (handledRequest != request) {
                    adView.loadAd(request, object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            Timber.i("Ad loaded")
                            viewModel.setAdsLoaded(true)
                            adLoaded = true
                            ad.adEventCallback = object : BannerAdEventCallback {
                                override fun onAdClicked() {
                                    Timber.d("Ad clicked")
                                }

                                override fun onAdImpression() {
                                    Timber.i("Ad impression")
                                }
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Timber.w("Ad failed to load: ${error.message} (${error.code})")
                            viewModel.setAdsLoaded(false)
                        }
                    })
                    handledRequest = request
                }
                if (handledState != currentLifecycleState) {
                    when (currentLifecycleState) {
                        Lifecycle.State.DESTROYED -> {
                            Timber.d("Destroying AdView")
                            adView.destroy()
                        }

                        else -> Unit
                    }
                    handledState = currentLifecycleState
                }
            })

            if (adLoaded && isLoggedInUser) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    contentDescription = "Close icon",
                    tint = IconColor.Primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClick = {
                            onCloseAds()
                            viewModel.handleAdsClosed()
                            Analytics.tracker.trackEvent(AdsBannerCloseAdsButtonPressedEvent)
                        })
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
    }
}
