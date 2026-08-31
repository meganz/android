package mega.privacy.android.navigation.contract

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

interface MainNavItem {
    /**
     * Stable, unique key used to persist this item (e.g. user-defined ordering).
     *
     * Once shipped, this value must never change — renaming a feature must not change its id,
     * otherwise stored user preferences referencing it would be lost.
     */
    val id: String
    val destination: MainNavItemNavKey
    val screen: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, navigationUiController: NavigationUiController, transferHandler: TransferHandler) -> Unit
    val icon: ImageVector
    val selectedIcon: ImageVector?
    val badge: Flow<MainNavItemBadge?>?
    val label: Int
    val preferredSlot: PreferredSlot
    val availableOffline: Boolean
    val analyticsEventIdentifier: NavigationEventIdentifier

    /**
     * Analytics identifier tracked when this section is opened from a Menu drawer row rather
     * than the navigation bar. Defaults to [analyticsEventIdentifier] for items without a
     * dedicated drawer event.
     */
    val drawerAnalyticsEventIdentifier: NavigationEventIdentifier
        get() = analyticsEventIdentifier
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

/**
 * Sorts the items by their default [PreferredSlot] order.
 *
 * [PreferredSlot.Ordered] items are sorted by slot number, followed by the [PreferredSlot.Last]
 * item. [PreferredSlot.None] items have no default slot and are excluded.
 */
fun Iterable<MainNavItem>.sortedByPreferredSlot(): List<MainNavItem> {
    return filterNot { it.preferredSlot is PreferredSlot.None }
        .sortedWith(
            compareBy { navItem ->
                when (val slot = navItem.preferredSlot) {
                    is PreferredSlot.Ordered -> slot.slot
                    is PreferredSlot.Last -> Int.MAX_VALUE
                    is PreferredSlot.None -> Int.MAX_VALUE
                }
            }
        )
}

/**
 * Orders the items according to a user-defined list of item ids.
 *
 * Items whose [MainNavItem.id] appears in [orderedIds] come first, following the order of
 * [orderedIds]. Any remaining items follow in [PreferredSlot] order, which excludes
 * [PreferredSlot.None] items — they are only included when their id appears in [orderedIds].
 * The item with [PreferredSlot.Last] is always pinned to the very end, even if its id appears
 * in [orderedIds].
 *
 * @param orderedIds the user-defined ordering of item ids; ids not matching any item are ignored
 */
fun Iterable<MainNavItem>.orderedByUserPreference(orderedIds: List<String>): List<MainNavItem> {
    val (lastItems, orderableItems) = partition { it.preferredSlot is PreferredSlot.Last }
    val (userOrderedItems, remainingItems) = orderableItems.partition { it.id in orderedIds }
    return userOrderedItems.sortedBy { orderedIds.indexOf(it.id) } +
            remainingItems.sortedByPreferredSlot() +
            lastItems
}