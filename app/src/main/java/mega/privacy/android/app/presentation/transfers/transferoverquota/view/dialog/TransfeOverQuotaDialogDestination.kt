package mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.Login
import mega.privacy.android.app.presentation.transfers.transferoverquota.TransferOverQuotaViewModel
import mega.privacy.android.feature.payment.UpgradeAccountNavKey
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object TransferOverQuotaDialog : NavKey

data object TransferOverQuotaDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            transferOverQuotaDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderBuilder<NavKey>.transferOverQuotaDialogDestination(
    navigateBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<TransferOverQuotaDialog>(
        metadata = DialogSceneStrategy.dialog()
    ) {
        val viewModel = hiltViewModel<TransferOverQuotaViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TransferOverQuotaDialog(
            uiState = uiState,
            navigateToUpgradeAccount = {
                navigate(UpgradeAccountNavKey())
            },
            navigateToLogin = {
                navigate(Login)
            },
            onDismiss = {
                viewModel.bandwidthOverQuotaDelayConsumed()
                onDialogHandled()
                navigateBack()
            },
        )
    }
}