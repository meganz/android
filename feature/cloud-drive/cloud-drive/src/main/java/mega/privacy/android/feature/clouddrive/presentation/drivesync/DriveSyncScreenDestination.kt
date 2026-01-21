package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey


fun EntryProviderScope<NavKey>.driveSyncScreen(
    navigationHandler: NavigationHandler,
    setNavigationVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<DriveSyncNavKey> { key ->
        val viewModel = hiltViewModel<DriveSyncViewModel>()
        val cloudDriveViewModel = hiltViewModel<CloudDriveViewModel, CloudDriveViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(CloudDriveNavKey(highlightedNodeHandle = key.highlightedNodeHandle))
            }
        )
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        DriveSyncScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            cloudDriveViewModel = cloudDriveViewModel,
            setNavigationBarVisibility = setNavigationVisibility,
            onTransfer = onTransfer,
            initialTabIndex = key.initialTabIndex,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )
    }
}
