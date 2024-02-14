package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ForwardMessageActionTest() {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var underTest: ForwardMessageAction

    private val chatViewModel = mock<ChatViewModel>()
    private val launchPicker = mock<(Context, Long, ActivityResultLauncher<Intent>) -> Unit>()

    @Before
    fun setUp() {
        underTest = ForwardMessageAction(
            chatViewModel = chatViewModel,
            launchChatPicker = launchPicker,
        )
    }

    @Test
    fun `test that action applies to non management messages`() {
        assertThat(underTest.appliesTo(listOf(mock<NormalMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply to management messages`() {
        assertThat(underTest.appliesTo(listOf(mock<ManagementMessage>()))).isFalse()
    }

    @Test
    fun `test that composable contains forward bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = emptyList(),
                chatId = 123L,
                context = mock(),
            )
        )

        composeTestRule.onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG).assertExists()
    }

    @Test
    fun `test that clicking the menu option launches the chat picker`() {
        val chatId = 123L
        val context = mock<Context>()
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = emptyList(),
                chatId = chatId,
                context = context,
            )
        )

        composeTestRule.onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_FORWARD_TAG).performClick()

        verify(launchPicker).invoke(eq(context), eq(chatId), any())
    }
}