package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.Flow

interface MainNavItem {
    val destination: Any
    val screen: NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit
    val iconRes: Int
    val badge: Flow<String?>?
    val label: String
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
}

