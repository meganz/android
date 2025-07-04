package mega.privacy.android.app.main.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.shared.original.core.ui.controls.ads.AdsCloseIcon
import mega.privacy.mobile.analytics.event.AdsBannerCloseAdsButtonPressedEvent
import timber.log.Timber

/**
 * Container for the Ads.
 */
@Composable
fun AdsContainer(
    request: AdManagerAdRequest?,
    modifier: Modifier = Modifier,
    isLoggedInUser: Boolean = true,
    viewModel: AdsContainerViewModel = hiltViewModel(),
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    var showFreeAdsDialog by rememberSaveable { mutableStateOf(false) }
    var handledState by remember { mutableStateOf(Lifecycle.State.INITIALIZED) }
    var handledRequest by remember { mutableStateOf<AdManagerAdRequest?>(null) }
    var adLoaded by remember { mutableStateOf(false) }
    if (request != null) {
        Box(modifier = modifier) {
            AndroidView(modifier = Modifier.align(Alignment.Center), factory = { context ->
                AdManagerAdView(context).apply {
                    adUnitId = BuildConfig.AD_UNIT_ID
                    setAdSize(AdSize(320, 50))
                    adListener = object : AdListener() {
                        override fun onAdClicked() {
                            Timber.d("Ad clicked")
                        }

                        override fun onAdClosed() {
                            Timber.i("Ad closed")
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Timber.w("Ad failed to load: ${adError.message} (${adError.code})")
                            onAdFailedToLoad()
                        }

                        override fun onAdImpression() {
                            Timber.i("Ad impression")
                        }

                        override fun onAdLoaded() {
                            Timber.i("Ad loaded")
                            adLoaded = true
                            onAdLoaded()
                        }

                        override fun onAdOpened() {
                            Timber.i("Ad opened")
                        }
                    }
                }
            }, update = {
                // update called many times when recomposition, so we need to check if the request and state are changed
                if (handledRequest != request) {
                    it.loadAd(request)
                    handledRequest = request
                }
                if (handledState != currentLifecycleState) {
                    when (currentLifecycleState) {
                        Lifecycle.State.DESTROYED -> {
                            Timber.d("Destroying AdView")
                            it.destroy()
                        }

                        Lifecycle.State.RESUMED -> {
                            Timber.d("Resuming AdView")
                            it.resume()
                        }

                        Lifecycle.State.STARTED -> {
                            Timber.d("Pausing AdView")
                            it.pause()
                        }

                        else -> Unit
                    }
                    handledState = currentLifecycleState
                }
            })

            if (adLoaded && isLoggedInUser) {
                AdsCloseIcon(modifier = Modifier.align(Alignment.TopEnd), onClick = {
                    showFreeAdsDialog = true
                    viewModel.handleAdsClosed()
                    Analytics.tracker.trackEvent(AdsBannerCloseAdsButtonPressedEvent)
                })
            }

            if (showFreeAdsDialog) {
                AdsFreeIntroView(onDismiss = {
                    showFreeAdsDialog = false
                })
            }
        }
    }
}