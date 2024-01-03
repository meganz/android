package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatTextFieldTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that emoji icon show correctly`() {
        composeRule.setContent {
            ChatTextField(text = "Hello world", onTextChange = {}, isExpanded = false)
        }
        composeRule.onNodeWithTag(CHAT_TEXT_FIELD_EMOJI_ICON).assertExists()
    }
}