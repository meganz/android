package mega.privacy.android.core.ui.controls.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageContainerTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun `test that error text shows correctly when send error`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = true,
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.manual_retry_alert)
        ).assertExists()
    }

    @Test
    fun `test that error text does not show when send successfully`() {
        initComposeRuleContent(
            isMine = false,
            showForwardIcon = false,
            time = null,
            isSendError = false,
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.manual_retry_alert)
        ).assertDoesNotExist()
    }

    private fun initComposeRuleContent(
        isMine: Boolean,
        showForwardIcon: Boolean,
        time: String?,
        isSendError: Boolean = false,
    ) {
        composeRule.setContent {
            ChatMessageContainer(
                isMine = isMine,
                showForwardIcon = showForwardIcon,
                time = time,
                isSendError = isSendError,
                avatarOrIcon = {},
                content = {},
            )
        }
    }
}