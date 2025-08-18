package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheet
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDrive
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.cloudDriveScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class CloudDriveFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            cloudDriveScreen(
                navigationHandler = navigationHandler,
                onBack = navigationHandler::back,
                onTransfer = transferHandler::setTransferEvent,
                onNavigateToFolder = { nodeId, name ->
                    navigationHandler.navigate(
                        CloudDrive(
                            nodeHandle = nodeId.longValue,
                            nodeName = name
                        )
                    )
                },
                onCreatedNewFolder = { nodeId ->
                    navigationHandler.navigate(
                        CloudDrive(
                            nodeHandle = nodeId.longValue,
                            isNewFolder = true
                        )
                    )
                },
                openNodeOptions = { nodeId ->
                    navigationHandler.navigate(
                        NodeOptionsBottomSheet(
                            nodeHandle = nodeId.longValue,
                            nodeSourceType = NodeSourceType.CLOUD_DRIVE
                        )
                    )
                },
            )
        }
}