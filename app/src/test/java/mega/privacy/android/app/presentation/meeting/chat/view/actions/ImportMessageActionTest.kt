package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ImportMessageActionTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var underTest: ImportMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    @Before
    fun setUp() {
        underTest = ImportMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to attachment messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<NodeAttachmentMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply other type of messages`() {
        Truth.assertThat(underTest.appliesTo(setOf(mock<LocationMessage>()))).isFalse()
    }

    @Test
    fun `test that composable contains edit bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<NodeAttachmentMessage>()),
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that onHandled() is invoked when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(
                messages = setOf(mock<NodeAttachmentMessage>()),
                onHandled = onHandled
            )
        }
        verify(onHandled).invoke()
    }
}