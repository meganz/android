package test.mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageTextViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that text showing correctly`() {
        initComposeRuleContent("Hello World", true)
        composeTestRule.onAllNodesWithText("Hello World")
    }

    private fun initComposeRuleContent(text: String, isMe: Boolean) {
        composeTestRule.setContent {
            ChatMessageTextView(text = text, isMe = isMe)
        }
    }
}