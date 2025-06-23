package mega.privacy.mobile.navigation.snowflake.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType

@Composable
internal fun MainNavigationItemBadge(text: String) {
    Badge(
        modifier = Modifier.Companion,
        badgeType = BadgeType.Mega,
        text = text
    )
}