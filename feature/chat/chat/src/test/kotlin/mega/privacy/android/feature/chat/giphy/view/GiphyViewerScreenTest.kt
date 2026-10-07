package mega.privacy.android.feature.chat.giphy.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class GiphyViewerScreenTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun initComposeRuleContent(
        showSendButton: Boolean,
        onSend: () -> Unit = {},
    ) {
        composeRule.setContent {
            GiphyViewerScreen(
                imageUrl = null,
                title = "GIF",
                showSendButton = showSendButton,
                onSend = onSend,
            )
        }
    }

    @Test
    fun `test that send button is shown when the gif is being picked`() {
        initComposeRuleContent(showSendButton = true)

        composeRule.onNodeWithTag(GIPHY_VIEWER_SEND_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that send button is not shown when the gif is only previewed`() {
        initComposeRuleContent(showSendButton = false)

        composeRule.onNodeWithTag(GIPHY_VIEWER_SEND_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that onSend is invoked when send button is clicked`() {
        val onSend = mock<() -> Unit>()
        initComposeRuleContent(showSendButton = true, onSend = onSend)

        composeRule.onNodeWithTag(GIPHY_VIEWER_SEND_TAG).performClick()

        verify(onSend).invoke()
    }

    @Test
    fun `test that gif image is shown`() {
        initComposeRuleContent(showSendButton = false)

        composeRule.onNodeWithTag(GIPHY_VIEWER_IMAGE_TAG).assertIsDisplayed()
    }
}
