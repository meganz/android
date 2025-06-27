package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.Flow
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

interface MainNavItem {
    val destination: Any
    val screen: NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit
    val iconRes: Int
    val badge: Flow<String?>?
    val label: String
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
    val analyticsEventIdentifier: NavigationEventIdentifier
}

