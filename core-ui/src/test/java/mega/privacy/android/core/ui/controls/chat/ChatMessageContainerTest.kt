package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageContainerTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that forward icon show correctly`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = true,
            time = null,
        )
        composeRule.onNodeWithTag(TEST_TAG_FORWARD_ICON).assertExists()
    }

    @Test
    fun `test that forward icon does not show`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
        )
        composeRule.onNodeWithTag(TEST_TAG_FORWARD_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that time show correctly`() {
        val time = "12:00"
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = time,
        )
        composeRule.onNodeWithText(time).assertExists()
    }

    private fun initComposeRuleContent(
        isMine: Boolean,
        showForwardIcon: Boolean,
        time: String?,
    ) {
        composeRule.setContent {
            ChatMessageContainer(
                isMine = isMine,
                showForwardIcon = showForwardIcon,
                time = time,
                avatarOrIcon = {},
                content = {},
            )
        }
    }
}