package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
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
    openSearch: (Long, NodeSourceType) -> Unit,
    nodeResultFlow: (String) -> Flow<NodeOptionsBottomSheetResult?>,
    clearResultFlow: (String) -> Unit,
) {
    entry<DriveSyncNavKey> { key ->
        val viewModel = hiltViewModel<DriveSyncViewModel>()
        val cloudDriveViewModel = hiltViewModel<CloudDriveViewModel, CloudDriveViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(CloudDriveNavKey(highlightedNodeHandle = key.highlightedNodeHandle))
            }
        )
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = nodeResultFlow,
            clearResultFlow = clearResultFlow,
        )

        DriveSyncScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            cloudDriveViewModel = cloudDriveViewModel,
            setNavigationItemVisibility = setNavigationVisibility,
            onTransfer = onTransfer,
            openSearch = openSearch,
            initialTabIndex = key.initialTabIndex,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )
    }
}
