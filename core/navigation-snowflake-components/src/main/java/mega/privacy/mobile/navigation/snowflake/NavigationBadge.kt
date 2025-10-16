package mega.privacy.mobile.navigation.snowflake

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.badge.NotificationBadge
import mega.privacy.android.navigation.contract.MainNavItemBadge

@Composable
fun NavigationBadge(
    navigationBadge: MainNavItemBadge,
    small: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        navigationBadge is MainNavItemBadge.IconBadge -> {
            NotificationBadge(navigationBadge.icon, modifier, small)
        }

        small -> {
            NotificationBadge(modifier)
        }

        navigationBadge is MainNavItemBadge.NumberBadge -> {
            NotificationBadge(navigationBadge.number, modifier)
        }

        navigationBadge is MainNavItemBadge.TextBadge -> {
            NotificationBadge(navigationBadge.text, modifier)
        }
    }
}