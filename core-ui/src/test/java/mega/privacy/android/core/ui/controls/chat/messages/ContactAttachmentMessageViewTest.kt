package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactAttachmentMessageViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that user name show correctly`() {
        val userName = "User name"
        initComposeRuleContent(
            isMe = true,
            userName = userName,
            email = "email",
        )
        composeRule.onNodeWithText(userName).assertExists()
    }

    @Test
    fun `test that email show correctly`() {
        val email = "email"
        initComposeRuleContent(
            isMe = true,
            userName = "User name",
            email = email,
        )
        composeRule.onNodeWithText(email).assertExists()
    }

    private fun initComposeRuleContent(
        isMe: Boolean,
        userName: String,
        email: String,
    ) {
        composeRule.setContent {
            ContactAttachmentMessageView(
                isMe = isMe,
                userName = userName,
                email = email,
                avatar = { },
                modifier = Modifier,
                status = null,
            )
        }
    }
}