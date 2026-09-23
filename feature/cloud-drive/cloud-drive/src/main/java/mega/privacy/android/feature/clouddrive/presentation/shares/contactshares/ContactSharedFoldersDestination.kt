package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionActionsEffect
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ContactSharedFoldersNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.selection.rememberNodeSelection

fun EntryProviderScope<NavKey>.contactSharedFolders(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<ContactSharedFoldersNavKey> { key ->
        val viewModel =
            hiltViewModel<ContactSharedFoldersViewModel, ContactSharedFoldersViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(ContactSharedFoldersViewModel.Args(email = key.email))
                }
            )
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )


        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
        )

        val megaNavigator = rememberMegaNavigator()
        val selectionModeActionHandler = rememberMultiNodeActionHandler(
            navigationHandler = navigationHandler,
            viewModel = nodeOptionsActionViewModel,
            megaNavigator = megaNavigator
        )

        val selection = rememberNodeSelection(items = uiState.shares.items)

        BackHandler(enabled = selection.isInSelectionMode) {
            selection.deselectAll()
        }

        @SuppressLint("ComposeViewModelForwarding")
        NodeSelectionActionsEffect(
            selection = selection,
            nodeSourceType = NodeSourceType.INCOMING_SHARES,
            uiState = nodeActionState,
            onIntent = nodeOptionsActionViewModel::processIntent,
        )

        ContactSharedFoldersScreen(
            email = key.email,
            navigationHandler = navigationHandler,
            onBack = navigationHandler::back,
            onNavigateToTransfers = { navigationHandler.navigate(TransfersNavKey()) },
            onShowNodeOptions = { nodeId ->
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = nodeId.longValue,
                        nodeSourceType = NodeSourceType.INCOMING_SHARES,
                        partiallyExpand = false
                    )
                )
            },
            onTransfer = transferHandler::setTransferEvent,
            uiState = uiState,
            nodeActionState = nodeActionState,
            selectionModeActionHandler = selectionModeActionHandler,
            selection = selection,
            onIncomingShareAction = viewModel::processAction,
            setSortOrder = viewModel::setSortOrder,
        )
    }
}
