package mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.transfers.transferoverquota.TransferOverQuotaViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

@Serializable
data object TransferOverQuotaDialog : NoSessionNavKey.Optional, DialogNavKey

data object TransferOverQuotaDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            transferOverQuotaDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.transferOverQuotaDialogDestination(
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
                navigate(LoginNavKey())
            },
            onDismiss = {
                viewModel.bandwidthOverQuotaDelayConsumed()
                onDialogHandled()
                navigateBack()
            },
        )
    }
}
