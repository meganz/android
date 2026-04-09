package mega.privacy.android.shared.ads.rewarded

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.VERTICAL
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.shared.ads.BuildConfig
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

    return handler
}

@Composable
private fun RewardedAdGate(
    viewModel: RewardedAdGateViewModel,
    handler: RewardedAdGateHandler,
    onNavigate: (NavKey) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.showDialog && !state.isLoading) return

    // TODO strings and design will be confirmed later
    val activity = LocalActivity.current
    val dialogBody = state.errorMessage
        ?: "You can watch a short ad to keep going,\nor go Pro for unlimited access with no interruptions."

    BasicDialog(
        title = "Unlock more with a quick ad",
        description = dialogBody,
        buttons = persistentListOf(
            BasicDialogButton(
                text = if (state.isLoading) "Watch ad…" else "Watch ad",
                onClick = {
                    if (!state.isLoading && activity != null) {
                        loadAndShowRewardedAd(
                            activity = activity,
                            onLoading = viewModel::setLoading,
                            onLoadingComplete = viewModel::setLoadingComplete,
                            onError = viewModel::setError,
                            onDismiss = viewModel::dismiss,
                            onRewardEarned = {
                                viewModel.dismiss()
                                handler.executeAndReset()
                            },
                        )
                    }
                }
            ),
            BasicDialogButton(
                text = "Go Pro",
                onClick = {
                    handler.cancelPendingAction()
                    viewModel.dismiss()
                    onNavigate(UpgradeAccountNavKey(source = UpgradeAccountSource.ADS_FREE_SCREEN))
                }
            ),
            BasicDialogButton(
                text = "Not now",
                onClick = {
                    handler.cancelPendingAction()
                    viewModel.dismiss()
                }
            )
        ),
        onDismissRequest = {
            handler.cancelPendingAction()
            viewModel.dismiss()
        },
        buttonDirection = VERTICAL
    )
}

private fun loadAndShowRewardedAd(
    activity: Activity,
    onLoading: () -> Unit,
    onLoadingComplete: () -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
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
                Timber.w("Activity is no longer valid, discarding loaded ad")
                onDismiss()
                return
            }

            ad.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Timber.d("Rewarded ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    Timber.e("Rewarded ad failed to show: ${fullScreenContentError.message}")
                    onError(fullScreenContentError.message)
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
            onError(adError.message)
        }
    })
}
