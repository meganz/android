package mega.privacy.android.app.appstate.global.quota

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.destination.QuotaWarningUpgradeNavKey
import mega.privacy.android.navigation.payment.QuotaWarningType

/**
 * Shows the transfer quota-warning upsell screen when a download or media streaming hits bandwidth
 * over quota, for a host owning a navigation back stack.
 *
 * Transfers can be started from any activity, so each host reacts only while resumed and the
 * foreground one handles the warning in its own back stack.
 */
@Composable
fun TransferOverQuotaWarningEffect(navigationHandler: NavigationHandler) {
    HandleTransferOverQuota { source ->
        navigationHandler.navigate(
            QuotaWarningUpgradeNavKey(
                type = QuotaWarningType.Transfer,
                trigger = source.quotaWarningTrigger,
            ),
            navOptions { dropIfAlreadyShown = true },
        )
    }
}

@Composable
private fun HandleTransferOverQuota(showWarning: (TransferOverQuotaSource) -> Unit) {
    val viewModel = hiltViewModel<TransferOverQuotaWarningViewModel>()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Not keyed on the lambda: it captures an unstable navigation handler, so it is a new
    // instance on every recomposition and would restart the collection each time.
    val currentShowWarning by rememberUpdatedState(showWarning)
    LaunchedEffect(lifecycle) {
        viewModel.transferOverQuotaEvents
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            .collect {
                viewModel.consumeTransferOverQuotaEvent()?.let(currentShowWarning)
            }
    }
}
