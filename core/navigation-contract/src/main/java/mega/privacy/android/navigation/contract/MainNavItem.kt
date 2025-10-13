package mega.privacy.android.navigation.contract

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

interface MainNavItem {
    val destination: NavKey
    val screen: EntryProviderBuilder<NavKey>.(navigationHandler: NavigationHandler, navigationUiController: NavigationUiController, transferHandler: TransferHandler) -> Unit
    val icon: ImageVector
    val selectedIcon: ImageVector?
    val badge: Flow<String?>?
    val label: Int
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
    val analyticsEventIdentifier: NavigationEventIdentifier
}

