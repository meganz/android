package mega.privacy.android.feature.clouddrive.presentation.favourites

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.FavouritesNavKey

fun EntryProviderScope<NavKey>.favouritesScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeResultFlow: (String) -> Flow<NodeOptionsBottomSheetResult?>,
    clearResultFlow: (String) -> Unit,
) {
    entry<FavouritesNavKey> {
        val viewModel = hiltViewModel<FavouritesViewModel>()
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = nodeResultFlow,
            clearResultFlow = clearResultFlow,
        )

        FavouritesScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onTransfer = onTransfer,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )
    }
}
