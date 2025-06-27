package mega.privacy.android.feature.example.navigation

import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.feature.example.presentation.HomeScreen
import mega.privacy.android.feature.example.presentation.HomeScreen2
import mega.privacy.android.feature.example.presentation.exampleHomeScreen
import mega.privacy.android.feature.example.presentation.otherExampleHomeScreen
import mega.privacy.android.icon.pack.R as IconPack
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

class ExampleMainItem : MainNavItem {
    override val destination: Any = HomeScreen
    override val screen: NavGraphBuilder.(NavigationHandler) -> Unit =
        { navigationHandler -> exampleHomeScreen() }
    override val iconRes: Int = IconPack.drawable.ic_cloud
    override val badge: Flow<String?>? = null
    override val label: String = "Demo"
    override val preferredSlot: PreferredSlot = PreferredSlot.Ordered(1)
    override val availableOffline: Boolean = false
    override val analyticsEventIdentifier: NavigationEventIdentifier = ExampleNavigationIdentifier
}

class OtherExampleMainItem : MainNavItem {
    override val destination: Any = HomeScreen2
    override val screen: NavGraphBuilder.(NavigationHandler) -> Unit =
        { navigationHandler -> otherExampleHomeScreen(navigationHandler::navigate) }
    override val iconRes: Int = IconPack.drawable.ic_vpn
    override val badge: Flow<String?>? = flow {
        var count = 0
        while (true) {
            emit(count.toString())
            kotlinx.coroutines.delay(5000) // Emit "New" every 5 seconds
            count++
        }
    }
    override val label: String = "Other"
    override val preferredSlot: PreferredSlot = PreferredSlot.Last
    override val availableOffline: Boolean = true
    override val analyticsEventIdentifier: NavigationEventIdentifier = ExampleNavigationIdentifier
}

object ExampleNavigationIdentifier : NavigationEventIdentifier {
    override val navigationElementType: String?
        get() = "ExampleNavigation"
    override val destination: String?
        get() = "ExampleNavigation"
    override val eventName: String
        get() = "ExampleNavigation"
    override val uniqueIdentifier: Int
        get() = -1
}

