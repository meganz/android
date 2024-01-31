package mega.privacy.android.core.ui.controls.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatEditMessageViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that edit message content view show correctly`() {
        val content = "Hello world"
        composeRule.setContent {
            ChatEditMessageView(
                content = content,
                onCloseEditing = {},
            )
        }
        composeRule.onNodeWithText(content).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.edit_chat_message))
            .assertExists()
    }
}