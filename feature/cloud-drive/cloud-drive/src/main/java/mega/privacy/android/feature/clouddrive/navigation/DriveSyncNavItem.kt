package mega.privacy.android.feature.clouddrive.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.clouddrive.presentation.drivesync.DriveSync
import mega.privacy.android.feature.clouddrive.presentation.drivesync.driveSyncScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SearchNodeNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.CloudDriveBottomNavigationItemEvent

class DriveSyncNavItem : MainNavItem {
    override val destination: NavKey = DriveSync
    override val screen: EntryProviderScope<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, navigationController, transferHandler ->
            driveSyncScreen(
                navigationHandler = navigationHandler,
                setNavigationVisibility = navigationController::showNavigation,
                onTransfer = transferHandler::setTransferEvent,
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
        }

    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Folder
    override val selectedIcon: ImageVector? = null
    override val badge: Flow<String?>? = null

    @StringRes
    override val label: Int = sharedR.string.general_section_cloud_drive
    override val preferredSlot: PreferredSlot = PreferredSlot.Ordered(1)
    override val availableOffline: Boolean = false
    override val analyticsEventIdentifier: NavigationEventIdentifier =
        CloudDriveBottomNavigationItemEvent
}

