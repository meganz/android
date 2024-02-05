package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
class ReactionChipTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that callback is invoked when reaction chip is clicked`() {
        val callback: (String) -> Unit = mock()
        val expectedReactionString = "A"
        composeRule.setContent {
            ReactionChip(
                reaction = UIReaction(expectedReactionString, 1, false),
                onClick = callback,
                systemLayoutDirection = LayoutDirection.Ltr,
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_REACTION_CHIP).performClick()
        verify(callback).invoke(expectedReactionString)
    }
}
