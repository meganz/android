package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
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
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.FOLDER_LINK) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
        )

        ViewedLinksScreen(
            uiState = uiState,
            onFolderLinkClicked = { link ->
                navigationHandler.navigate(FolderLinkNavKey(link))
            },
            onFileLinkClicked = { link ->
                navigationHandler.navigate(LegacyFileLinkNavKey(link))
            },
            onMenuClicked = { item ->
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = item.viewedLink.nodeHandle,
                        nodeSourceType = NodeSourceType.FOLDER_LINK,
                        publicLinkUrl = item.viewedLink.linkUrl
                            .takeIf { item.viewedLink.type == RecentlyUsedType.FileLink },
                    )
                )
            },
            onClearAllLinks = viewModel::clearAllLinks,
            onBack = navigationHandler::back,
        )
    }
}
