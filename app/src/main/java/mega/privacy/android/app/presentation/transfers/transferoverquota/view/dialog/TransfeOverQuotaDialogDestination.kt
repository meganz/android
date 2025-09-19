package mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginScreen
import mega.privacy.android.app.presentation.transfers.transferoverquota.TransferOverQuotaViewModel
import mega.privacy.android.feature.payment.UpgradeAccount
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object TransferOverQuotaDialog : NavKey

data object TransferOverQuotaDialogDestinations : AppDialogDestinations {
    override val navigationGraph: NavGraphBuilder.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            transferOverQuotaDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun NavGraphBuilder.transferOverQuotaDialogDestination(
    navigateBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    dialog<TransferOverQuotaDialog> {
        val viewModel = hiltViewModel<TransferOverQuotaViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TransferOverQuotaDialog(
            uiState = uiState,
            navigateToUpgradeAccount = {
                navigate(UpgradeAccount())
            },
            navigateToLogin = {
                navigate(LoginScreen)
            },
            onDismiss = {
                viewModel.bandwidthOverQuotaDelayConsumed()
                onDialogHandled()
                navigateBack()
            },
        )
    }
}