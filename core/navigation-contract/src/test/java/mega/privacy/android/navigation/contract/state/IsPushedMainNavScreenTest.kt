package mega.privacy.android.navigation.contract.state

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class IsPushedMainNavScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private data object HomeKey : NavKey
    private data object MenuKey : NavKey

    private fun setContent(topLevelNavKeyClass: KClass<out NavKey>?) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides topLevelNavKeyClass) {
                Text(if (isPushedMainNavScreen(HomeKey::class)) "Pushed" else "Root")
            }
        }
    }

    @Test
    fun `test that isPushedMainNavScreen returns false when not hosted in the main navigation display`() {
        setContent(null)

        composeTestRule.onNodeWithText("Root").assertIsDisplayed()
    }

    @Test
    fun `test that isPushedMainNavScreen returns false when the screen roots its hosting top level`() {
        setContent(HomeKey::class)

        composeTestRule.onNodeWithText("Root").assertIsDisplayed()
    }

    @Test
    fun `test that isPushedMainNavScreen returns true when the screen is hosted by another top level`() {
        setContent(MenuKey::class)

        composeTestRule.onNodeWithText("Pushed").assertIsDisplayed()
    }
}
