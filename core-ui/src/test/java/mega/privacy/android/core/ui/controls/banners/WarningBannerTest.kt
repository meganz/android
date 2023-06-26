package mega.privacy.android.core.ui.controls.banners

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WarningBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that close button is shown when on close event is set`() {
        val onCloseEvent = mock<() -> Unit>()
        composeTestRule.setContent {
            WarningBanner(textString = "Warning!", onCloseEvent)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_WARNING_BANNER_CLOSE).assertExists()
    }

    @Test
    fun `test that close button is not shown when on close event is null`() {
        composeTestRule.setContent {
            WarningBanner(textString = "Warning!", null)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_WARNING_BANNER_CLOSE).assertDoesNotExist()
    }

    @Test
    fun `test that text is shown`() {
        val text = "Warning!"
        composeTestRule.setContent {
            WarningBanner(textString = text, null)
        }
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test that on close click event is fired when close button is clicked`() {
        val onCloseEvent = mock<() -> Unit>()
        composeTestRule.setContent {
            WarningBanner(textString = "Warning!", onCloseEvent)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_WARNING_BANNER_CLOSE).performClick()
        verify(onCloseEvent).invoke()
    }
}