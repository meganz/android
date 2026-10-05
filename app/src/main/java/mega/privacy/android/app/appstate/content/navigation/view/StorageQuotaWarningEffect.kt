package mega.privacy.android.app.appstate.content.navigation.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mega.privacy.android.app.appstate.content.navigation.StorageStatusViewModel
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.destination.QuotaWarningUpgradeNavKey
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType

/**
 * Shows the storage quota warning whenever [StorageStatusViewModel] raises one.
 *
 * Must stay hosted at the activity level. A warning is dropped when nothing is collecting, and
 * screens are torn down on navigating to a full-screen destination — Transfers among them, which is
 * exactly where the user waits for the upload that raises the warning.
 *
 * @param navigationHandler the outer navigation handler used to open the warning
 */
@Composable
fun StorageQuotaWarningEffect(navigationHandler: NavigationHandler) {
    val viewModel = hiltViewModel<StorageStatusViewModel>()

    LaunchedEffect(Unit) {
        viewModel.quotaWarnings.collect { warning ->
            navigationHandler.navigate(
                QuotaWarningUpgradeNavKey(
                    type = QuotaWarningType.Storage,
                    trigger = when (warning.trigger) {
                        StorageQuotaWarningTrigger.LoginOrReload -> QuotaWarningTrigger.General
                        StorageQuotaWarningTrigger.UploadSuccess -> QuotaWarningTrigger.Upload
                    },
                ),
                navOptions { dropIfAlreadyShown = true },
            )
            viewModel.onQuotaWarningShown(warning)
        }
    }
}
