package mega.privacy.android.feature.photos.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.destination.MediaMainNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.PhotosBottomNavigationItemEvent

class MediaNavItem : MainNavItem {
    override val destination: MainNavItemNavKey = MediaMainNavKey
    override val screen: EntryProviderScope<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, navigationController, transferHandler ->
            mediaMainRoute(navigationHandler = navigationHandler)
        }
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Image01
    override val selectedIcon: ImageVector = IconPack.Medium.Thin.Solid.Image01
    override val badge: Flow<MainNavItemBadge?>? = null
    override val label: Int = sharedR.string.media_feature_title
    override val preferredSlot: PreferredSlot = PreferredSlot.Ordered(2)
    override val availableOffline: Boolean = true
    override val analyticsEventIdentifier: NavigationEventIdentifier =
        PhotosBottomNavigationItemEvent
}