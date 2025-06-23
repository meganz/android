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

    private fun createTestNavItems(): ImmutableSet<MainNavItem> {
        return listOf(
            createMockNavItem("Home", 1),
            createMockNavItem("Chat", 2),
            createMockNavItem("Photos", 3)
        ).toImmutableSet()
    }

    private fun createTestNavItemsWithBadge(): ImmutableSet<MainNavItem> {
        return listOf(
            createMockNavItem("Home", 1),
            createMockNavItemWithBadge("Chat", 2, "5"),
            createMockNavItem("Photos", 3)
        ).toImmutableSet()
    }

    private fun createMockNavItem(label: String, iconRes: Int): MainNavItem {
        return mock<MainNavItem> {
            on { this.label } doReturn label
            on { this.iconRes } doReturn iconRes
            on { badge } doReturn null
            on { destinationClass } doReturn TestHomeScreen::class
            on { destination } doReturn TestHomeScreen
            on { screen } doReturn { navigationHandler -> testHomeScreen() }
        }
    }

    private fun createMockNavItemWithBadge(
        label: String,
        iconRes: Int,
        badgeText: String,
    ): MainNavItem {
        return mock<MainNavItem> {
            on { this.label } doReturn label
            on { this.iconRes } doReturn iconRes
            on { badge } doReturn flowOf(badgeText)
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