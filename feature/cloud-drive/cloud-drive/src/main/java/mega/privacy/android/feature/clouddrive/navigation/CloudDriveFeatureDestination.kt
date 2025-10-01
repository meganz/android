package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogNavKey
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.cloudDriveScreen
import mega.privacy.android.feature.clouddrive.presentation.offline.offlineScreen
import mega.privacy.android.feature.clouddrive.presentation.rubbishbin.rubbishBin
import mega.privacy.android.feature.clouddrive.presentation.shares.shares
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.SearchNodeNavKey

class CloudDriveFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            cloudDriveScreen(
                navigationHandler = navigationHandler,
                onBack = navigationHandler::back,
                onTransfer = transferHandler::setTransferEvent,
                onNavigateToFolder = { nodeId, name ->
                    navigationHandler.navigate(
                        CloudDriveNavKey(
                            nodeHandle = nodeId.longValue,
                            nodeName = name
                        )
                    )
                },
                onCreatedNewFolder = { nodeId ->
                    navigationHandler.navigate(
                        CloudDriveNavKey(
                            nodeHandle = nodeId.longValue,
                            isNewFolder = true
                        )
                    )
                },
                onRenameNode = { nodeId ->
                    navigationHandler.navigate(
                        RenameNodeDialogNavKey(nodeId = nodeId.longValue)
                    )
                },
                openSearch = { isFirstNavigationLevel, parentHandle, nodeSourceType ->
                    navigationHandler.navigate(
                        SearchNodeNavKey(
                            isFirstNavigationLevel = isFirstNavigationLevel,
                            nodeSourceType = nodeSourceType,
                            parentHandle = parentHandle
                        )
                    )
                }
            )

            rubbishBin(navigationHandler, transferHandler)

            shares(navigationHandler, transferHandler)

            offlineScreen(
                onBack = navigationHandler::back,
                onNavigateToFolder = { parentId, name ->
                    navigationHandler.navigate(
                        OfflineNavKey(
                            nodeId = parentId,
                            title = name,
                        )
                    )
                }
            )
        }
}