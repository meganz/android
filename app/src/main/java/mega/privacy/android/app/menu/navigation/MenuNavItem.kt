package mega.privacy.android.app.menu.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.menu.presentation.MenuHomeScreen
import mega.privacy.android.app.menu.presentation.menuHomeScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.analytics.event.MenuBottomNavigationItemEvent


class MenuNavItem : MainNavItem {
    override val destination: NavKey = MenuHomeScreen
    override val screen: EntryProviderBuilder<NavKey>.(NavigationHandler, NavigationUiController, TransferHandler) -> Unit =
        { navigationHandler, _, _ -> menuHomeScreen(navigationHandler::navigate) }
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Menu01
    override val selectedIcon: ImageVector? = null
    override val badge: Flow<String?>? = null
    override val label: Int = sharedR.string.general_menu
    override val preferredSlot: PreferredSlot = PreferredSlot.Last
    override val availableOffline: Boolean = true
    override val analyticsEventIdentifier: NavigationEventIdentifier = MenuNavigationIdentifier
}

object MenuNavigationIdentifier : NavigationEventIdentifier {
    override val navigationElementType: String?
        get() = MenuBottomNavigationItemEvent.navigationElementType
    override val destination: String?
        get() = MenuBottomNavigationItemEvent.destination
    override val eventName: String
        get() = MenuBottomNavigationItemEvent.eventName
    override val uniqueIdentifier: Int
        get() = MenuBottomNavigationItemEvent.uniqueIdentifier
}
