package mega.privacy.android.app.presentation.meeting.chat.view.bottombar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/** Mirrors CHAT_TEXT_FIELD_TEXT_TAG, which is internal to :shared:original-core-ui. */
private const val CHAT_TEXT_FIELD_TAG = "chat_text_field"

@RunWith(AndroidJUnit4::class)
class ChatBottomBarTest {
    @get:Rule
    var composeRule = createComposeRule()

    private val viewModel = mock<ChatBottomBarViewModel>()

    @Test
    fun `test that a draft populates the field without reporting the user as typing`() {
        setContent(ChatUiState(sendingText = "restored draft"))

        composeRule.onNodeWithText("restored draft").assertExists()
        verify(viewModel, never()).onUserTyping()
    }

    @Test
    fun `test that typing reports the user as typing`() {
        setContent(ChatUiState())

        composeRule.onNodeWithTag(CHAT_TEXT_FIELD_TAG).performTextInput("hi")

        verify(viewModel, atLeastOnce()).onUserTyping()
    }

    @Test
    fun `test that typing on top of a draft reports the user as typing`() {
        setContent(ChatUiState(sendingText = "restored draft"))

        composeRule.onNodeWithTag(CHAT_TEXT_FIELD_TAG).performTextInput("!")

        verify(viewModel, atLeastOnce()).onUserTyping()
    }

    private fun setContent(uiState: ChatUiState) {
        composeRule.setContent {
            ChatBottomBar(
                uiState = uiState,
                onSendClick = {},
                onAttachmentClick = {},
                onCloseEditing = {},
                viewModel = viewModel,
            )
        }
        composeRule.waitForIdle()
    }
}
