package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromptMessageBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that prompt message is shown`() {
        val message = "This is a prompt message"
        composeTestRule.setContent {
            PromptMessageBanner(message)
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }
}