package mega.privacy.mobile.navigation.snowflake

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import kotlinx.collections.immutable.ImmutableSet
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.navigation.snowflake.item.MainNavigationIcon
import mega.privacy.mobile.navigation.snowflake.item.MainNavigationItemBadge
import mega.privacy.mobile.navigation.snowflake.model.NavigationAnimationConfig
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem

/**
 * Material Design inspired animation configuration
 */
val DefaultNavigationAnimationConfig = NavigationAnimationConfig(
    durationMillis = 300,
    easing = FastOutSlowInEasing,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    mainNavItems: ImmutableSet<NavigationItem>,
    onDestinationClick: (Any) -> Unit,
    isSelected: (Any) -> Boolean,
    animationConfig: NavigationAnimationConfig = DefaultNavigationAnimationConfig,
    mainNavItemIcon: @Composable (ImageVector, String, Modifier) -> Unit = { icon, label, modifier ->
        MainNavigationIcon(
            icon = icon,
            label = label,
            modifier = modifier
        )
    },
    mainNavItemBadge: @Composable (String) -> Unit = { text ->
        MainNavigationItemBadge(text)
    },
    navContent: @Composable (NavigationUiController) -> Unit,
    availableSlots: Int = 5,
) {

    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())


    // Order items based on preferredSlot
    val orderedItems = orderNavigationItems(items = mainNavItems, availableSlots = availableSlots)
    var isNavigationVisible by remember { mutableStateOf(true) }
    val navUiController = NavigationUiController {
        isNavigationVisible = it
    }

    MegaNavigationSuiteScaffoldLayout(
        navigationSuite = {
            MegaNavigationSuite(
                navSuiteType = navSuiteType,
                orderedItems = orderedItems,
                mainNavItemIcon = mainNavItemIcon,
                mainNavItemBadge = mainNavItemBadge,
                isSelected = isSelected,
                onDestinationClick = onDestinationClick,
            )
        },
        layoutType = navSuiteType,
        isNavigationVisible = isNavigationVisible,
        animationConfig = animationConfig,
        content = {
            navContent(navUiController)
        }
    )
}

@Composable
private fun MegaNavigationSuite(
    navSuiteType: NavigationSuiteType,
    orderedItems: List<NavigationItem>,
    mainNavItemIcon: @Composable ((ImageVector, String, Modifier) -> Unit),
    mainNavItemBadge: @Composable ((String) -> Unit),
    isSelected: (Any) -> Boolean,
    onDestinationClick: (Any) -> Unit,
) {
    val scaffoldColors = NavigationScaffoldColors.scaffoldColors()
    val itemColors = NavigationScaffoldColors.itemColors()
    val borderColor = DSTokens.colors.border.subtle
    NavigationSuite(
        colors = scaffoldColors,
        modifier = Modifier
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
            .padding(top = 16.dp)
    ) {
        orderedItems.forEach { navItem ->
            item(
                icon = {
                    mainNavItemIcon(
                        navItem.icon,
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
}


/**
 * Orders navigation items based on their preferredSlot
 * - Ordered items are sorted by slot number and take the first available slots
 * - Last item is placed in the final slot
 */
private fun orderNavigationItems(
    items: ImmutableSet<NavigationItem>,
    availableSlots: Int,
): List<NavigationItem> {
    val orderedItems = items.filter { it.preferredSlot is PreferredSlot.Ordered }
        .sortedBy { (it.preferredSlot as PreferredSlot.Ordered).slot }

    val lastItem = items.find { it.preferredSlot is PreferredSlot.Last }

    return if (lastItem != null) {
        orderedItems.take(availableSlots - 1) + lastItem
    } else {
        orderedItems.take(availableSlots)
    }
}


@Composable
private fun MegaNavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    isNavigationVisible: Boolean,
    animationConfig: NavigationAnimationConfig,
    content: @Composable () -> Unit = {},
) {
    LookaheadScope {
        // Animate the navigation visibility using animateFloatAsState
        val navigationVisibility by animateFloatAsState(
            targetValue = if (isNavigationVisible) 1f else 0f,
            animationSpec = animationConfig.createAnimationSpec(),
            label = "navigation_visibility"
        )
        Layout(
            content = {
                // Wrap the navigation suite and content composables each in a Box to not propagate the
                // parent's (Surface) min constraints to its children (see b/312664933).
                Box(
                    Modifier.layoutId(NavigationSuiteLayoutIdTag)
                ) { navigationSuite() }
                Box(
                    Modifier.layoutId(ContentLayoutIdTag)
                ) { content() }
            }
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val isNavigationBar = layoutType == NavigationSuiteType.NavigationBar
            val layoutHeight = constraints.maxHeight
            val layoutWidth = constraints.maxWidth

            // Find the navigation suite composable through its layoutId tag
            val navigationPlaceable =
                measurables
                    .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                    .measure(looseConstraints)

            // Calculate animated navigation dimensions
            val animatedNavigationHeight =
                (navigationPlaceable.height * navigationVisibility).toInt()
            val animatedNavigationWidth = (navigationPlaceable.width * navigationVisibility).toInt()

            // Find the content composable through its layoutId tag
            val contentPlaceable =
                measurables
                    .fastFirst { it.layoutId == ContentLayoutIdTag }
                    .measure(
                        if (isNavigationBar) {
                            constraints.copy(
                                minHeight = layoutHeight - animatedNavigationHeight,
                                maxHeight = layoutHeight - animatedNavigationHeight
                            )
                        } else {
                            constraints.copy(
                                minWidth = layoutWidth - animatedNavigationWidth,
                                maxWidth = layoutWidth - animatedNavigationWidth
                            )
                        }
                    )

            layout(layoutWidth, layoutHeight) {
                if (isNavigationBar) {
                    // Place content above the navigation component.
                    contentPlaceable.placeRelative(0, 0)
                    // Place the navigation component at the bottom of the screen with animated position.
                    val navigationY = layoutHeight - animatedNavigationHeight
                    navigationPlaceable.placeRelative(0, navigationY)
                } else {
                    // Place the navigation component at the start of the screen with animated position.
                    val navigationX = -navigationPlaceable.width + animatedNavigationWidth
                    navigationPlaceable.placeRelative(navigationX, 0)
                    // Place content to the side of the navigation component.
                    contentPlaceable.placeRelative(animatedNavigationWidth, 0)
                }
            }
        }
    }
}

private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"

internal val WindowAdaptiveInfoDefault
    @Composable get() = currentWindowAdaptiveInfo()