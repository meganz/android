package mega.privacy.android.feature.clouddrive.presentation.favourites

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.FavouritesNavKey

fun EntryProviderScope<NavKey>.favouritesScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<FavouritesNavKey> {
        val viewModel = hiltViewModel<FavouritesViewModel>()
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        FavouritesScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onTransfer = onTransfer,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )

        HandleNodeOptionsResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )
    }
}
