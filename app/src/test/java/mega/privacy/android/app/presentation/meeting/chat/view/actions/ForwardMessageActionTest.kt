package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ForwardMessageActionTest {
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
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply to management messages`() {
        assertThat(underTest.appliesTo(setOf(mock<ManagementMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to invalid messages`() {
        assertThat(underTest.appliesTo(setOf(mock<InvalidMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to invalid meta messages`() {
        assertThat(underTest.appliesTo(setOf(mock<InvalidMetaMessage>()))).isFalse()
    }

    @Test
    fun `test that composable contains forward bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<TextMessage>()),
                setAction = {},
                hideBottomSheet = {}
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that on trigger launches the chat picker`() {
        val chatId = 123L
        val message = mock<NormalMessage> {
            on { this.chatId } doReturn chatId
        }
        val messages = setOf(message)
        composeTestRule.setContent {
            underTest.OnTrigger(messages = messages) {}
        }

        verify(launchPicker).invoke(any(), eq(chatId), any())
    }
}