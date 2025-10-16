package mega.privacy.mobile.navigation.snowflake.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.MainNavItemBadge
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

/**
 * Navigation item
 *
 * @property destination The destination of the navigation item, can be any type.
 * @property isEnabled Indicates whether the navigation item is enabled or not.
 * @property badge Optional badge associated with the navigation item.
 */
@Immutable
data class NavigationItem(
    val destination: NavKey,
    val icon: ImageVector,
    val selectedIcon: ImageVector?,
    @StringRes val label: Int,
    val isEnabled: Boolean,
    val badge: MainNavItemBadge?,
    val analyticsEventIdentifier: NavigationEventIdentifier?,
    val preferredSlot: PreferredSlot,
    val testTag: String = "main_navigation:navigation_item_${destination::class.simpleName}"
){
    fun getIcon(isSelected: Boolean) = selectedIcon?.takeIf { isSelected } ?: icon
}