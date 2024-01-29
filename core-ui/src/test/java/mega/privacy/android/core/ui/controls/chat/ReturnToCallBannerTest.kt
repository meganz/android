package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.assertIsDisplayed
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
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ReturnToCallBannerTest {

    @get:Rule
    var composeRule = createComposeRule()


    val text = "Hello world"

    @Test
    fun `test banner shows correctly`() {
        composeRule.setContent {
            ReturnToCallBanner(text = text, onBannerClicked = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL, true).assertIsDisplayed()
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    @Test
    fun `test banner on click is invoked`() {
        val actionPressed = mock<() -> Unit>()
        composeRule.setContent {
            ReturnToCallBanner(text = text, onBannerClicked = actionPressed)
        }
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(actionPressed).invoke()
    }

    @Test
    fun `test Chronometer is displayed if duration is received`() {
        val duration = 100.seconds
        composeRule.setContent {
            ReturnToCallBanner(text = text, onBannerClicked = {}, duration = duration)
        }
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL, true).assertIsDisplayed()
        composeRule.onNodeWithText(text).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertIsDisplayed()
    }

    @Test
    fun `test Chronometer is not displayed if duration is received`() {
        composeRule.setContent {
            ReturnToCallBanner(text = text, onBannerClicked = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL, true).assertIsDisplayed()
        composeRule.onNodeWithText(text).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER).assertDoesNotExist()
    }
}