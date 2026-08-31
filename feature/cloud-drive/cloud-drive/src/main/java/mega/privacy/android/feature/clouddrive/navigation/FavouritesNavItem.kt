package mega.privacy.android.feature.clouddrive.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.clouddrive.presentation.favourites.favouritesScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.destination.FavouritesNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.FavouritesBottomNavigationItemEvent
import mega.privacy.mobile.analytics.event.MyMenuFavouritesNavigationItemEvent

/**
 * Main navigation item for the Favourites section.
 *
 * Has no default slot; it only appears in the navigation bar when the user adds it via the
 * navigation items preference.
 */
class FavouritesNavItem : MainNavItem, Flagged {
    override val id: String = "favourites"
    override val feature: Feature = ApiFeatures.CustomisableBottomNavigation
    override val destination: MainNavItemNavKey = FavouritesNavKey
    override val screen: EntryProviderScope<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, _, transferHandler ->
            favouritesScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
            )
        }

    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Heart
    override val selectedIcon: ImageVector? = IconPack.Medium.Thin.Solid.Heart
    override val badge: Flow<MainNavItemBadge?>? = null

    @StringRes
    override val label: Int = sharedR.string.video_section_title_favourite_playlist
    override val preferredSlot: PreferredSlot = PreferredSlot.None
    override val availableOffline: Boolean = false
    override val analyticsEventIdentifier: NavigationEventIdentifier =
        FavouritesBottomNavigationItemEvent
    override val drawerAnalyticsEventIdentifier: NavigationEventIdentifier =
        MyMenuFavouritesNavigationItemEvent
}

