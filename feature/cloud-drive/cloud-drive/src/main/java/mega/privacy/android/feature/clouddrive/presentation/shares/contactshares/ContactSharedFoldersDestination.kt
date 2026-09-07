package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ContactSharedFoldersNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

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

        val sharesState = uiState.shares
        val isInSelectionMode = sharesState.isInSelectionMode
        val selectedItemsCount = sharesState.selectedItemsCount

        BackHandler(enabled = isInSelectionMode) {
            viewModel.processAction(IncomingSharesAction.DeselectAllItems)
        }

        EventEffect(
            nodeActionState.actionTriggeredEvent,
            nodeOptionsActionViewModel::resetActionTriggered
        ) {
            viewModel.processAction(IncomingSharesAction.DeselectAllItems)
        }

        LaunchedEffect(selectedItemsCount) {
            nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
                selectedNodes = (if (isInSelectionMode) sharesState.selectedNodes else emptyList()).toSet(),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )
        }

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
            onSelectAllClicked = { viewModel.processAction(IncomingSharesAction.SelectAllItems) },
            onCancelSelectionClicked = { viewModel.processAction(IncomingSharesAction.DeselectAllItems) },
            onIncomingShareAction = viewModel::processAction,
            setSortOrder = viewModel::setSortOrder,
        )
    }
}
