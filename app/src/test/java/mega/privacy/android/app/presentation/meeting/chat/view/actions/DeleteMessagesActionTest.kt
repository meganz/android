package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CHAT_BOTTOM_SHEET_OPTION_DELETE_TAG
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class DeleteMessagesActionTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var underTest: DeleteMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    @Before
    fun setUp() {
        underTest = DeleteMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to deletable messages which are mine`() {
        assertThat(underTest.appliesTo(listOf(mock<NormalMessage> {
            on { isDeletable } doReturn true
            on { isMine } doReturn true
        }))).isTrue()
    }

    @Test
    fun `test that action does not apply to deletable messages which are not mine`() {
        assertThat(underTest.appliesTo(listOf(mock<NormalMessage> {
            on { isDeletable } doReturn true
            on { isMine } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non deletable messages even if they are mine`() {
        assertThat(underTest.appliesTo(listOf(mock<NormalMessage> {
            on { isDeletable } doReturn false
            on { isMine } doReturn true
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non deletable messages which are not mine`() {
        assertThat(underTest.appliesTo(listOf(mock<NormalMessage> {
            on { isDeletable } doReturn false
            on { isMine } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that composable contains delete bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = emptyList(),
                context = mock(),
                hideBottomSheet = {},
            )
        )

        composeTestRule.onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_DELETE_TAG).assertExists()
    }

    @Test
    fun `test that clicking the menu option shows the confirmation dialog`() {
        val context = mock<Context>()
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = emptyList(),
                context = context,
                hideBottomSheet = {},
            )
        )

        composeTestRule.onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_DELETE_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking the cancel option in the confirmation dialog does not invoke view model`() {
        val context = mock<Context>()
        with(composeTestRule) {
            setContent(
                underTest.bottomSheetMenuItem(
                    messages = emptyList(),
                    context = context,
                    hideBottomSheet = {},
                )
            )
            onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_DELETE_TAG).performClick()
            onNodeWithTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG)
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.button_cancel)).performClick()
        }
        verifyNoInteractions(chatViewModel)
    }

    @Test
    fun `test that clicking the remove option in the confirmation dialog invokes view model`() {
        val context = mock<Context>()
        val messages = listOf(mock<NormalMessage>())
        with(composeTestRule) {
            setContent(
                underTest.bottomSheetMenuItem(
                    messages = messages,
                    context = context,
                    hideBottomSheet = {},
                )
            )
            onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_DELETE_TAG).performClick()
            onNodeWithTag(TEST_TAG_REMOVE_MESSAGES_CONFIRMATION_DIALOG)
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.context_remove)).performClick()
        }
        verify(chatViewModel).onDeletedMessages(messages)
    }
}