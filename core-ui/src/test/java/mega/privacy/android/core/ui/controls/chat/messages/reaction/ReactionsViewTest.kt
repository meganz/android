package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ReactionsViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that add reaction chip is always shown when there is no reactions`() {
        val reactions = emptyList<UIReaction>()
        composeRule.setContent {
            ReactionsView(
                modifier = Modifier,
                reactions = reactions,
                onAddReactionClicked = {},
                onReactionClicked = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP).assertExists()
    }

    @Test
    fun `test that add reaction chip is always shown when there are reactions`() {
        val reactions = listOf(
            UIReaction("ReactionA", 1, false),
            UIReaction("ReactionB", 2, false),
            UIReaction("ReactionC", 3, false),
        )
        composeRule.setContent {
            ReactionsView(
                modifier = Modifier,
                reactions = reactions,
                onAddReactionClicked = {},
                onReactionClicked = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP).assertExists()
    }

    @Test
    fun `test that add reaction call back is invoked when it is clicked`() {
        val reactions = emptyList<UIReaction>()
        val addReactionCallBack: () -> Unit = mock()
        composeRule.setContent {
            ReactionsView(
                modifier = Modifier,
                reactions = reactions,
                onAddReactionClicked = addReactionCallBack,
                onReactionClicked = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP).performClick()
        verify(addReactionCallBack).invoke()
    }

    @Test
    fun `test that reaction chip callback is invoked when it is clicked`() {
        val reactions = listOf(
            UIReaction("ReactionA", 1, false),
        )
        val reactionChipCallback: (String) -> Unit = mock()

        composeRule.setContent {
            ReactionsView(
                modifier = Modifier,
                reactions = reactions,
                onAddReactionClicked = {},
                onReactionClicked = reactionChipCallback,
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CHAT_MESSAGE_REACTION_CHIP).performClick()
        verify(reactionChipCallback).invoke(reactions[0].reaction)
    }
}
