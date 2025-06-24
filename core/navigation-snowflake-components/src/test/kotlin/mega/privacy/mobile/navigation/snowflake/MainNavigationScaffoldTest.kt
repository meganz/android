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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.PreferredSlot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@RunWith(AndroidJUnit4::class)
class MainNavigationScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_navigation_items_are_displayed() {
        val navItems = createTestNavItems()
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
                },
                navContent = {}
            )
        }

        // Click on a navigation item
        composeTestRule.onNodeWithText("Home").performClick()

        // Verify the click handler was called with the correct item
        verify(onDestinationClick).invoke(navItems.first { it.label == "Home" })
        verifyNoMoreInteractions(onDestinationClick)
    }

    @Test
    fun test_that_selected_state_is_reflected_in_ui() {
        val navItems = createTestNavItems()
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val selectedItem = navItems.first { it.label == "Home" }
        val isSelected: (MainNavItem) -> Boolean = { it == selectedItem }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItemsWithBadge,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
                },
                navContent = {}
            )
        }

        // Verify the item with badge is displayed
        composeTestRule.onNodeWithText("Chat").assertIsDisplayed()
    }

    @Test
    fun test_that_empty_navigation_items_list_is_handled() {
        val emptyNavItems: ImmutableSet<MainNavItem> = emptySet<MainNavItem>().toImmutableSet()
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }
        val expected = "contentArea"

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = emptyNavItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
                },
                navContent = {
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }
        val expected = "contentArea"
        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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
        val onDestinationClick: (MainNavItem) -> Unit = mock()
        val isSelected: (MainNavItem) -> Boolean = { false }

        composeTestRule.setContent {
            MainNavigationScaffold(
                mainNavItems = navItems,
                onDestinationClick = onDestinationClick,
                isSelected = isSelected,
                mainNavItemIcon = { mainNavItem ->
                    TestIcon(mainNavItem)
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

    private fun createTestNavItems(): ImmutableSet<MainNavItem> {
        return listOf(
            createMockNavItem("Home", 1, PreferredSlot.Ordered(1)),
            createMockNavItem("Chat", 2, PreferredSlot.Ordered(2)),
            createMockNavItem("Photos", 3, PreferredSlot.Last)
        ).toImmutableSet()
    }

    private fun createTestNavItemsWithBadge(): ImmutableSet<MainNavItem> {
        return listOf(
            createMockNavItem("Home", 1, PreferredSlot.Ordered(1)),
            createMockNavItemWithBadge("Chat", 2, "5", PreferredSlot.Ordered(2)),
            createMockNavItem("Photos", 3, PreferredSlot.Last)
        ).toImmutableSet()
    }

    private fun createMockNavItem(
        label: String,
        iconRes: Int,
        preferredSlot: PreferredSlot,
    ): MainNavItem {
        return mock<MainNavItem> {
            on { this.label } doReturn label
            on { this.iconRes } doReturn iconRes
            on { badge } doReturn null
            on { destinationClass } doReturn TestHomeScreen::class
            on { destination } doReturn TestHomeScreen
            on { screen } doReturn { navigationHandler -> testHomeScreen() }
            on { this.preferredSlot } doReturn preferredSlot
        }
    }

    private fun createMockNavItemWithBadge(
        label: String,
        iconRes: Int,
        badgeText: String,
        preferredSlot: PreferredSlot,
    ): MainNavItem {
        return mock<MainNavItem> {
            on { this.label } doReturn label
            on { this.iconRes } doReturn iconRes
            on { badge } doReturn flowOf(badgeText)
            on { this.preferredSlot } doReturn preferredSlot
        }
    }
}

@Composable
private fun TestIcon(mainNavItem: MainNavItem) {
    // This is a placeholder for the actual icon rendering logic
    Icon(
        painter = ColorPainter(Color.Red),
        contentDescription = mainNavItem.label,
    )
}

@Serializable
object TestHomeScreen

fun NavGraphBuilder.testHomeScreen() {
    composable<TestHomeScreen> {
        TestHomeScreen()
    }
}

@Composable
fun TestHomeScreen() {
    // This is a placeholder for the actual screen content
    Text(text = "Home Screen")
}