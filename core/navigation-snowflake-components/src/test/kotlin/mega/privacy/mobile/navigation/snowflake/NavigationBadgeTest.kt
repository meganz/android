package mega.privacy.mobile.navigation.snowflake

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.navigation.contract.DefaultNumberBadge
import mega.privacy.android.navigation.contract.MainNavItemBadge
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that number is shown when is a numbered badge`() {
        val number = 5
        composeRule.setContent {
            NavigationBadge(DefaultNumberBadge(number), false)
        }
        composeRule.onNodeWithText("$number").assertIsDisplayed()
    }

    @Test
    fun `test that small dot is shown when is a small badge`() {
        val tag = "test"
        val number = 5
        composeRule.setContent {
            NavigationBadge(DefaultNumberBadge(number), true, Modifier.testTag(tag))
        }
        composeRule.onNodeWithText("$number").assertDoesNotExist()
        composeRule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(6.dp)
            .assertHeightIsEqualTo(6.dp)
    }
}