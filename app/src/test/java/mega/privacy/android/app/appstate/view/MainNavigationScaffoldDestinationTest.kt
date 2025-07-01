package mega.privacy.android.app.appstate.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.navigation.contract.PreferredSlot
import mega.privacy.mobile.analytics.core.event.identifier.NavigationEventIdentifier
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class MainNavigationScaffoldDestinationTest {

    val composeTestRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var navController: TestNavHostController

    val tabATag = "Tab A"
    val tabBTag = "Tab B"
    val tabCTag = "Tab C"
    private val tabA = testNavItem(TabDestinationA, tabATag)
    private val tabB = testNavItem(TabDestinationB, tabBTag)
    private val tabC = testNavItem(TabDestinationC, tabCTag)

    private val testTopLevelDestinations = persistentSetOf(tabA, tabB, tabC)

    @Before
    fun setUp() {
        // Create a TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            // This is the outer NavHost simulating your app's structure
            NavHost(navController = navController, startDestination = TopLevelTestDestination) {
                composable<TopLevelTestDestination> { /* Simple Composable for the top level */ }

                // Your mainNavigationScaffold composable from the original code
                mainNavigationScaffold(
                    modifier = Modifier,
                    topLevelDestinations = testTopLevelDestinations,
                    startDestination = TabDestinationA // Start tab within the scaffold
                ) {
                    // Define the content for each tab within the scaffold's NavHost
                    composable<TabDestinationA> { /* Content for Tab A */ }
                    composable<TabDestinationB> { /* Content for Tab B */ }
                    composable<TabDestinationC> { /* Content for Tab C */ }
                }
            }
        }
    }

    @Test
    fun test_that_back_navigation_from_scaffold_tab_returns_to_top_level_destination() {
        // 1. Navigate from TopLevelTestDestination to MainNavigationScaffoldDestination
        composeTestRule.runOnUiThread {
            navController.navigate(MainNavigationScaffoldDestination)
        }
        composeTestRule.waitForIdle() // Ensure navigation completes

        // Verify we are inside the scaffold, Tab A should be selected by default
        assertThat(
            navController.currentDestination?.hasRoute(MainNavigationScaffoldDestination::class)
        ).isTrue()
        // You might also want to check the inner NavController of the scaffold if accessible
        // or assert that Tab A's content is visible.

        // 2. Click on Tab B
        composeTestRule.onNodeWithTag(tabBTag, useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Optionally verify Tab B is now active (e.g., its content is visible, or check inner NavController)

        // 3. Click on Tab C
        composeTestRule.onNodeWithTag(tabCTag, useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Optionally verify Tab C is now active

        // 4. Simulate a back press
        // For Robolectric, the NavController's popBackStack is the most direct way
        // If the NavHost has focus, Robolectric might also route a simulated back key event.
        val popped = composeTestRule.runOnUiThread {
            navController.popBackStack()
        }
        composeTestRule.waitForIdle()

        // Assert that the back press was handled and did pop something
        assertThat(popped).isTrue()

        // 5. Verify current destination is now TopLevelTestDestination
        assertThat(
            navController.currentDestination?.hasRoute(TopLevelTestDestination::class)
        ).isTrue()
        assertThat(
            navController.currentDestination?.hasRoute(MainNavigationScaffoldDestination::class)
        ).isFalse()
    }

    private fun testNavItem(
        destination: Any,
        testTag: String,
    ) = NavigationItem(
        destination = destination,
        iconRes = android.R.drawable.ic_menu_more,
        label = android.R.string.ok,
        isEnabled = true,
        analyticsEventIdentifier = mock<NavigationEventIdentifier>(),
        preferredSlot = PreferredSlot.Ordered(1),
        badgeText = null,
        testTag = testTag,
    )
}

// --- Destinations for testing ---
@Serializable
object TopLevelTestDestination // The destination we expect to return to

// Dummy destinations for tabs within MainNavigationScaffold
@Serializable
object TabDestinationA

@Serializable
object TabDestinationB

@Serializable
object TabDestinationC

