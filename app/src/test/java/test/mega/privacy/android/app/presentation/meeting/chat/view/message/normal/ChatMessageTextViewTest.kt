package test.mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageTextViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text showing correctly`() {
        initComposeRuleContent(
            text = "Hello World",
            isMe = true,
        )
        composeTestRule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun `test that enable rich link preview showing correctly when counter less than 3`() {
        initComposeRuleContent(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 2,
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_enable_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.text_enable_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_always_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_not_now_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_always_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_never_rich_links))
            .assertDoesNotExist()
    }

    @Test
    fun `test that enable rich link preview showing correctly when counter more than 3`() {
        initComposeRuleContent(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.title_enable_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.text_enable_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_always_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_not_now_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_always_rich_links))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.button_never_rich_links))
            .assertExists()
    }

    private fun initComposeRuleContent(
        text: String,
        isMe: Boolean = false,
        shouldShowWarning: Boolean = false,
        counterNotNowRichLinkWarning: Int = 0,
    ) {
        composeTestRule.setContent {
            ChatMessageTextView(
                text = text,
                isMe = isMe,
                shouldShowWarning = shouldShowWarning,
                counterNotNowRichLinkWarning = counterNotNowRichLinkWarning,
            )
        }
    }
}