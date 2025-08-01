package mega.privacy.mobile.navigation.snowflake

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.navigation.snowflake.item.MainNavigationIcon
import mega.privacy.mobile.navigation.snowflake.item.MainNavigationItemBadge
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    mainNavItems: ImmutableSet<NavigationItem>,
    onDestinationClick: (Any) -> Unit,
    isSelected: (Any) -> Boolean,
    mainNavItemIcon: @Composable (Int, String, Modifier) -> Unit = { icon, label, modifier ->
        MainNavigationIcon(
            iconRes = icon,
            label = label,
            modifier = modifier
        )
    },
    mainNavItemBadge: @Composable (String) -> Unit = { text ->
        MainNavigationItemBadge(text)
    },
    navContent: @Composable () -> Unit,
) {
    val scaffoldColors = NavigationScaffoldColors.scaffoldColors()
    val itemColors = NavigationScaffoldColors.itemColors()
    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val borderColor = DSTokens.colors.border.subtle

    // Order items based on preferredSlot
    val orderedItems = orderNavigationItems(mainNavItems)

    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            NavigationSuite(
                colors = scaffoldColors,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        if (navSuiteType == NavigationSuiteType.NavigationBar) {
                            drawLine(
                                color = borderColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = strokeWidth
                            )
                        } else {
                            drawLine(
                                color = borderColor,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
            ) {
                orderedItems.forEach { navItem ->
                    item(
                        icon = {
                            mainNavItemIcon(
                                navItem.iconRes,
                                stringResource(navItem.label),
                                Modifier.testTag(navItem.testTag),
                            )
                        },
                        badge = {
                            navItem.badgeText?.let { text ->
                                mainNavItemBadge(text)
                            }
                        },
                        label = { Text(text = stringResource(navItem.label)) },
                        selected = isSelected(navItem.destination),
                        onClick = {
                            Analytics.tracker.trackEvent(navItem.analyticsEventIdentifier)
                            onDestinationClick(navItem.destination)
                        },
                        colors = itemColors,
                        enabled = navItem.isEnabled,
                    )
                }
            }

        },
        layoutType = navSuiteType,
        content = navContent
    )
}

/**
 * Orders navigation items based on their preferredSlot
 * - Ordered items are sorted by slot number and take the first available slots
 * - Last item is placed in the final slot
 */
private fun orderNavigationItems(items: ImmutableSet<NavigationItem>): List<NavigationItem> {
    val orderedItems = items.filter { it.preferredSlot is PreferredSlot.Ordered }
        .sortedBy { (it.preferredSlot as PreferredSlot.Ordered).slot }

    val lastItem = items.find { it.preferredSlot is PreferredSlot.Last }

    // For now, assume 5 slots for bottom navigation
    val availableSlots = 5

    return if (lastItem != null) {
        orderedItems.take(availableSlots - 1) + lastItem
    } else {
        orderedItems.take(availableSlots)
    }
}