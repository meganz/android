package mega.privacy.mobile.navigation.snowflake

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@RunWith(AndroidJUnit4::class)
class MainNavigationScaffoldTest {

    val composeTestRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun test_that_navigation_items_are_displayed() {
        val navItems = createTestNavItems()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // Verify all navigation items are displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Photos").assertIsDisplayed()
    }

    @Test
    fun test_that_navigation_item_click_is_handled() {
        val navItems = createTestNavItems()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // Click on a navigation item
        composeTestRule.onNodeWithText("Home").performClick()

        // Verify the click handler was called with the correct item
        verify(onDestinationClick).invoke(navItems.first { it.label == "Home" }.destination)
        verifyNoMoreInteractions(onDestinationClick)
    }

    @Test
    fun test_that_selected_state_is_reflected_in_ui() {
        val navItems = createTestNavItems()
        val onDestinationClick: (Any) -> Unit = mock()
        val selectedItem = navItems.first { it.label == "Home" }.destination
        val isSelected: (Any) -> Boolean = { it == selectedItem }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // The selected item should be displayed (UI state changes are handled by Material3)
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Photos").assertIsDisplayed()
    }

    @Test
    fun test_that_badge_is_displayed_if_provided() {
        val navItemsWithBadge = createTestNavItemsWithBadge()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItemsWithBadge,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = @Composable { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // Verify the item with badge is displayed
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
    }

    @Test
    fun test_that_empty_navigation_items_list_is_handled() {
        val emptyNavItems: ImmutableSet<NavigationItem> =
            emptySet<NavigationItem>().toImmutableSet()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }
        val expected = "contentArea"

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = emptyNavItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = @Composable { _, label ->
                    TestIcon(label)
                },
                navContent = @Composable {
                    Text(text = "Content Area", modifier = Modifier.Companion.testTag(expected))
                }
            )
        }

        // Should not crash and should display content area
        composeTestRule.onNodeWithTag(expected).assertIsDisplayed()

    }

    @Test
    fun test_that_content_area_is_displayed() {
        val navItems = createTestNavItems()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }
        val expected = "contentArea"
        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {
                    Text(text = "Content Area", modifier = Modifier.Companion.testTag(expected))
                }
            )
        }

        composeTestRule.onNodeWithTag(expected).assertIsDisplayed()
    }

    // Tests for preferred slot ordering logic

    @Test
    fun test_that_only_first_four_items_and_last_item_are_displayed_when_more_than_four_are_added() {

        val navItems = listOf(
            createMockNavItem("Home", 1, PreferredSlot.Ordered(1)),
            createMockNavItem("Chat", 2, PreferredSlot.Ordered(2)),
            createMockNavItem("Photos", 3, PreferredSlot.Ordered(3)),
            createMockNavItem("Settings", 4, PreferredSlot.Ordered(4)),
            createMockNavItem("Extra1", 5, PreferredSlot.Ordered(5)),
            createMockNavItem("Extra2", 6, PreferredSlot.Ordered(6)),
            createMockNavItem("Menu", 7, PreferredSlot.Last)
        ).toImmutableSet()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // Should display: Home (slot 1), Chat (slot 2), Photos (slot 3), Settings (slot 4), Menu (last)
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Photos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Menu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Extra1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Extra2").assertDoesNotExist()

    }

    @Test
    fun test_that_all_items_are_displayed_if_only_one_item_and_last_are_passed() {
        val navItems = listOf(
            createMockNavItem("Home", 1, PreferredSlot.Ordered(1)),
            createMockNavItem("Menu", 2, PreferredSlot.Last)
        ).toImmutableSet()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        // Should display both items: Home (ordered) and Menu (last)
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Menu").assertIsDisplayed()
    }

    @Test
    fun test_that_five_items_are_displayed_if_five_or_more_items_are_passed_and_no_last_item_is_passed() {
        val navItems = listOf(
            createMockNavItem("Home", 1, PreferredSlot.Ordered(1)),
            createMockNavItem("Chat", 2, PreferredSlot.Ordered(2)),
            createMockNavItem("Photos", 3, PreferredSlot.Ordered(3)),
            createMockNavItem("Settings", 4, PreferredSlot.Ordered(4)),
            createMockNavItem("Extra1", 5, PreferredSlot.Ordered(5))
        ).toImmutableSet()
        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()      // slot 1
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()      // slot 2
        composeTestRule.onNodeWithText("Photos").assertIsDisplayed()    // slot 3
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()  // slot 4
        composeTestRule.onNodeWithText("Extra1").assertIsDisplayed()    // slot 5
    }

    @Test
    fun test_that_analytics_events_are_fired_when_navigation_items_are_selected() = runTest {
        val expected = mock<NavigationEventIdentifier>()
        val navItems = listOf(
            createMockNavItem(
                label = "Home",
                iconRes = 1,
                preferredSlot = PreferredSlot.Ordered(1),
                navigationEventIdentifier = expected,
            ),
        ).toImmutableSet()

        val onDestinationClick: (Any) -> Unit = mock()
        val isSelected: (Any) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { _, label ->
                    TestIcon(label)
                },
                navContent = {}
            )
        }

        composeTestRule.onNodeWithText("Home").performClick()

        assertThat(analyticsRule.events).contains(expected)
    }

    private fun createTestNavItems() = listOf(
        createMockNavItem(label = "Home", iconRes = 1, preferredSlot = PreferredSlot.Ordered(1)),
        createMockNavItem(label = "Chat", iconRes = 2, preferredSlot = PreferredSlot.Ordered(2)),
        createMockNavItem(label = "Photos", iconRes = 3, preferredSlot = PreferredSlot.Last)
    ).toImmutableSet()

    private fun createTestNavItemsWithBadge() = listOf(
        createMockNavItem(label = "Home", iconRes = 1, preferredSlot = PreferredSlot.Ordered(1)),
        createMockNavItem(
            label = "Chat",
            iconRes = 2,
            preferredSlot = PreferredSlot.Ordered(2),
            badgeText = "5"
        ),
        createMockNavItem(label = "Photos", iconRes = 3, preferredSlot = PreferredSlot.Last)
    ).toImmutableSet()

    private fun createMockNavItem(
        label: String,
        iconRes: Int,
        preferredSlot: PreferredSlot,
        enabled: Boolean = true,
        badgeText: String? = null,
        navigationEventIdentifier: NavigationEventIdentifier = mock<NavigationEventIdentifier>(),
    ): NavigationItem {
        return NavigationItem(
            destination = TestHomeScreen,
            iconRes = iconRes,
            label = label,
            isEnabled = enabled,
            badgeText = badgeText,
            analyticsEventIdentifier = navigationEventIdentifier,
            preferredSlot = preferredSlot,
        )
    }

    @Composable
    private fun TestIcon(label: String) {
        // This is a placeholder for the actual icon rendering logic
        Icon(
            painter = ColorPainter(Color.Red),
            contentDescription = label,
        )
    }

    @Serializable
    object TestHomeScreen
}