package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.FileLinkNavKey
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.ViewedLinksScreenNavKey

/**
 * Navigation destination for the full-screen Viewed Links list.
 *
 * @param navigationHandler
 * @param transferHandler
 */
fun EntryProviderScope<NavKey>.viewedLinksScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<ViewedLinksScreenNavKey> {
        val viewModel = hiltViewModel<ViewedLinksViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val lazyItems = viewModel.pagedItems.collectAsLazyPagingItems()
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { vm -> vm.create(NodeSourceType.FOLDER_LINK) },
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
        )

        EventEffect(
            event = uiState.clearAllLinksEvent,
            onConsumed = viewModel::onClearAllLinksEventConsumed,
            action = { navigationHandler.remove(it) }
        )

        ViewedLinksScreen(
            uiState = uiState,
            lazyItems = lazyItems,
            onFolderLinkClicked = { link ->
                navigationHandler.navigate(FolderLinkNavKey(link))
            },
            onFileLinkClicked = { link ->
                navigationHandler.navigate(FileLinkNavKey(link))
            },
            onClearAllLinks = viewModel::clearAllLinks,
            onSortOptionSelected = viewModel::updateSortConfiguration,
            onBack = navigationHandler::back,
        )
    }
}
