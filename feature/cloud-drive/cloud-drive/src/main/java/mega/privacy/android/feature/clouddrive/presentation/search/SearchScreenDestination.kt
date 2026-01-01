package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.SearchNavKey

fun EntryProviderScope<NavKey>.searchScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<SearchNavKey> { key ->
        val viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(key)
            }
        )
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        SearchScreen(
            navigationHandler = navigationHandler,
            onBack = onBack,
            onTransfer = onTransfer,
            viewModel = viewModel,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )
    }
}

