package mega.privacy.android.feature.clouddrive.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.clouddrive.presentation.CloudDriveScreen
import mega.privacy.android.feature.clouddrive.presentation.cloudDriveScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

class CloudDriveMainNavItem : MainNavItem {
    override val destination: Any = CloudDriveScreen
    override val screen: NavGraphBuilder.(NavigationHandler, NavigationUiController) -> Unit =
        { navigationHandler, _ -> cloudDriveScreen(navigationHandler::back) }

    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Folder
    override val badge: Flow<String?>? = null

    @StringRes
    override val label: Int = sharedR.string.general_section_cloud_drive
    override val preferredSlot: PreferredSlot = PreferredSlot.Ordered(1)
    override val availableOffline: Boolean = false
    override val analyticsEventIdentifier: NavigationEventIdentifier =
        CloudDriveNavigationIdentifier
}

object CloudDriveNavigationIdentifier : NavigationEventIdentifier {
    override val navigationElementType: String?
        get() = "CloudDriveNavigation"
    override val destination: String?
        get() = "CloudDrive"
    override val eventName: String
        get() = "CloudDriveScreen"
    override val uniqueIdentifier: Int
        get() = 100
}

