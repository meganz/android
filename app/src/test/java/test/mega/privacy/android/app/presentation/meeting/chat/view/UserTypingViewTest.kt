package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.UserTypingView
import mega.privacy.android.app.utils.TextUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserTypingViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that user typing view is displayed correctly when one user is typing`() {
        initComposeRuleContent(listOf("user1"))
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.resources.getQuantityString(
                    R.plurals.user_typing,
                    1,
                    "user1"
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that user typing view is displayed correctly when two users are typing`() {
        initComposeRuleContent(listOf("user1", "user2"))
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.resources.getQuantityString(
                    R.plurals.user_typing,
                    2,
                    "user1, user2"
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that user typing view is displayed correctly when more than two users are typing`() {
        initComposeRuleContent(listOf("user1", "user2", "user3"))
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.more_users_typing,
                    "user1, user2"
                )
            )
        ).assertExists()
    }

    private fun initComposeRuleContent(usersTyping: List<String?>) {
        composeTestRule.setContent {
            UserTypingView(
                usersTyping = usersTyping
            )
        }
    }
}