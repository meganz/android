package mega.privacy.mobile.navigation.snowflake.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier

/**
 * Navigation item
 *
 * @property destination The destination of the navigation item, can be any type.
 * @property isEnabled Indicates whether the navigation item is enabled or not.
 * @property badgeText Optional text for the badge associated with the navigation item.
 */
@Immutable
data class NavigationItem(
    val destination: Any,
    val icon: ImageVector,
    @StringRes val label: Int,
    val isEnabled: Boolean,
    val badgeText: String?,
    val analyticsEventIdentifier: NavigationEventIdentifier,
    val preferredSlot: PreferredSlot,
    val testTag: String = "main_navigation:navigation_item_${destination::class.simpleName}"
)