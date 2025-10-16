package mega.privacy.android.navigation.contract

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

interface MainNavItem {
    val destination: NavKey
    val screen: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, navigationUiController: NavigationUiController, transferHandler: TransferHandler) -> Unit
    val icon: ImageVector
    val selectedIcon: ImageVector?
    val badge: Flow<MainNavItemBadge?>?
    val label: Int
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
    val analyticsEventIdentifier: NavigationEventIdentifier
}

sealed interface MainNavItemBadge {
    val count: Int
    val priority: Int? get() = null

    interface NumberBadge : MainNavItemBadge {
        val number: Int
    }

    interface TextBadge : MainNavItemBadge {
        val text: String
    }

    interface IconBadge : MainNavItemBadge {
        val icon: ImageVector
    }
}

data class DefaultNumberBadge(override val number: Int) : MainNavItemBadge.NumberBadge {
    override val count = number
}

data class DefaultTextBadge(override val text: String) : MainNavItemBadge.TextBadge {
    override val count = 1
}

data class DefaultIconBadge(
    override val icon: ImageVector,
    override val priority: Int = 1,
) : MainNavItemBadge.IconBadge {
    override val count = 1
}

