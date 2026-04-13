package mega.privacy.android.shared.ads.rewarded

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.launch
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.shared.ads.BuildConfig
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Handler for gating actions behind a rewarded ad.
 *
 * The pending action lambda is held here in the Compose scope,
 * not in the ViewModel, to avoid stale references after config changes.
 *
 * @see rememberRewardedAdGate
 */
class RewardedAdGateHandler(
    private val showDialog: () -> Unit,
) {
    private var pendingAction: (() -> Unit)? = null

    /**
     * Gate an action behind a rewarded ad dialog.
     * The action is deferred until the user watches the ad.
     */
    fun requestAction(action: () -> Unit) {
        pendingAction = action
        showDialog()
    }

    internal fun cancelPendingAction() {
        pendingAction = null
    }

    internal fun executeAndReset() {
        val action = pendingAction
        pendingAction = null
        runCatching { action?.invoke() }
            .onFailure {
                Timber.e(it, "Pending action failed, likely stale reference after config change")
            }
    }
}

/**
 * Sets up a Rewarded Ad Gate and places the dialog in the composition tree.
 *
 * Returns a [RewardedAdGateHandler] so the caller can trigger it via [RewardedAdGateHandler.requestAction].
 *
 * @param onNavigate Called to navigate to a destination (e.g., upgrade account).
 */
@Composable
fun rememberRewardedAdGate(
    onNavigate: (NavKey) -> Unit,
): RewardedAdGateHandler {
    val viewModel: RewardedAdGateViewModel = hiltViewModel()
    val handler = remember(viewModel) { RewardedAdGateHandler(viewModel::showDialog) }

    RewardedAdGate(
        viewModel = viewModel,
        handler = handler,
        onNavigate = onNavigate,
    )

    // Dismiss dialog on config change so stale pendingAction is never invoked
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                handler.cancelPendingAction()
                viewModel.dismiss()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return handler
}

@Composable
private fun RewardedAdGate(
    viewModel: RewardedAdGateViewModel,
    handler: RewardedAdGateHandler,
    onNavigate: (NavKey) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()

    if (state.showDialog) {
        RewardedAdDialog(
            isAdLoading = state.isLoading,
            onDismiss = viewModel::dismiss,
            onWatchAd = {
                if (!state.isLoading && activity != null) {
                    val onComplete = {
                        viewModel.dismiss()
                        handler.executeAndReset()
                    }
                    loadAndShowRewardedAd(
                        activity = activity,
                        onLoading = viewModel::setLoading,
                        onLoadingComplete = viewModel::setLoadingComplete,
                        onAdUnavailable = {
                            coroutineScope.launch {
                                Toast.makeText(
                                    activity,
                                    activity.getString(sharedR.string.rewarded_ad_unavailable_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onComplete()
                        },
                        onRewardEarned = onComplete,
                    )
                }
            },
            onUpgradePro = {
                handler.cancelPendingAction()
                viewModel.dismiss()
                onNavigate(UpgradeAccountNavKey(source = UpgradeAccountSource.ADS_FREE_SCREEN))
            }
        )
    }
}

private fun loadAndShowRewardedAd(
    activity: Activity,
    onLoading: () -> Unit,
    onLoadingComplete: () -> Unit,
    onAdUnavailable: () -> Unit,
    onRewardEarned: () -> Unit,
) {
    onLoading()
    val activityRef = WeakReference(activity)
    val adRequest = AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build()

    RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
        override fun onAdLoaded(ad: RewardedAd) {
            Timber.i("Rewarded ad loaded")
            onLoadingComplete()

            val activityInstance = activityRef.get()
            if (activityInstance == null || activityInstance.isFinishing || activityInstance.isDestroyed) {
                Timber.w("Activity is no longer valid, letting user continue")
                onAdUnavailable()
                return
            }

            ad.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Timber.d("Rewarded ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    Timber.e("Rewarded ad failed to show: ${fullScreenContentError.message}")
                    onAdUnavailable()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.d("Rewarded ad showed full screen")
                }

                override fun onAdImpression() {
                    Timber.d("Rewarded ad impression")
                }

                override fun onAdClicked() {
                    Timber.d("Rewarded ad clicked")
                }
            }

            ad.show(activityInstance) { reward ->
                Timber.i("User earned reward: ${reward.amount} ${reward.type}")
                onRewardEarned()
            }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            Timber.e("Rewarded ad failed to load: ${adError.message} (${adError.code})")
            onAdUnavailable()
        }
    })
}
