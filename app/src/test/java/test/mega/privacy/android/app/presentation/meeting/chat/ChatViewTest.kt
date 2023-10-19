package test.mega.privacy.android.app.presentation.meeting.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.ChatView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that title shows correctly when passing title to uiState`() {
        val title = "my chat room title"
        initComposeRuleContent(
            ChatUiState(title = title)
        )
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    private fun initComposeRuleContent(
        state: ChatUiState,
    ) {
        composeTestRule.setContent {
            ChatView(
                uiState = state,
                onBackPressed = {}
            )
        }
    }
}