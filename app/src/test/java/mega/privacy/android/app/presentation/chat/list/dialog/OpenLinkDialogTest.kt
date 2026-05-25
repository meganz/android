package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.RegexPatternType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class OpenLinkDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that generic open link title is shown when neither chat nor join meeting`() {
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = false,
                isJoinMeeting = false,
                inputLink = "",
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_open_link))
            .assertIsDisplayed()
    }

    @Test
    fun `test that open chat link title is shown for chat screen`() {
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = true,
                isJoinMeeting = false,
                inputLink = "",
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.action_open_chat_link))
            .assertIsDisplayed()
    }

    @Test
    fun `test that paste meeting link title is shown when joining a meeting`() {
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = true,
                isJoinMeeting = true,
                inputLink = "",
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.paste_meeting_link_guest_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that confirm button label switches to open chat link for chat link type`() {
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = true,
                isJoinMeeting = false,
                inputLink = "https://mega.nz/chat/example",
                linkType = RegexPatternType.CHAT_LINK,
            )
        }

        composeTestRule
            .onNodeWithTag(INPUT_DIALOG_CONFIRM_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onConfirm is invoked when confirm button is clicked`() {
        val onConfirm = mock<(String) -> Unit>()
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = false,
                isJoinMeeting = false,
                inputLink = "abc",
                onConfirm = onConfirm,
            )
        }

        composeTestRule.onNodeWithTag(INPUT_DIALOG_CONFIRM_TAG).performClick()
        verify(onConfirm).invoke("abc")
    }

    @Test
    fun `test that onDismissRequest is invoked when cancel button is clicked`() {
        val onDismissRequest = mock<() -> Unit>()
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = false,
                isJoinMeeting = false,
                inputLink = "",
                onDismissRequest = onDismissRequest,
            )
        }

        composeTestRule.onNodeWithTag(INPUT_DIALOG_CANCEL_TAG).performClick()
        verify(onDismissRequest).invoke()
    }

    @Test
    fun `test that onLinkChanged is invoked when the input field changes`() {
        val onLinkChanged = mock<(String) -> Unit>()
        composeTestRule.setContent {
            OpenLinkDialogContent(
                isChatScreen = false,
                isJoinMeeting = false,
                inputLink = "",
                onLinkChanged = onLinkChanged,
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("x")
        verify(onLinkChanged).invoke("x")
    }

    companion object {
        private const val INPUT_DIALOG_TITLE_TAG = "input_dialog:text_title"
        private const val INPUT_DIALOG_TEXT_TAG = "input_dialog:generic_text_field_input"
        private const val INPUT_DIALOG_CANCEL_TAG = "input_dialog:button_cancel"
        private const val INPUT_DIALOG_CONFIRM_TAG = "input_dialog:button_confirm"
    }
}
