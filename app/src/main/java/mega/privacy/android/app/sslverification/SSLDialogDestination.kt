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
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.WebSiteNavKey

@Serializable
data object SSLErrorDialog : DialogNavKey

data object SSLAppDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            sslDialogDestination(
                remove = navigationHandler::remove,
                navigateAndClear = navigationHandler::navigateAndClearTo,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.sslDialogDestination(
    remove: (NavKey) -> Unit,
    navigateAndClear: (NavKey, NavKey, Boolean) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<SSLErrorDialog>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val viewModel = hiltViewModel<SSLErrorViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        when (val state = uiState) {
            SSLDialogState.Loading -> {}
            is SSLDialogState.Ready -> {
                SSLErrorDialog(
                    closeDialog = {
                        onDialogHandled()
                        remove(key)
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

