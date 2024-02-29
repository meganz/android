package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.CHAT_LOCATION_VIEW_TAG
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class EditLocationMessageActionTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var underTest: EditLocationMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    private val message = mock<LocationMessage> {
        on { isEditable } doReturn true
    }
    private val messages = setOf(message)

    @Before
    fun setUp() {
        underTest = EditLocationMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to editable location messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(message))).isTrue()
    }

    @Test
    fun `test that action does not apply to non editable, location messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<LocationMessage> {
            on { isEditable } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non location messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isFalse()
    }

    @Test
    fun `test that composable contains edit bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = messages,
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that clicking the menu option shows the location view`() {
        val state = mock<ChatUiState> {
            on { isGeolocationEnabled } doReturn true
        }
        val message = mock<LocationMessage> {
            on { msgId } doReturn 123L
        }
        val messages = setOf(message)
        whenever(chatViewModel.state).thenReturn(MutableStateFlow(state).asStateFlow())
        composeTestRule.setContent {
            underTest.OnTrigger(messages) {}
        }
        composeTestRule.onNodeWithTag(CHAT_LOCATION_VIEW_TAG).assertExists()
    }
}