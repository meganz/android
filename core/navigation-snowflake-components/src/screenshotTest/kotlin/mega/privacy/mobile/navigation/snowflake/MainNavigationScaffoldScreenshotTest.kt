package mega.privacy.mobile.navigation.snowflake

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.toImmutableSet
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem

/**
 * Screenshot tests for the [MainNavigationScaffold] bottom navigation bar, covering a
 * customised five-slot arrangement in non-default order and a bar with a disabled item.
 */
class MainNavigationScaffoldScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MainNavigationScaffoldCustomisedOrder() {
        AndroidThemeForPreviews {
            MainNavigationScaffold(
                mainNavItems = customisedOrderItems(disabledId = null),
                onDestinationClick = {},
                isSelected = { (it as? ScreenshotNavKey)?.id == "chat" },
                navContent = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MainNavigationScaffoldDisabledItem() {
        AndroidThemeForPreviews {
            MainNavigationScaffold(
                mainNavItems = customisedOrderItems(disabledId = "media"),
                onDestinationClick = {},
                isSelected = { (it as? ScreenshotNavKey)?.id == "chat" },
                navContent = {},
            )
        }
    }

    private fun customisedOrderItems(disabledId: String?) = listOf(
        navigationItem(
            id = "chat",
            label = sharedR.string.general_chat,
            icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
            selectedIcon = IconPack.Medium.Thin.Solid.MessageChatCircle,
            preferredSlot = PreferredSlot.Ordered(1),
            isEnabled = disabledId != "chat",
        ),
        navigationItem(
            id = "media",
            label = sharedR.string.media_feature_title,
            icon = IconPack.Medium.Thin.Outline.Image01,
            selectedIcon = IconPack.Medium.Thin.Solid.Image01,
            preferredSlot = PreferredSlot.Ordered(2),
            isEnabled = disabledId != "media",
        ),
        navigationItem(
            id = "home",
            label = sharedR.string.general_section_home,
            icon = IconPack.Medium.Thin.Outline.Home,
            selectedIcon = IconPack.Medium.Thin.Solid.Home,
            preferredSlot = PreferredSlot.Ordered(3),
            isEnabled = disabledId != "home",
        ),
        navigationItem(
            id = "drive",
            label = sharedR.string.general_drive,
            icon = IconPack.Medium.Thin.Outline.Folder,
            selectedIcon = IconPack.Medium.Thin.Solid.Folder,
            preferredSlot = PreferredSlot.Ordered(4),
            isEnabled = disabledId != "drive",
        ),
        navigationItem(
            id = "menu",
            label = sharedR.string.general_menu,
            icon = IconPack.Medium.Thin.Outline.Menu01,
            selectedIcon = IconPack.Medium.Thin.Solid.Menu01,
            preferredSlot = PreferredSlot.Last,
            isEnabled = disabledId != "menu",
        ),
    ).toImmutableSet()

    private fun navigationItem(
        id: String,
        label: Int,
        icon: ImageVector,
        selectedIcon: ImageVector,
        preferredSlot: PreferredSlot,
        isEnabled: Boolean,
    ) = NavigationItem(
        destination = ScreenshotNavKey(id),
        icon = icon,
        selectedIcon = selectedIcon,
        label = label,
        isEnabled = isEnabled,
        badge = null,
        analyticsEventIdentifier = null,
        preferredSlot = preferredSlot,
        testTag = "main_navigation:navigation_item_$id",
    )
}

private data class ScreenshotNavKey(val id: String) : MainNavItemNavKey
