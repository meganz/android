package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class EditMessageActionTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var underTest: EditMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    @Before
    fun setUp() {
        underTest = EditMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to editable, non location messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<NormalMessage> {
            on { isEditable } doReturn true
        }))).isTrue()
    }

    @Test
    fun `test that action does not apply to editable, location messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<LocationMessage> {
            on { isEditable } doReturn true
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non editable messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<NormalMessage> {
            on { isEditable } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that composable contains edit bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<TextMessage>()),
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that on trigger invokes view model`() {
        val message = mock<LocationMessage> {
            on { isEditable } doReturn true
        }
        val messages = setOf(message)
        composeTestRule.setContent {
            underTest.OnTrigger(messages = messages) {
            }
        }

        verify(chatViewModel).onEditMessage(messages.first())
    }
}