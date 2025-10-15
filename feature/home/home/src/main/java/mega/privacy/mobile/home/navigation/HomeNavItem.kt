package mega.privacy.mobile.home.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.HomeBottomNavigationItemEvent
import mega.privacy.mobile.home.presentation.home.Home
import mega.privacy.mobile.home.presentation.home.homeScreen

class HomeNavItem : MainNavItem {
    override val destination: NavKey = Home
    override val screen: EntryProviderScope<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, navigationController, transferHandler ->
            homeScreen(
                navigationHandler = navigationHandler,
                onTransfer = transferHandler::setTransferEvent,
            )
        }
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Home
    override val selectedIcon: ImageVector = IconPack.Medium.Thin.Solid.Home
    override val badge: Flow<String?>? = null
    override val label: Int = sharedR.string.general_section_home
    override val preferredSlot: PreferredSlot = PreferredSlot.Ordered(0)
    override val availableOffline: Boolean = true
    override val analyticsEventIdentifier: NavigationEventIdentifier = HomeBottomNavigationItemEvent
}