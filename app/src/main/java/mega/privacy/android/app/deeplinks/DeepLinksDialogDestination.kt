package mega.privacy.android.app.deeplinks

import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.app.deeplinks.view.DeepLinksDialog
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.DeepLinksAfterFetchNodesDialogNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey

data object DeepLinksDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            deepLinkDialogDestination(
                remove = navigationHandler::remove,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
            deepLinkAfterFetchNodesDialogDestination(
                remove = navigationHandler::remove,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.deepLinkDialogDestination(
    remove: (NavKey) -> Unit,
    navigate: (List<NavKey>) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<DeepLinksDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) {
        val viewModel = hiltViewModel<DeepLinksViewModel, DeepLinksViewModel.Factory> { factory ->
            factory.create(args = DeepLinksViewModel.Args(uri = it.deepLink.toUri()))
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        DeepLinksDialog(
            uiState = uiState,
            onNavigate = navigate,
            onDismiss = {
                remove(it)
                onDialogHandled()
            }
        )
    }
}

fun EntryProviderScope<DialogNavKey>.deepLinkAfterFetchNodesDialogDestination(
    remove: (NavKey) -> Unit,
    navigate: (List<NavKey>) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<DeepLinksAfterFetchNodesDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) {
        val viewModel = hiltViewModel<DeepLinksViewModel, DeepLinksViewModel.Factory> { factory ->
            factory.create(
                DeepLinksViewModel.Args(
                    uri = it.deepLink.toUri(),
                    regexPatternType = it.regexPatternType
                )
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        DeepLinksDialog(
            uiState = uiState,
            onNavigate = navigate,
            onDismiss = {
                remove(it)
                onDialogHandled()
            }
        )
    }
}

