package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ReactionsInfoViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that onUserClicked is called when a user is clicked`() {
        val onUserClicked: (Long) -> Unit = mock()
        val mockUserA = UIReactionUser(
            userHandle = 123,
            name = "UserA",
        )
        val mockUserB = UIReactionUser(
            userHandle = 456,
            name = "UserB",
        )
        val mockUIReactionList = listOf(
            UIReaction(
                reaction = "ReactionA",
                shortCode = ":short_code:",
                count = 1,
                hasMe = false,
                userList = listOf(
                    mockUserA,
                    mockUserB,
                )
            )
        )

        composeRule.setContent {
            ReactionsInfoView(
                modifier = Modifier,
                currentReaction = mockUIReactionList[0].reaction,
                reactionList = mockUIReactionList,
                onUserClick = onUserClicked,
            )
        }
        composeRule.onNodeWithText(mockUIReactionList[0].reaction).performClick()
        composeRule.onNodeWithText(mockUserA.name).performClick()
        verify(onUserClicked).invoke(mockUserA.userHandle)
    }
}