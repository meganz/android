package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface MainNavItem {
    val destinationClass: KClass<*>
    val destination: Any
    val screen: NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit
    val iconRes: Int
    val badge: Flow<String?>?
    val label: String
    val preferredSlot: PreferredSlot
}

