package test.mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.message.ChatManagementMessageView
import mega.privacy.android.core.ui.model.SpanIndicator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatManagementMessageViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that text showing correctly`() {
        initComposeRuleContent(
            text = "[A]Hello[/A] World",
            styles = mapOf(
                SpanIndicator('A') to SpanStyle(
                    fontWeight = FontWeight.Bold
                )
            )
        )
        composeTestRule.onAllNodesWithText("Hello World")
    }

    private fun initComposeRuleContent(
        text: String,
        styles: Map<SpanIndicator, SpanStyle>,
    ) {
        composeTestRule.setContent {
            ChatManagementMessageView(text = text, styles = styles)
        }
    }
}