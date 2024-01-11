package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInputTextToolbarTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that attach icon show correctly`() {
        initComposeRuleContent("Hello world", "Placeholder")
        composeRule.onNodeWithTag(TEST_TAG_ATTACHMENT_ICON).assertExists()
    }

    @Test
    fun `test that placeholder show correctly`() {
        val placeholder = "Placeholder"
        initComposeRuleContent("", placeholder)
        composeRule.onNodeWithText(placeholder).assertExists()
    }

    @Test
    fun `test that text show correctly`() {
        val text = "Hello world"
        val placeHolder = "Placeholder"
        initComposeRuleContent(text, placeHolder)
        composeRule.onNodeWithText(text).assertExists()
        composeRule.onNodeWithText(placeHolder).assertDoesNotExist()
    }

    private fun initComposeRuleContent(
        text: String,
        placeHolder: String,
    ) {
        composeRule.setContent {
            ChatInputTextToolbar(
                text = text,
                placeholder = placeHolder,
                showEmojiPicker = false,
                {},
                {},
                {})
        }
    }
}