package mega.privacy.mobile.navigation.snowflake.model

import androidx.compose.runtime.Immutable
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
    val iconRes: Int,
    val label: String,
    val isEnabled: Boolean,
    val badgeText: String?,
    val analyticsEventIdentifier: NavigationEventIdentifier,
    val preferredSlot: PreferredSlot,
)