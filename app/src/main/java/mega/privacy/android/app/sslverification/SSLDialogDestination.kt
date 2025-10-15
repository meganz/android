package mega.privacy.android.app.sslverification

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.app.sslverification.model.SSLDialogState
import mega.privacy.android.app.sslverification.view.SSLErrorDialog
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.WebSiteNavKey

@Serializable
data object SSLErrorDialog : NavKey

data object SSLAppDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            sslDialogDestination(
                navigateBack = navigationHandler::back,
                navigateAndClear = navigationHandler::navigateAndClearTo,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<NavKey>.sslDialogDestination(
    navigateBack: () -> Unit,
    navigateAndClear: (NavKey, NavKey, Boolean) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<SSLErrorDialog>(
        metadata = DialogSceneStrategy.dialog()
    ) {
        val viewModel = hiltViewModel<SSLErrorViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        when (val state = uiState) {
            SSLDialogState.Loading -> {}
            is SSLDialogState.Ready -> {
                SSLErrorDialog(
                    closeDialog = {
                        onDialogHandled()
                        navigateBack()
                    },
                    onRetry = viewModel::onRetry,
                    onOpenBrowser = {
                        navigateAndClear(
                            WebSiteNavKey(state.webUrl),
                            SSLErrorDialog,
                            true,
                        )
                    },
                    onDismiss = viewModel::onDismiss,
                )
            }
        }

    }
}

