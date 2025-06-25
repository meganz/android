package mega.privacy.mobile.navigation.snowflake.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.navigation.contract.MainNavItem

/**
 * Navigation item
 *
 * @property navItem
 * @property isEnabled
 */
@Immutable
data class NavigationItem(
    val navItem: MainNavItem,
    val isEnabled: Boolean,
)