package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey

/**
 * Entry for Cloud Drive Screen
 * @param navigationHandler Navigation handler to handle navigation actions
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onTransfer Callback to handle transfer events
 * @param setNavigationBarVisibility Optional callback to set the visibility of the bottom navigation bar, used in HomeScreens
 */
fun EntryProviderScope<NavKey>.cloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    setNavigationBarVisibility: (Boolean) -> Unit = { },
) {
    entry<CloudDriveNavKey> { key ->
        val viewModel = hiltViewModel<CloudDriveViewModel, CloudDriveViewModel.Factory>(
            creationCallback = { factory ->
                val args = CloudDriveViewModel.Args(
                    currentFolderId = NodeId(key.nodeHandle),
                    title = LocalizedText.Literal(key.nodeName ?: ""),
                    nodeSourceType = key.nodeSourceType,
                    highlightedNodeId = key.highlightedNodeHandle?.let { NodeId(it) },
                    highlightedNodeNames = key.highlightedNodeNames,
                )
                factory.create(args)
            }
        )
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
        )

        CloudDriveScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onBack = onBack,
            onTransfer = onTransfer,
            setNavigationBarVisibility = setNavigationBarVisibility,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigateToCloudDriveFolder = { folder, nodeSourceType ->
                navigationHandler.navigate(
                    CloudDriveNavKey(
                        nodeHandle = folder.id.longValue,
                        nodeName = folder.name,
                        nodeSourceType = nodeSourceType,
                    )
                )
            }
        )
    }
}