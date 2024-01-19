package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AddReactionChipTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that callback is invoked when add reaction chip is clicked`() {
        val callback: () -> Unit = mock()
        composeRule.setContent {
            AddReactionChip(
                onAddClicked = callback,
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP).performClick()
        verify(callback).invoke()
    }
}