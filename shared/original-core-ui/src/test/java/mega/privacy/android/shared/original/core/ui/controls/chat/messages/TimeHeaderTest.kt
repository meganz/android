package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeHeaderTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that time header show correctly`() {
        initComposeRuleContent(
            timeString = "12:00",
        )
        composeRule.onNodeWithText("12:00").assertExists()
    }

    @Test
    fun `test that time and user name header show correctly`() {
        initComposeRuleContent(
            timeString = "12:00",
            userName = "User"
        )
        composeRule.onNodeWithText("12:00").assertExists()
        composeRule.onNodeWithText("User").assertExists()
    }

    private fun initComposeRuleContent(
        timeString: String,
        userName: String? = null,
    ) {
        composeRule.setContent {
            TimeHeader(
                timeString = timeString,
                displayAsMine = true,
                userName = userName
            )
        }
    }
}