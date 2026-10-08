package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDriveViewModel
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.FolderBrowserViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.mobile.analytics.event.DriveSyncScreenEvent


fun EntryProviderScope<NavKey>.driveSyncScreen(
    navigationHandler: NavigationHandler,
    setNavigationVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<DriveSyncNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(DriveSyncScreenEvent)
        }
    ) { key ->
        val viewModel = hiltViewModel<DriveSyncViewModel>()
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
        )

        val navigateToCloudDriveFolder = { folder: TypedFolderNode, source: NodeSourceType ->
            navigationHandler.navigate(
                CloudDriveNavKey(
                    nodeHandle = folder.id.longValue,
                    nodeName = folder.name,
                    nodeSourceType = source,
                )
            )
        }

        FeatureFlagGate(
            feature = ApiFeatures.NodeListPagination,
            disabled = {
                val cloudDriveViewModel =
                    hiltViewModel<CloudDriveViewModel, CloudDriveViewModel.Factory> { factory ->
                        factory.create(
                            CloudDriveViewModel.Args(
                                currentFolderId = NodeId(-1L),
                                title = LocalizedText.Literal(""),
                                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                                highlightedNodeId = key.highlightedNodeHandle
                                    ?.let { NodeId(it) },
                                highlightedNodeNames = null,
                            )
                        )
                    }

                LegacyDriveSyncScreen(
                    navigationHandler = navigationHandler,
                    viewModel = viewModel,
                    cloudDriveViewModel = cloudDriveViewModel,
                    setNavigationBarVisibility = setNavigationVisibility,
                    onTransfer = onTransfer,
                    initialTabIndex = key.initialTabIndex,
                    nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                    navigateToCloudDriveFolder = navigateToCloudDriveFolder,
                )
            },
        ) {
            val folderBrowserViewModel =
                hiltViewModel<FolderBrowserViewModel, FolderBrowserViewModel.Factory> { factory ->
                    factory.create(
                        FolderBrowserViewModel.Args(
                            currentFolderId = NodeId(-1L),
                            title = LocalizedText.Literal(""),
                            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                            highlightedNodeId = key.highlightedNodeHandle?.let { NodeId(it) },
                            highlightedNodeNames = null,
                        )
                    )
                }

            DriveSyncScreen(
                navigationHandler = navigationHandler,
                viewModel = viewModel,
                folderBrowserViewModel = folderBrowserViewModel,
                setNavigationBarVisibility = setNavigationVisibility,
                onTransfer = onTransfer,
                initialTabIndex = key.initialTabIndex,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                navigateToCloudDriveFolder = navigateToCloudDriveFolder,
            )
        }
    }
}
