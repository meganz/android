package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        composeRule.setContent {
            ChatInputTextToolbar {}
        }
        composeRule.onNodeWithTag(TEST_TAG_ATTACHMENT_ICON).assertExists()
    }
}