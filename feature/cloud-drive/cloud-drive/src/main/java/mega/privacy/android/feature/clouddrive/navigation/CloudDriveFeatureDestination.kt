package mega.privacy.android.feature.clouddrive.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.cloudDriveScreen
import mega.privacy.android.feature.clouddrive.presentation.drivesync.driveSyncScreen
import mega.privacy.android.feature.clouddrive.presentation.favourites.favouritesScreen
import mega.privacy.android.feature.clouddrive.presentation.offline.offlineScreen
import mega.privacy.android.feature.clouddrive.presentation.rubbishbin.rubbishBin
import mega.privacy.android.feature.clouddrive.presentation.shares.links.openPasswordLinkDialog
import mega.privacy.android.feature.clouddrive.presentation.shares.shares
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey
import mega.privacy.android.navigation.destination.OfflineInfoNavKey
import mega.privacy.android.navigation.destination.OfflineNavKey
import mega.privacy.android.navigation.destination.SearchNodeNavKey

class CloudDriveFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            cloudDriveScreen(
                navigationHandler = navigationHandler,
                onBack = navigationHandler::back,
                onTransfer = transferHandler::setTransferEvent,
                openSearch = { parentHandle, nodeSourceType ->
                    navigationHandler.navigate(
                        SearchNodeNavKey(
                            nodeSourceType = nodeSourceType,
                            parentHandle = parentHandle
                        )
                    )
                },
                nodeResultFlow = navigationHandler::monitorResult,
                clearResultFlow = navigationHandler::clearResult
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
                },
                onTransfer = transferHandler::setTransferEvent,
                openFileInformation = { handle ->
                    navigationHandler.navigate(OfflineInfoNavKey(handle = handle))
                }
            )

            driveSyncScreen(
                navigationHandler = navigationHandler,
                setNavigationVisibility = { /* No-op for FeatureDestination */ },
                onTransfer = transferHandler::setTransferEvent,
                openSearch = { parentHandle, nodeSourceType ->
                    navigationHandler.navigate(
                        SearchNodeNavKey(
                            nodeSourceType = nodeSourceType,
                            parentHandle = parentHandle
                        )
                    )
                },
                nodeResultFlow = navigationHandler::monitorResult,
                clearResultFlow = navigationHandler::clearResult
            )

            favouritesScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
            )

            openPasswordLinkDialog(
                onBack = navigationHandler::back,
                onNavigateToFileLink = { folderLinkUri ->
                    navigationHandler.back() //to dismiss the dialog
                    navigationHandler.navigate(LegacyFileLinkNavKey(folderLinkUri))
                },
                onNavigateToFolderLink = { folderLinkUri ->
                    navigationHandler.back() //to dismiss the dialog
                    navigationHandler.navigate(LegacyFolderLinkNavKey(folderLinkUri))
                },
            )
        }
}