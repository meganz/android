package test.mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class FirstMessageHeaderTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that first message header is correctly shown`() {
        val chatTitle = "Title"
        composeTestRule.setContent {
            FirstMessageHeader(uiState = ChatUiState(title = chatTitle))
        }
        composeTestRule.onNodeWithText(chatTitle).assertExists()
        composeTestRule.onNodeWithText(R.string.chat_chatroom_first_message_header_mega_info_text)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.title_mega_confidentiality_empty_screen)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.mega_confidentiality_empty_screen).assertExists()
        composeTestRule.onNodeWithText(R.string.title_mega_confidentiality_empty_screen)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.chat_chatroom_first_message_header_authenticity_info_text)
            .assertExists()
    }
}