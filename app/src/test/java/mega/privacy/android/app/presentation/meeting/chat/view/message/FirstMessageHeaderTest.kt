package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class FirstMessageHeaderTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that first message header is correctly shown`() {
        val chatTitle = "Title"
        composeTestRule.setContent {
            FirstMessageHeader(
                scheduledMeeting = null,
                isNoteToSelfChat = false,
                title = chatTitle
            )
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


    @Test
    fun `test that first message header for note to self is correctly shown`() {
        val chatTitle = "Title"
        composeTestRule.setContent {
            FirstMessageHeader(
                scheduledMeeting = null,
                isNoteToSelfChat = true,
                title = chatTitle
            )
        }
        composeTestRule.onNodeWithText(sharedR.string.chat_note_to_self_chat_first_message_header)
            .assertExists()
        composeTestRule.onNodeWithText(sharedR.string.chat_note_to_self_chat_first_message_header_paragraph)
            .assertExists()
    }
}