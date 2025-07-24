package mega.privacy.android.navigation.contract

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

interface MainNavItem {
    val destination: NavKey
    val screen: NavGraphBuilder.(navigationHandler: NavigationHandler, navigationUiController: NavigationUiController) -> Unit
    val icon: ImageVector
    val badge: Flow<String?>?
    val label: Int
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
    val analyticsEventIdentifier: NavigationEventIdentifier
}

