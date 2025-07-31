package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CloudDrive
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.cloudDriveScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class CloudDriveFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            cloudDriveScreen(
                onBack = navigationHandler::back,
                onTransfer = transferHandler::setTransferEvent,
                onNavigateToFolder = { nodeId ->
                    navigationHandler.navigate(CloudDrive(nodeHandle = nodeId.longValue))
                }
            )
        }
}