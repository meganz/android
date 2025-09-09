package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialog
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.cloudDriveScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.CloudDrive

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
                onRenameNode = { nodeId ->
                    navigationHandler.navigate(
                        RenameNodeDialog(nodeId = nodeId.longValue)
                    )
                }
            )
        }
}